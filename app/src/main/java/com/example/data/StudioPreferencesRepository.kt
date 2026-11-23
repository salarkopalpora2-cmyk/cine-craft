package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AspectRatioPreset
import com.example.model.CanvasOptions
import com.example.model.CaptureDefaults
import com.example.model.ExportProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinecraft_studio_prefs"
)

data class CaptureDefaultPrefs(
    val zoomLevel: Float = CaptureDefaults.DEFAULT_ZOOM_LEVEL,
    val startTrimSec: Int = CaptureDefaults.DEFAULT_START_TRIM_SEC,
    val endTrimSec: Int = CaptureDefaults.DEFAULT_END_TRIM_SEC
)

class StudioPreferencesRepository(private val context: Context) {

    private object Keys {
        val ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        val SHOW_GRID = booleanPreferencesKey("show_grid")
        val SHOW_SAFE_ZONES = booleanPreferencesKey("show_safe_zones")
        val LUT_PRESET = stringPreferencesKey("lut_preset")
        val EXPORT_RESOLUTION = stringPreferencesKey("export_resolution")
        val EXPORT_FPS = stringPreferencesKey("export_fps")
        val EXPORT_CODEC = stringPreferencesKey("export_codec")
        val EXPORT_BITRATE = stringPreferencesKey("export_bitrate")
        val EXPORT_PLATFORM = stringPreferencesKey("export_platform")

        // Smart Capture persistent defaults (mirroring vss_default_* keys)
        val VSS_DEFAULT_ZOOM = floatPreferencesKey("vss_default_zoom_level")
        val VSS_DEFAULT_START_TRIM = intPreferencesKey("vss_default_start_trim")
        val VSS_DEFAULT_END_TRIM = intPreferencesKey("vss_default_end_trim")
    }

    val canvasOptionsFlow: Flow<CanvasOptions> = context.studioDataStore.data.map { prefs ->
        val ratioName = prefs[Keys.ASPECT_RATIO]
        val aspect = AspectRatioPreset.entries.firstOrNull { it.name == ratioName }
            ?: AspectRatioPreset.WIDE_16_9
        CanvasOptions(
            aspectRatio = aspect,
            showGridLines = prefs[Keys.SHOW_GRID] ?: true,
            showSafeZones = prefs[Keys.SHOW_SAFE_ZONES] ?: true,
            lutPreset = prefs[Keys.LUT_PRESET] ?: "CineBlue LUT"
        )
    }

    val exportProfileFlow: Flow<ExportProfile> = context.studioDataStore.data.map { prefs ->
        ExportProfile(
            resolution = prefs[Keys.EXPORT_RESOLUTION] ?: "4K UHD",
            frameRate = prefs[Keys.EXPORT_FPS] ?: "24 fps",
            codec = prefs[Keys.EXPORT_CODEC] ?: "H.265 HEVC",
            bitrate = prefs[Keys.EXPORT_BITRATE] ?: "45 Mbps",
            targetPlatform = prefs[Keys.EXPORT_PLATFORM] ?: "YouTube Cinema"
        )
    }

    val captureDefaultsFlow: Flow<CaptureDefaultPrefs> = context.studioDataStore.data.map { prefs ->
        CaptureDefaultPrefs(
            zoomLevel = (prefs[Keys.VSS_DEFAULT_ZOOM] ?: CaptureDefaults.DEFAULT_ZOOM_LEVEL)
                .coerceIn(CaptureDefaults.MIN_ZOOM_LEVEL, CaptureDefaults.MAX_ZOOM_LEVEL),
            startTrimSec = (prefs[Keys.VSS_DEFAULT_START_TRIM] ?: CaptureDefaults.DEFAULT_START_TRIM_SEC)
                .coerceAtLeast(0),
            endTrimSec = (prefs[Keys.VSS_DEFAULT_END_TRIM] ?: CaptureDefaults.DEFAULT_END_TRIM_SEC)
                .coerceAtLeast(0)
        )
    }

    suspend fun saveCanvasOptions(options: CanvasOptions) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.ASPECT_RATIO] = options.aspectRatio.name
            prefs[Keys.SHOW_GRID] = options.showGridLines
            prefs[Keys.SHOW_SAFE_ZONES] = options.showSafeZones
            prefs[Keys.LUT_PRESET] = options.lutPreset
        }
    }

    suspend fun saveExportProfile(profile: ExportProfile) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.EXPORT_RESOLUTION] = profile.resolution
            prefs[Keys.EXPORT_FPS] = profile.frameRate
            prefs[Keys.EXPORT_CODEC] = profile.codec
            prefs[Keys.EXPORT_BITRATE] = profile.bitrate
            prefs[Keys.EXPORT_PLATFORM] = profile.targetPlatform
        }
    }

    suspend fun saveCaptureZoom(zoom: Float) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.VSS_DEFAULT_ZOOM] = zoom.coerceIn(
                CaptureDefaults.MIN_ZOOM_LEVEL,
                CaptureDefaults.MAX_ZOOM_LEVEL
            )
        }
    }

    suspend fun saveCaptureTrims(startTrimSec: Int, endTrimSec: Int) {
        context.studioDataStore.edit { prefs ->
            prefs[Keys.VSS_DEFAULT_START_TRIM] = startTrimSec.coerceAtLeast(0)
            prefs[Keys.VSS_DEFAULT_END_TRIM] = endTrimSec.coerceAtLeast(0)
        }
    }
}
