package com.hyouka.video60fps4k

enum class OutputResolution(
    val label: String,
    val width: Int,
    val height: Int
) {
    ORIGINAL("Original", 0, 0),
    HD_1080P("1080p", 1920, 1080),
    QHD_2K("2K", 2560, 1440),
    UHD_4K("4K", 3840, 2160);

    val keepsSourceResolution: Boolean
        get() = this == ORIGINAL
}

enum class OutputFps(val value: Int) {
    FPS_60(60),
    FPS_90(90),
    FPS_120(120),
    FPS_144(144),
    FPS_240(240),
    FPS_360(360)
}

data class ConversionSettings(
    val resolution: OutputResolution,
    val fps: OutputFps
)

object VideoProcessingSpec {
    fun buildFilter(settings: ConversionSettings): String {
        val fps = settings.fps.value
        val interpolation = "minterpolate=fps=\${fps}:mi_mode=mci:mc_mode=aobmc:me_mode=bidir:vsbmc=1"

        if (settings.resolution.keepsSourceResolution) {
            return interpolation
        }

        val width = settings.resolution.width
        val height = settings.resolution.height

        return interpolation +
            ",scale=\${width}:\${height}:force_original_aspect_ratio=decrease," +
            "pad=\${width}:\${height}:(ow-iw)/2:(oh-ih)/2"
    }

    fun buildCommand(input: String, output: String, settings: ConversionSettings): String {
        return listOf(
            "-y",
            "-i", quote(input),
            "-vf", quote(buildFilter(settings)),
            "-map", "0:v:0",
            "-map", "0:a?",
            "-c:v", "libx264",
            "-preset", "medium",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-r", settings.fps.value.toString(),
            "-fps_mode", "cfr",
            "-c:a", "aac",
            "-b:a", "192k",
            "-movflags", "+faststart",
            quote(output)
        ).joinToString(" ")
    }

    private fun quote(value: String): String =
        "'" + value.replace("'", "'\\\\''") + "'"
}
