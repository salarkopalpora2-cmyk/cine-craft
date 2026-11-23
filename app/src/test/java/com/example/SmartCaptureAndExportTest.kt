package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.example.data.FaceDetectionProvider
import com.example.data.GalleryExportHelper
import com.example.data.SmartFrameAnalyzer
import com.example.model.DetectedSubjectFace
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmartCaptureAndExportTest {

    private fun createPortraitWithTorsoBitmap(
        width: Int = 640,
        height: Int = 360,
        withCornerYoutubeBadge: Boolean = false,
        withCenterCreditsText: Boolean = false
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Clean interior room wall background
        canvas.drawColor(Color.rgb(42, 48, 62))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Draw textured hair + sharp facial features in center
        paint.color = Color.rgb(30, 24, 22)
        canvas.drawCircle(width * 0.5f, height * 0.34f, 44f, paint)
        paint.color = Color.rgb(218, 168, 134)
        canvas.drawOval(
            width * 0.5f - 34f,
            height * 0.24f,
            width * 0.5f + 34f,
            height * 0.45f,
            paint
        )
        // Eyes + brows for crisp local sharpness in face ROI
        paint.color = Color.rgb(24, 24, 24)
        canvas.drawCircle(width * 0.5f - 14f, height * 0.33f, 5f, paint)
        canvas.drawCircle(width * 0.5f + 14f, height * 0.33f, 5f, paint)
        canvas.drawRect(width * 0.5f - 22f, height * 0.30f, width * 0.5f - 6f, height * 0.31f, paint)
        canvas.drawRect(width * 0.5f + 6f, height * 0.30f, width * 0.5f + 22f, height * 0.31f, paint)
        paint.color = Color.rgb(160, 72, 68)
        canvas.drawRect(width * 0.5f - 10f, height * 0.40f, width * 0.5f + 10f, height * 0.415f, paint)

        // Draw structured shoulders/torso shirt below face
        paint.color = Color.rgb(112, 34, 40)
        canvas.drawRect(width * 0.5f - 78f, height * 0.48f, width * 0.5f + 78f, height * 0.92f, paint)
        paint.color = Color.rgb(210, 200, 190)
        for (xOff in -66..66 step 18) {
            canvas.drawRect(
                width * 0.5f + xOff,
                height * 0.49f,
                width * 0.5f + xOff + 4f,
                height * 0.90f,
                paint
            )
        }

        if (withCornerYoutubeBadge) {
            // Red YouTube SUBSCRIBE/logo pill + white badge text in top-right corner
            paint.color = Color.rgb(235, 22, 22)
            canvas.drawRect(width - 145f, 16f, width - 16f, 58f, paint)
            paint.color = Color.WHITE
            paint.textSize = 20f
            canvas.drawText("SUBSCRIBE", width - 135f, 44f, paint)
        }

        if (withCenterCreditsText) {
            paint.color = Color.WHITE
            paint.textSize = 26f
            val lines = listOf(
                "EXECUTIVE PRODUCER: AB ROKON",
                "CHIEF ASSISTANT DIRECTOR",
                "MD OMAR FARUK • MEHEDI HASAN",
                "CINEMATOGRAPHY & EDITING TEAM"
            )
            lines.forEachIndexed { idx, line ->
                canvas.drawText(line, width * 0.16f, height * (0.35f + idx * 0.12f), paint)
            }
        }
        return bmp
    }

    private fun stubFaceAtCenter(width: Int = 640, height: Int = 360) = FaceDetectionProvider {
        listOf(
            DetectedSubjectFace(
                centerX = width * 0.5f,
                centerY = height * 0.34f,
                eyesDistance = 28f,
                confidence = 0.85f
            )
        )
    }

    @Test
    fun `rejects distant scenery or empty drone shot with no detected human face`() {
        val skyline = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(skyline)
        val p = Paint()
        // Pale blue sky top + tiny high-frequency gray/brown rooftop blocks
        p.color = Color.rgb(175, 210, 235)
        canvas.drawRect(0f, 0f, 640f, 140f, p)
        for (x in 0 until 640 step 16) {
            p.color = if ((x / 16) % 2 == 0) Color.rgb(110, 118, 125) else Color.rgb(155, 130, 115)
            canvas.drawRect(x.toFloat(), 140f, (x + 12).toFloat(), 360f, p)
        }

        val result = SmartFrameAnalyzer.processAndVerifyFrame(
            sourceBitmap = skyline,
            enableSmartFilter = true,
            faceDetector = FaceDetectionProvider { emptyList() }
        )

        assertFalse("Skyline without subject must be rejected", result.accepted)
        assertFalse(result.evaluation.hasValidSubject)
        skyline.recycle()
    }

    @Test
    fun `rejects frame covered with opening credit text`() {
        val creditFrame = createPortraitWithTorsoBitmap(withCenterCreditsText = true)
        val result = SmartFrameAnalyzer.processAndVerifyFrame(
            sourceBitmap = creditFrame,
            enableSmartFilter = true,
            faceDetector = stubFaceAtCenter()
        )

        assertFalse("Opening credits frame must be rejected", result.accepted)
        assertTrue(result.evaluation.hasHeavyUnremovableText)
        creditFrame.recycle()
    }

    @Test
    fun `detects cleans and re-verifies corner YouTube watermark on valid subject frame`() {
        val watermarked = createPortraitWithTorsoBitmap(withCornerYoutubeBadge = true)
        val faceStub = stubFaceAtCenter()

        val initialEval = SmartFrameAnalyzer.evaluateFrameAnalysis(
            bitmap = watermarked,
            requireSubject = true,
            faceDetector = faceStub
        )
        assertTrue("Initial frame should detect valid subject, eval=$initialEval", initialEval.hasValidSubject)
        assertTrue("Initial frame should detect corner YouTube overlay, eval=$initialEval", initialEval.overlayRegions.isNotEmpty())

        val processed = SmartFrameAnalyzer.processAndVerifyFrame(
            sourceBitmap = watermarked,
            enableSmartFilter = true,
            faceDetector = faceStub
        )

        assertTrue("Cleaned & re-verified frame should pass: eval=${processed.evaluation}, reason=${processed.rejectionReason}", processed.accepted)
        assertTrue("Should flag that watermark was cleaned", processed.wasWatermarkCleaned)
        assertTrue("Re-verified evaluation should have zero remaining overlays: ${processed.evaluation.overlayRegions}", processed.evaluation.overlayRegions.isEmpty())
        processed.bitmap?.recycle()
        watermarked.recycle()
    }

    @Test
    fun `accepts clean subject frame without altering pixels`() {
        val clean = createPortraitWithTorsoBitmap()
        val processed = SmartFrameAnalyzer.processAndVerifyFrame(
            sourceBitmap = clean,
            enableSmartFilter = true,
            faceDetector = stubFaceAtCenter()
        )

        assertTrue("Clean frame failed: eval=${processed.evaluation}, reason=${processed.rejectionReason}", processed.accepted)
        assertFalse(processed.wasWatermarkCleaned)
        assertTrue(processed.evaluation.hasValidSubject)
        clean.recycle()
    }

    @Test
    fun `creates non-empty verified ZIP archive with intact readable JPG entries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val img1 = createPortraitWithTorsoBitmap()
        val img2 = createPortraitWithTorsoBitmap()

        val bytes1 = ByteArrayOutputStream().use { baos ->
            img1.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            baos.toByteArray()
        }
        val bytes2 = ByteArrayOutputStream().use { baos ->
            img2.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            baos.toByteArray()
        }
        img1.recycle()
        img2.recycle()

        val outZip = File(context.cacheDir, "test_export_bundle.zip")
        outZip.delete()

        val verification = GalleryExportHelper.createAndVerifyZipArchive(
            entries = listOf(
                "01_Shot_01_0405.jpg" to bytes1,
                "02_Shot_02_0409.jpg" to bytes2
            ),
            outZipFile = outZip
        )

        assertTrue(verification.isValid)
        assertEquals(2, verification.entryCount)
        assertTrue(outZip.exists() && outZip.length() > 100L)

        // Extract and verify exact byte integrity
        val extracted = mutableMapOf<String, ByteArray>()
        ZipInputStream(FileInputStream(outZip)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                extracted[entry.name] = zis.readBytes()
                zis.closeEntry()
            }
        }
        assertEquals(2, extracted.size)
        assertArrayEquals(bytes1, extracted["01_Shot_01_0405.jpg"])
        assertArrayEquals(bytes2, extracted["02_Shot_02_0409.jpg"])
        outZip.delete()
    }
}
