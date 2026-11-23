package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.media.FaceDetector
import com.example.model.DetectedSubjectFace
import com.example.model.FrameEvaluationResult
import com.example.model.OverlayKind
import com.example.model.OverlayRegion
import com.example.model.ProcessedFrameResult
import com.example.model.ScreenshotItem
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun interface FaceDetectionProvider {
    fun detectFaces(bitmap: Bitmap): List<DetectedSubjectFace>
}

object AndroidFaceDetectionProvider : FaceDetectionProvider {
    override fun detectFaces(bitmap: Bitmap): List<DetectedSubjectFace> {
        val srcW = bitmap.width
        val srcH = bitmap.height
        if (srcW < 32 || srcH < 32) return emptyList()

        // Scale to <= 960px wide with strictly even width as required by android.media.FaceDetector
        val rawTargetW = min(960, srcW)
        val targetW = (rawTargetW and 1.inv()).coerceAtLeast(32)
        val scale = targetW.toFloat() / srcW.toFloat()
        val targetH = (srcH * scale).roundToInt().coerceAtLeast(32)

        val rgb565 = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
        return try {
            val canvas = Canvas(rgb565)
            canvas.drawBitmap(
                bitmap,
                Rect(0, 0, srcW, srcH),
                Rect(0, 0, targetW, targetH),
                null
            )
            val maxFaces = 4
            val detector = FaceDetector(targetW, targetH, maxFaces)
            val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
            val count = detector.findFaces(rgb565, faces)
            val result = mutableListOf<DetectedSubjectFace>()
            val mid = PointF()
            for (i in 0 until count) {
                val f = faces[i] ?: continue
                f.getMidPoint(mid)
                result.add(
                    DetectedSubjectFace(
                        centerX = mid.x / scale,
                        centerY = mid.y / scale,
                        eyesDistance = f.eyesDistance() / scale,
                        confidence = f.confidence()
                    )
                )
            }
            result
        } finally {
            rgb565.recycle()
        }
    }
}

object SmartFrameAnalyzer {

    private const val TILE_COLS = 32
    private const val TILE_ROWS = 18

    /**
     * Crops source bitmap by zoom & pan percentages and scales into target dimensions.
     */
    fun drawCroppedFrame(
        source: Bitmap,
        zoom: Float,
        panX: Float,
        panY: Float,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val vWidth = source.width.toFloat().coerceAtLeast(1f)
        val vHeight = source.height.toFloat().coerceAtLeast(1f)
        val safeZoom = zoom.coerceIn(1.0f, 3.0f)

        val cropWidth = vWidth / safeZoom
        val cropHeight = vHeight / safeZoom

        val centerX = vWidth / 2f + (panX / 50f) * (vWidth - cropWidth) / 2f
        val centerY = vHeight / 2f + (panY / 50f) * (vHeight - cropHeight) / 2f

        val sx = (centerX - cropWidth / 2f).coerceIn(0f, (vWidth - cropWidth).coerceAtLeast(0f)).toInt()
        val sy = (centerY - cropHeight / 2f).coerceIn(0f, (vHeight - cropHeight).coerceAtLeast(0f)).toInt()
        val sw = cropWidth.toInt().coerceIn(1, source.width - sx)
        val sh = cropHeight.toInt().coerceIn(1, source.height - sy)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val srcRect = Rect(sx, sy, sx + sw, sy + sh)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(source, srcRect, dstRect, null)
        return output
    }

