package com.hyouka.video60fps4k

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsCommandTest {

    @Test
    fun buildsAllResolutionTargets() {
        val expected = mapOf(
            OutputResolution.HD_1080P to "scale=1920:1080",
            OutputResolution.QHD_2K to "scale=2560:1440",
            OutputResolution.UHD_4K to "scale=3840:2160"
        )

        expected.forEach { (resolution, fragment) ->
            val filter = VideoProcessingSpec.buildFilter(
                ConversionSettings(resolution, OutputFps.FPS_60)
            )
            assertTrue(filter.contains("minterpolate=fps=60"))
            assertTrue(filter.contains(fragment))
            assertTrue(filter.contains("pad=${resolution.width}:${resolution.height}"))
        }
    }

    @Test
    fun buildsEveryRequestedFrameRate() {
        OutputFps.entries.forEach { fps ->
            val command = VideoProcessingSpec.buildCommand(
                "/input/source video.mp4",
                "/output/result.mp4",
                ConversionSettings(OutputResolution.UHD_4K, fps)
            )
            assertTrue(command.contains("minterpolate=fps=${fps.value}"))
            assertTrue(command.contains("-r ${fps.value}"))
            assertTrue(command.contains("-fps_mode cfr"))
        }

        assertEquals(6, OutputFps.entries.size)
    }

    @Test
    fun commandQuotesPathsAndKeepsAudio() {
        val command = VideoProcessingSpec.buildCommand(
            "/input/my video.mp4",
            "/output/my result.mp4",
            ConversionSettings(OutputResolution.QHD_2K, OutputFps.FPS_120)
        )

        assertTrue(command.contains("'/input/my video.mp4'"))
        assertTrue(command.contains("'/output/my result.mp4'"))
        assertTrue(command.contains("-map 0:a?"))
        assertTrue(command.contains("scale=2560:1440"))
    }
}
