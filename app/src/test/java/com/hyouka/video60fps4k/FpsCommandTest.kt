package com.hyouka.video60fps4k

import org.junit.Assert.assertTrue
import org.junit.Test

class FpsCommandTest {
    @Test
    fun targetSpecContainsInterpolationAnd4K() {
        val spec =
            "minterpolate=fps=60:mi_mode=mci:mc_mode=aobmc:me_mode=bidir:vsbmc=1," +
                "scale=3840:2160:force_original_aspect_ratio=decrease," +
                "pad=3840:2160:(ow-iw)/2:(oh-ih)/2"
        assertTrue(spec.contains("minterpolate=fps=60"))
        assertTrue(spec.contains("scale=3840:2160"))
        assertTrue(spec.contains("pad=3840:2160"))
    }
}