    /**
     * Full sequential quality + subject + text/logo/watermark detection, cleaning & re-verification pipeline.
     * Steps:
     *  (2) Mandatory Subject & Quality Check (face, uncropped framing, torso continuity, no blur, no drone/skyline)
     *  (3) Smart Filter detection of Text / Credits / YouTube Logo / Channel Name / Watermarks
     *  (4) Clean inpaint removal of localized peripheral overlays if frame has valid subject
     *  (5) Re-verify the cleaned image before approving for final save
     */
    fun processAndVerifyFrame(
        sourceBitmap: Bitmap,
        enableSmartFilter: Boolean,
        faceDetector: FaceDetectionProvider = AndroidFaceDetectionProvider
    ): ProcessedFrameResult {
        if (!enableSmartFilter) {
            val quickEval = evaluateFrameAnalysis(
                bitmap = sourceBitmap,
                requireSubject = false,
                faceDetector = faceDetector
            )
            return ProcessedFrameResult(
                bitmap = sourceBitmap,
                evaluation = quickEval.copy(isAcceptable = true),
                accepted = true,
                wasWatermarkCleaned = false,
                cleanedOverlayCount = 0,
                rejectionReason = null
            )
        }

        // Step 2 & 3: Subject/quality + overlay detection
        val rawFaces = faceDetector.detectFaces(sourceBitmap)
        val eval1 = evaluateFrameAnalysis(
            bitmap = sourceBitmap,
            requireSubject = true,
            faceDetector = faceDetector,
            preDetectedFaces = rawFaces
        )

        if (!eval1.hasValidSubject || eval1.isBlurry || eval1.hasHeavyUnremovableText) {
            return ProcessedFrameResult(
                bitmap = null,
                evaluation = eval1,
                accepted = false,
                wasWatermarkCleaned = false,
                cleanedOverlayCount = 0,
                rejectionReason = eval1.rejectionReason ?: "ফ্রেমটি কোয়ালিটি ফিল্টারে গ্রহণযোগ্য হয়নি"
            )
        }

        // If no localized overlays detected, frame is already completely clean
        if (eval1.overlayRegions.isEmpty()) {
            return ProcessedFrameResult(
                bitmap = sourceBitmap,
                evaluation = eval1,
                accepted = eval1.isAcceptable,
                wasWatermarkCleaned = false,
                cleanedOverlayCount = 0,
                rejectionReason = if (eval1.isAcceptable) null else eval1.rejectionReason
            )
        }

        // Step 4: Cleanly remove & replace localized watermarks/logos/channel text
        val cleaned = cleanOverlaysAndWatermarks(
            source = sourceBitmap,
            regions = eval1.overlayRegions,
            protectedFaces = rawFaces
        )

        // Step 5: Mandatory Re-Verification on the cleaned bitmap
        val eval2 = evaluateFrameAnalysis(
            bitmap = cleaned,
            requireSubject = true,
            faceDetector = faceDetector
        )

        val verifiedClean = eval2.isAcceptable &&
            eval2.hasValidSubject &&
            !eval2.isBlurry &&
            !eval2.hasHeavyUnremovableText &&
            eval2.overlayRegions.isEmpty()

        return if (verifiedClean) {
            ProcessedFrameResult(
                bitmap = cleaned,
                evaluation = eval2,
                accepted = true,
                wasWatermarkCleaned = true,
                cleanedOverlayCount = eval1.overlayRegions.size,
                rejectionReason = null
            )
        } else {
            cleaned.recycle()
            ProcessedFrameResult(
                bitmap = null,
                evaluation = eval2,
                accepted = false,
                wasWatermarkCleaned = false,
                cleanedOverlayCount = 0,
                rejectionReason = eval2.rejectionReason ?: "ওয়াটারমার্ক বা টেক্সট সম্পূর্ণ পরিষ্কার করা যায়নি"
            )
        }
    }

    /**
     * Evaluates a single frame for subject visibility, framing/cropping, motion blur,
     * heavy credits/ads, and localized corner watermarks/YouTube logos/channel names.
     */
    fun evaluateFrameAnalysis(
        bitmap: Bitmap,
        requireSubject: Boolean = true,
        faceDetector: FaceDetectionProvider = AndroidFaceDetectionProvider,
        preDetectedFaces: List<DetectedSubjectFace>? = null
    ): FrameEvaluationResult {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 32 || h <= 32) {
            return FrameEvaluationResult(
                overallScore = 0f,
                sharpnessScore = 0f,
                hasAdOverlay = true,
                isBlurry = true,
                isAcceptable = false,
                rejectionReason = "অবৈধ বা অতি ক্ষুদ্র ফ্রেম"
            )
        }

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val detectedFaces = preDetectedFaces ?: faceDetector.detectFaces(bitmap)
        val validFaces = filterAndValidateSubjects(pixels, w, h, detectedFaces)
        val bestConfidence = validFaces.maxOfOrNull { it.confidence } ?: 0f

        // 320x180 high-resolution adaptive sampling grid for text, logos, sharpness, and luma
        val stepX = max(1, w / 320)
        val stepY = max(1, h / 180)

        var totalSamples = 0
        var whitePixels = 0
        var rightSamples = 0
        var rightWhitePixels = 0

        var bottomSamples = 0
        var bottomWhitePixels = 0
        var bottomRedPixels = 0
        var bottomLumaSum = 0.0
        var aboveBottomLumaSum = 0.0
        var aboveBottomSamples = 0

        var globalTextTransitions = 0
        var transitionPairs = 0

        var lumaSum = 0.0
        var lumaSqSum = 0.0

        val cxStart = (w * 0.16f).toInt()
        val cxEnd = (w * 0.84f).toInt()
        val cyStart = (h * 0.14f).toInt()
        val cyEnd = (h * 0.86f).toInt()

        var centerSamples = 0
        var centerGradSum = 0.0
        var centerStrongEdges = 0
        var outerGradSum = 0.0
        var outerSamples = 0
        var minCenterLuma = 255f
        var maxCenterLuma = 0f

        val tileCount = TILE_COLS * TILE_ROWS
        val tileSamples = IntArray(tileCount)
        val tileHTransitions = IntArray(tileCount)
        val tileVTransitions = IntArray(tileCount)
        val tileRedLogoPixels = IntArray(tileCount)
        val tileBrightPixels = IntArray(tileCount)
        val tileMinLuma = FloatArray(tileCount) { 255f }
        val tileMaxLuma = FloatArray(tileCount) { 0f }

        val bottomThresholdY = (h * 0.83f).toInt()
        val aboveBottomStartY = (h * 0.72f).toInt()

        var y = 0
        while (y < h) {
            var prevScanLuma = -1f
            val ty = ((y * TILE_ROWS) / h).coerceIn(0, TILE_ROWS - 1)
            val upY = max(0, y - stepY)
            var x = 0
            while (x < w) {
                val color = pixels[y * w + x]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val luma = 0.299f * r + 0.587f * g + 0.114f * b

                totalSamples++
                lumaSum += luma
                lumaSqSum += (luma * luma)

                val isWhite = r > 226 && g > 226 && b > 226
                if (isWhite) whitePixels++

                if (x >= w / 2) {
                    rightSamples++
                    if (isWhite) rightWhitePixels++
                }

                if (y >= bottomThresholdY) {
                    bottomSamples++
                    bottomLumaSum += luma
                    if (isWhite) bottomWhitePixels++
                    if (r > 180 && g < 85 && b < 85) bottomRedPixels++
                } else if (y >= aboveBottomStartY) {
                    aboveBottomSamples++
                    aboveBottomLumaSum += luma
                }

                if (prevScanLuma >= 0f) {
                    transitionPairs++
                    if (abs(luma - prevScanLuma) > 54f) {
                        globalTextTransitions++
                    }
                }
                prevScanLuma = luma

                val lx = max(0, x - stepX)
                val rx = min(w - 1, x + stepX)
                val dy = min(h - 1, y + stepY)

                val lc = pixels[y * w + lx]
                val rc = pixels[y * w + rx]
                val uc = pixels[upY * w + x]
                val dc = pixels[dy * w + x]

                val lLuma = lumaOf(lc)
                val rLuma = lumaOf(rc)
                val uLuma = lumaOf(uc)
                val dLuma = lumaOf(dc)

                val grad = abs(luma - rLuma) + abs(luma - dLuma)

                val inCenter = x in cxStart..cxEnd && y in cyStart..cyEnd
                if (inCenter) {
                    centerSamples++
                    centerGradSum += grad
                    if (grad > 20f) centerStrongEdges++
                    if (luma < minCenterLuma) minCenterLuma = luma
                    if (luma > maxCenterLuma) maxCenterLuma = luma
                } else {
                    outerSamples++
                    outerGradSum += grad
                }

                // Tile accumulation
                val tx = ((x * TILE_COLS) / w).coerceIn(0, TILE_COLS - 1)
                val tIdx = ty * TILE_COLS + tx
                tileSamples[tIdx]++
                if (abs(luma - lLuma) > 48f) tileHTransitions[tIdx]++
                if (abs(luma - uLuma) > 48f) tileVTransitions[tIdx]++
                if (r > 180 && g < 75 && b < 75 && (r - max(g, b)) > 105) {
                    tileRedLogoPixels[tIdx]++
                }
                if (luma > 185f || (r > 185 && g > 170 && b < 110)) {
                    tileBrightPixels[tIdx]++
                }
                if (luma < tileMinLuma[tIdx]) tileMinLuma[tIdx] = luma
                if (luma > tileMaxLuma[tIdx]) tileMaxLuma[tIdx] = luma

                x += stepX
            }
            y += stepY
        }

        val safeTotal = totalSamples.coerceAtLeast(1).toFloat()
        val meanLuma = (lumaSum / safeTotal).toFloat()
        val variance = ((lumaSqSum / safeTotal) - (meanLuma * meanLuma)).coerceAtLeast(0.0).toFloat()

        val overallWhiteRatio = whitePixels / safeTotal
        val rightWhiteRatio = rightWhitePixels / rightSamples.coerceAtLeast(1).toFloat()
        val textRatio = globalTextTransitions / transitionPairs.coerceAtLeast(1).toFloat()

        val bottomSafe = bottomSamples.coerceAtLeast(1).toFloat()
        val bottomWhiteRatio = bottomWhitePixels / bottomSafe
        val bottomRedRatio = bottomRedPixels / bottomSafe
        val bottomMeanLuma = (bottomLumaSum / bottomSafe).toFloat()
        val aboveBottomMeanLuma = (aboveBottomLumaSum / aboveBottomSamples.coerceAtLeast(1).toFloat()).toFloat()
        val boundaryStep = abs(bottomMeanLuma - aboveBottomMeanLuma)

        val centerSafe = centerSamples.coerceAtLeast(1).toFloat()
        val sharpnessScore = (centerGradSum / centerSafe).toFloat()
        val centerStrongEdgeRatio = centerStrongEdges / centerSafe
        val outerSharpness = (outerGradSum / outerSamples.coerceAtLeast(1).toFloat()).toFloat().coerceAtLeast(0.1f)
        val focalRatio = sharpnessScore / outerSharpness
        val centerDynamicRange = (maxCenterLuma - minCenterLuma).coerceAtLeast(0f)

        // Classify tiles for text/glyphs & YouTube red badges (ignoring pixels inside valid human faces)
        val isTextTile = BooleanArray(tileCount)
        val isRedLogoTile = BooleanArray(tileCount)

        for (ty in 0 until TILE_ROWS) {
            for (tx in 0 until TILE_COLS) {
                val idx = ty * TILE_COLS + tx
                val n = tileSamples[idx].coerceAtLeast(1).toFloat()
                val tileCenterX = ((tx + 0.5f) / TILE_COLS) * w
                val tileCenterY = ((ty + 0.5f) / TILE_ROWS) * h
                if (isPointInsideAnyFace(tileCenterX, tileCenterY, validFaces)) {
                    continue
                }

                val hRate = tileHTransitions[idx] / n
                val vRate = tileVTransitions[idx] / n
                val contrast = tileMaxLuma[idx] - tileMinLuma[idx]
                val brightRate = tileBrightPixels[idx] / n
                val redRate = tileRedLogoPixels[idx] / n

                if (redRate >= 0.09f) {
                    isRedLogoTile[idx] = true
                }
                val looksLikeTextGlyphs =
                    (hRate >= 0.075f && vRate >= 0.055f && contrast >= 62f) ||
                        (hRate >= 0.095f && vRate >= 0.045f && brightRate >= 0.06f && contrast >= 75f)
                if (looksLikeTextGlyphs) {
                    isTextTile[idx] = true
                }
            }
        }

        // Analyze center text runs (opening credits, title cards, full-screen captions)
        var centerTextTiles = 0
        var totalTextTiles = 0
        var maxCenterRowRun = 0
        for (ty in 0 until TILE_ROWS) {
            var currentRun = 0
            for (tx in 0 until TILE_COLS) {
                val idx = ty * TILE_COLS + tx
                if (isTextTile[idx]) {
                    totalTextTiles++
                    if (tx in 4..27 && ty in 2..15) {
                        centerTextTiles++
                        currentRun++
                        if (currentRun > maxCenterRowRun) maxCenterRowRun = currentRun
                    } else {
                        currentRun = 0
                    }
                } else {
                    currentRun = 0
                }
            }
        }

        // Group peripheral text/red-logo tiles into cleanable OverlayRegions
        val rawPeripheralRegions = extractPeripheralOverlayRegions(
            w = w,
            h = h,
            isTextTile = isTextTile,
            isRedLogoTile = isRedLogoTile,
            validFaces = validFaces
        )

        val oversizedPeripheral = rawPeripheralRegions.any { r ->
            (r.right - r.left) > w * 0.48f || (r.bottom - r.top) > h * 0.28f
        }

        val isWhitePackshot = overallWhiteRatio > 0.16f || (rightWhiteRatio > 0.22f && textRatio > 0.045f)
        val isLowerThirdAd = bottomWhiteRatio > 0.24f ||
            (bottomMeanLuma > 214f && boundaryStep > 26f) ||
            (bottomRedRatio > 0.08f && bottomWhiteRatio > 0.12f)
        val hasHeavyCreditOrCenterText = centerTextTiles >= 7 ||
            maxCenterRowRun >= 5 ||
            totalTextTiles >= 28 ||
            oversizedPeripheral ||
            textRatio > 0.085f

        val hasHeavyUnremovableText = isWhitePackshot || isLowerThirdAd || hasHeavyCreditOrCenterText
        val hasAdOverlay = hasHeavyUnremovableText || rawPeripheralRegions.isNotEmpty()

        // Quality & Subject verification
        val subjectMissingOrUnclear = requireSubject && validFaces.isEmpty()
        val isMotionBlur = if (validFaces.isNotEmpty()) {
            sharpnessScore < 4.5f && centerStrongEdgeRatio < 0.03f
        } else {
            sharpnessScore < 8.6f
        }
        val isExposureOrFlatFailure = centerDynamicRange < 30f ||
            variance < 22f ||
            meanLuma < 15f ||
            meanLuma > 243f

        val isBlurry = isMotionBlur || isExposureOrFlatFailure || subjectMissingOrUnclear

        val reason = when {
            isWhitePackshot -> "বাণিজ্যিক বিজ্ঞাপন / সাদা প্যাকশট শনাক্ত"
            isLowerThirdAd -> "লোয়ার-থার্ড অ্যাড ব্যানার শনাক্ত"
            hasHeavyCreditOrCenterText -> "অন-স্ক্রিন টেক্সট / ওপেনিং ক্রেডিট শনাক্ত"
            subjectMissingOrUnclear -> "স্পষ্ট মানুষ বা সাবজেক্ট নেই (দূরবর্তী/কাটা দৃশ্য)"
            isMotionBlur -> "মোশন ব্লার / ফোকাসহীন ফ্রেম"
            meanLuma < 15f -> "অতিরিক্ত অন্ধকার ফ্রেম"
            meanLuma > 243f -> "অতিরিক্ত সাদা ফ্ল্যাশ ফ্রেম"
            isExposureOrFlatFailure -> "অস্পষ্ট বা কম কনট্রাস্ট ফ্রেম"
            rawPeripheralRegions.isNotEmpty() -> "কর্নার টেক্সট / লোগো / ওয়াটারমার্ক শনাক্ত"
            else -> null
        }

        val isAcceptable = !hasHeavyUnremovableText && !isBlurry && rawPeripheralRegions.isEmpty()

        val rawScore = sharpnessScore * 2.8f +
            sqrt(variance) * 0.7f +
            (if (validFaces.isNotEmpty()) 30f + bestConfidence * 15f else 0f) +
            (if (focalRatio > 1.2f) 10f else 0f) +
            (if (centerDynamicRange > 55f) 10f else 0f) -
            (if (hasHeavyUnremovableText) 85f else 0f) -
            (if (subjectMissingOrUnclear) 75f else 0f) -
            (if (isMotionBlur) 60f else 0f) -
            (rawPeripheralRegions.size * 12f)

        return FrameEvaluationResult(
            overallScore = rawScore.coerceIn(0f, 100f),
            sharpnessScore = sharpnessScore,
            hasAdOverlay = hasAdOverlay,
            isBlurry = isBlurry,
            isAcceptable = isAcceptable,
            rejectionReason = reason,
            hasValidSubject = validFaces.isNotEmpty(),
            hasHeavyUnremovableText = hasHeavyUnremovableText,
            overlayRegions = rawPeripheralRegions,
            detectedFaceCount = validFaces.size,
            subjectConfidence = bestConfidence
        )
    }

    /**
     * Validates that at least one detected face is:
     * - Confident (>= 0.38)
     * - Appropriately sized (not a tiny distant speck)
     * - Uncropped inside frame edges
     * - Locally sharp on the face ROI (no motion blur on the person)
     * - Connected to foreground body/shoulder region below the face
     */
    private fun filterAndValidateSubjects(
        pixels: IntArray,
        w: Int,
        h: Int,
        candidates: List<DetectedSubjectFace>
    ): List<DetectedSubjectFace> {
        if (candidates.isEmpty()) return emptyList()
        val minEyesDist = w * 0.015f
        val valid = mutableListOf<DetectedSubjectFace>()

        for (face in candidates) {
            if (face.confidence < 0.38f) continue
            if (face.eyesDistance < minEyesDist) continue

            val halfW = face.eyesDistance * 1.05f
            val topY = face.centerY - face.eyesDistance * 1.35f
            val bottomY = face.centerY + face.eyesDistance * 1.45f

            // Reject severely cropped faces against frame borders
            if (face.centerX - halfW < -w * 0.015f) continue
            if (face.centerX + halfW > w * 1.015f) continue
            if (topY < -h * 0.015f) continue
            if (bottomY > h * 0.94f) continue

            // Measure sharpness inside face ROI
            val fx0 = (face.centerX - halfW).roundToInt().coerceIn(0, w - 2)
            val fx1 = (face.centerX + halfW).roundToInt().coerceIn(fx0 + 1, w - 1)
            val fy0 = topY.roundToInt().coerceIn(0, h - 2)
            val fy1 = bottomY.roundToInt().coerceIn(fy0 + 1, h - 1)

            val faceStep = max(1, (fx1 - fx0) / 24)
            var faceGradSum = 0.0
            var faceStrongEdges = 0
            var faceCount = 0
            var faceMinLuma = 255f
            var faceMaxLuma = 0f
            var fy = fy0
            while (fy < fy1 - faceStep) {
                var fx = fx0
                while (fx < fx1 - faceStep) {
                    val l0 = lumaOf(pixels[fy * w + fx])
                    val lr = lumaOf(pixels[fy * w + fx + faceStep])
                    val ld = lumaOf(pixels[(fy + faceStep) * w + fx])
                    val g = abs(l0 - lr) + abs(l0 - ld)
                    faceGradSum += g
                    if (g > 18f) faceStrongEdges++
                    if (l0 < faceMinLuma) faceMinLuma = l0
                    if (l0 > faceMaxLuma) faceMaxLuma = l0
                    faceCount++
                    fx += faceStep
                }
                fy += faceStep
            }
            val faceSharpness = if (faceCount > 0) (faceGradSum / faceCount).toFloat() else 0f
            val faceStrongRatio = if (faceCount > 0) faceStrongEdges.toFloat() / faceCount.toFloat() else 0f
            val faceRange = (faceMaxLuma - faceMinLuma).coerceAtLeast(0f)
            if (faceSharpness < 3.0f && (faceStrongRatio < 0.015f || faceRange < 35f)) continue

            // Check torso/shoulder continuity below the face (ensures subject body isn't absent/cut off)
            val torsoY0 = fy1.coerceIn(0, h - 2)
            val torsoY1 = (fy1 + (face.eyesDistance * 2.2f).roundToInt()).coerceIn(torsoY0 + 1, h - 1)
            val torsoX0 = (face.centerX - face.eyesDistance * 1.6f).roundToInt().coerceIn(0, w - 2)
            val torsoX1 = (face.centerX + face.eyesDistance * 1.6f).roundToInt().coerceIn(torsoX0 + 1, w - 1)

            var torsoGradSum = 0.0
            var torsoCount = 0
            var torsoMin = 255f
            var torsoMax = 0f
            val torsoStep = max(2, (torsoX1 - torsoX0) / 20)
            var ty = torsoY0
            while (ty < torsoY1 - torsoStep) {
                var tx = torsoX0
                while (tx < torsoX1 - torsoStep) {
                    val l0 = lumaOf(pixels[ty * w + tx])
                    val lr = lumaOf(pixels[ty * w + tx + torsoStep])
                    val ld = lumaOf(pixels[(ty + torsoStep) * w + tx])
                    torsoGradSum += abs(l0 - lr) + abs(l0 - ld)
                    if (l0 < torsoMin) torsoMin = l0
                    if (l0 > torsoMax) torsoMax = l0
                    torsoCount++
                    tx += torsoStep
                }
                ty += torsoStep
            }
            val torsoRange = (torsoMax - torsoMin).coerceAtLeast(0f)
            val torsoSharp = if (torsoCount > 0) (torsoGradSum / torsoCount).toFloat() else 10f
            if (torsoCount > 16 && torsoRange < 18f && torsoSharp < 3.5f) continue

            valid.add(face)
        }
        return valid
    }

    private fun isPointInsideAnyFace(x: Float, y: Float, faces: List<DetectedSubjectFace>): Boolean {
        for (f in faces) {
            val hw = f.eyesDistance * 1.45f
            val hhTop = f.eyesDistance * 1.75f
            val hhBot = f.eyesDistance * 1.85f
            if (x >= f.centerX - hw && x <= f.centerX + hw &&
                y >= f.centerY - hhTop && y <= f.centerY + hhBot
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Collects connected peripheral/corner overlay regions (logos, watermarks, channel names, edge text).
     */
    private fun extractPeripheralOverlayRegions(
        w: Int,
        h: Int,
        isTextTile: BooleanArray,
        isRedLogoTile: BooleanArray,
        validFaces: List<DetectedSubjectFace>
    ): List<OverlayRegion> {
        val flagged = BooleanArray(TILE_COLS * TILE_ROWS)
        for (ty in 0 until TILE_ROWS) {
            for (tx in 0 until TILE_COLS) {
                val idx = ty * TILE_COLS + tx
                val inEdgeBand = ty <= 3 || ty >= TILE_ROWS - 4 || tx <= 4 || tx >= TILE_COLS - 5
                if (inEdgeBand && (isTextTile[idx] || isRedLogoTile[idx])) {
                    flagged[idx] = true
                }
            }
        }

        val visited = BooleanArray(TILE_COLS * TILE_ROWS)
        val regions = mutableListOf<OverlayRegion>()
        val queue = ArrayDeque<Int>()

        for (ty in 0 until TILE_ROWS) {
            for (tx in 0 until TILE_COLS) {
                val startIdx = ty * TILE_COLS + tx
                if (!flagged[startIdx] || visited[startIdx]) continue

                var minTx = tx
                var maxTx = tx
                var minTy = ty
                var maxTy = ty
                var hasRed = false
                var tileCounter = 0

                visited[startIdx] = true
                queue.addLast(startIdx)

                while (queue.isNotEmpty()) {
                    val curr = queue.removeFirst()
                    val cx = curr % TILE_COLS
                    val cy = curr / TILE_COLS
                    tileCounter++
                    if (isRedLogoTile[curr]) hasRed = true
                    if (cx < minTx) minTx = cx
                    if (cx > maxTx) maxTx = cx
                    if (cy < minTy) minTy = cy
                    if (cy > maxTy) maxTy = cy

                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = cx + dx
                            val ny = cy + dy
                            if (nx in 0 until TILE_COLS && ny in 0 until TILE_ROWS) {
                                val nIdx = ny * TILE_COLS + nx
                                if (flagged[nIdx] && !visited[nIdx]) {
                                    visited[nIdx] = true
                                    queue.addLast(nIdx)
                                }
                            }
                        }
                    }
                }

                // Require >= 2 tiles or any red logo tile to avoid spurious 1-tile noise
                if (tileCounter >= 2 || hasRed) {
                    val padX = max(8, w / 90)
                    val padY = max(6, h / 70)
                    var left = ((minTx * w) / TILE_COLS - padX).coerceIn(0, w - 2)
                    var top = ((minTy * h) / TILE_ROWS - padY).coerceIn(0, h - 2)
                    var right = (((maxTx + 1) * w) / TILE_COLS + padX).coerceIn(left + 1, w)
                    var bottom = (((maxTy + 1) * h) / TILE_ROWS + padY).coerceIn(top + 1, h)

                    // Clamp away from any valid human face so we never paint over faces
                    for (face in validFaces) {
                        val fL = (face.centerX - face.eyesDistance * 1.3f).roundToInt()
                        val fR = (face.centerX + face.eyesDistance * 1.3f).roundToInt()
                        val fT = (face.centerY - face.eyesDistance * 1.5f).roundToInt()
                        val fB = (face.centerY + face.eyesDistance * 1.6f).roundToInt()
                        val overlaps = left < fR && right > fL && top < fB && bottom > fT
                        if (overlaps) {
                            if (right > fL && left < fL) right = fL.coerceAtLeast(left + 1)
                            else if (left < fR && right > fR) left = fR.coerceAtMost(right - 1)
                        }
                    }

                    if (right - left >= 8 && bottom - top >= 8) {
                        val kind = when {
                            hasRed -> OverlayKind.YOUTUBE_LOGO
                            minTy >= TILE_ROWS - 4 -> OverlayKind.LOWER_THIRD_BANNER
                            minTy <= 3 && (minTx <= 6 || maxTx >= TILE_COLS - 7) -> OverlayKind.CHANNEL_WATERMARK
                            else -> OverlayKind.CORNER_TEXT
                        }
                        regions.add(OverlayRegion(left, top, right, bottom, kind))
                    }
                }
            }
        }
        return regions
    }

    /**
     * Cleanly removes and replaces localized watermarks, logos, and corner text using
     * boundary-weighted directional interpolation with feathered perimeter blending.
     */
    fun cleanOverlaysAndWatermarks(
        source: Bitmap,
        regions: List<OverlayRegion>,
        protectedFaces: List<DetectedSubjectFace> = emptyList()
    ): Bitmap {
        val w = source.width
        val h = source.height
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        if (regions.isEmpty()) return mutable

        val pixels = IntArray(w * h)
        mutable.getPixels(pixels, 0, w, 0, 0, w, h)
        val original = pixels.copyOf()

        for (region in regions) {
            val x0 = region.left.coerceIn(0, w - 2)
            val y0 = region.top.coerceIn(0, h - 2)
            val x1 = region.right.coerceIn(x0 + 1, w)
            val y1 = region.bottom.coerceIn(y0 + 1, h)
            val rw = (x1 - x0).coerceAtLeast(1)
            val rh = (y1 - y0).coerceAtLeast(1)
            val feather = 4

            for (py in y0 until y1) {
                for (px in x0 until x1) {
                    if (isPointInsideAnyFace(px.toFloat(), py.toFloat(), protectedFaces)) {
                        continue
                    }

                    // Sample nearest clean surrounding pixels outside each side of the box
                    var weightSum = 0.0
                    var rAcc = 0.0
                    var gAcc = 0.0
                    var bAcc = 0.0

                    // Top border band
                    if (y0 >= 3) {
                        val sy = (y0 - 3).coerceIn(0, h - 1)
                        val sx = px.coerceIn(0, w - 1)
                        val d = (py - sy).coerceAtLeast(1).toDouble()
                        val wt = 1.0 / (d * d)
                        val c = original[sy * w + sx]
                        rAcc += ((c shr 16) and 0xFF) * wt
                        gAcc += ((c shr 8) and 0xFF) * wt
                        bAcc += (c and 0xFF) * wt
                        weightSum += wt
                    }

                    // Bottom border band
                    if (y1 < h - 3) {
                        val sy = (y1 + 2).coerceIn(0, h - 1)
                        val sx = px.coerceIn(0, w - 1)
                        val d = (sy - py).coerceAtLeast(1).toDouble()
                        val wt = 1.0 / (d * d)
                        val c = original[sy * w + sx]
                        rAcc += ((c shr 16) and 0xFF) * wt
                        gAcc += ((c shr 8) and 0xFF) * wt
                        bAcc += (c and 0xFF) * wt
                        weightSum += wt
                    }

                    // Left border band
                    if (x0 >= 3) {
                        val sx = (x0 - 3).coerceIn(0, w - 1)
                        val sy = py.coerceIn(0, h - 1)
                        val d = (px - sx).coerceAtLeast(1).toDouble()
                        val wt = 1.0 / (d * d)
                        val c = original[sy * w + sx]
                        rAcc += ((c shr 16) and 0xFF) * wt
                        gAcc += ((c shr 8) and 0xFF) * wt
                        bAcc += (c and 0xFF) * wt
                        weightSum += wt
                    }

                    // Right border band
                    if (x1 < w - 3) {
                        val sx = (x1 + 2).coerceIn(0, w - 1)
                        val sy = py.coerceIn(0, h - 1)
                        val d = (sx - px).coerceAtLeast(1).toDouble()
                        val wt = 1.0 / (d * d)
                        val c = original[sy * w + sx]
                        rAcc += ((c shr 16) and 0xFF) * wt
                        gAcc += ((c shr 8) and 0xFF) * wt
                        bAcc += (c and 0xFF) * wt
                        weightSum += wt
                    }

                    if (weightSum > 0.0) {
                        val estR = (rAcc / weightSum).roundToInt().coerceIn(0, 255)
                        val estG = (gAcc / weightSum).roundToInt().coerceIn(0, 255)
                        val estB = (bAcc / weightSum).roundToInt().coerceIn(0, 255)

                        // Feathered seam blend along perimeter of the replaced box
                        val edgeDist = min(
                            min(px - x0, x1 - 1 - px),
                            min(py - y0, y1 - 1 - py)
                        ).coerceAtLeast(0)
                        val alpha = if (rw > 12 && rh > 12 && edgeDist < feather) {
                            (edgeDist + 1).toFloat() / (feather + 1).toFloat()
                        } else {
                            1.0f
                        }

                        val origC = original[py * w + px]
                        val oR = (origC shr 16) and 0xFF
                        val oG = (origC shr 8) and 0xFF
                        val oB = origC and 0xFF

                        val outR = (oR + (estR - oR) * alpha).roundToInt().coerceIn(0, 255)
                        val outG = (oG + (estG - oG) * alpha).roundToInt().coerceIn(0, 255)
                        val outB = (oB + (estB - oB) * alpha).roundToInt().coerceIn(0, 255)

                        pixels[py * w + px] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
                    }
                }
            }
        }

        mutable.setPixels(pixels, 0, w, 0, 0, w, h)
        return mutable
    }

    private fun lumaOf(color: Int): Float {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /**
     * Computes a 3x2 spatial RGB signature (18 normalized values) for anti-same-scene shuffling.
     */
    fun computeSceneSignatureFromBitmap(bitmap: Bitmap): List<Float> {
        val w = bitmap.width
        val h = bitmap.height
        val sig = MutableList(18) { 0f }
        if (w <= 0 || h <= 0) return sig

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val cellW = (w / 3).coerceAtLeast(1)
        val cellH = (h / 2).coerceAtLeast(1)

        for (row in 0 until 2) {
            for (col in 0 until 3) {
                val x0 = col * cellW
                val y0 = row * cellH
                val x1 = if (col == 2) w else (x0 + cellW)
                val y1 = if (row == 1) h else (y0 + cellH)

                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0
                var count = 0

                val stepX = max(1, (x1 - x0) / 12)
                val stepY = max(1, (y1 - y0) / 12)

                var y = y0
                while (y < y1) {
                    var x = x0
                    while (x < x1) {
                        val c = pixels[y * w + x]
                        rSum += ((c shr 16) and 0xFF) / 255.0
                        gSum += ((c shr 8) and 0xFF) / 255.0
                        bSum += (c and 0xFF) / 255.0
                        count++
                        x += stepX
                    }
                    y += stepY
                }
                val safe = count.coerceAtLeast(1)
                val baseIdx = (row * 3 + col) * 3
                sig[baseIdx] = (rSum / safe).toFloat()
                sig[baseIdx + 1] = (gSum / safe).toFloat()
                sig[baseIdx + 2] = (bSum / safe).toFloat()
            }
        }
        return sig
    }
}

object SmartTimelineEngine {

    /**
     * Reorders screenshots so consecutive shots avoid near-identical scene signatures.
     */
    fun shuffleAntiSameScene(items: List<ScreenshotItem>): List<ScreenshotItem> {
        if (items.size <= 2) return items
        val remaining = items.toMutableList()
        val ordered = mutableListOf(remaining.removeAt(0))

        while (remaining.isNotEmpty()) {
            val prevSig = ordered.last().sceneSignature
            var bestIdx = 0
            var bestDist = -1f

            for (i in remaining.indices) {
                val candidateSig = remaining[i].sceneSignature
                val dist = signatureDistance(prevSig, candidateSig)
                if (dist > bestDist) {
                    bestDist = dist
                    bestIdx = i
                }
            }
            ordered.add(remaining.removeAt(bestIdx))
        }
        return ordered.mapIndexed { idx, item -> item.copy(index = idx + 1) }
    }

    private fun signatureDistance(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size || a.isEmpty()) return 1f
        var sum = 0f
        for (i in a.indices) {
            sum += abs(a[i] - b[i])
        }
        return sum / a.size
    }
}
