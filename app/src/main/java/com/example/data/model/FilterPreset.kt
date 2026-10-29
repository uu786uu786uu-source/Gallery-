package com.example.data.model

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    WARM("Warm"),
    COOL("Cool"),
    MONOCHROME("B&W"),
    SEPIA("Sepia"),
    CYBERPUNK("Cyber"),
    DRAMA("Drama");

    fun toColorFilter(): ColorFilter? {
        return when (this) {
            ORIGINAL -> null
            VIVID -> {
                // Boost saturation & contrast
                val matrix = ColorMatrix().apply {
                    setToSaturation(1.4f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            WARM -> {
                // Enhance red & yellow tint
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 15f,
                        0f, 1.05f, 0f, 0f, 10f,
                        0f, 0f, 0.85f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            COOL -> {
                // Enhance cyan & blue tint
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.85f, 0f, 0f, 0f, -5f,
                        0f, 1.0f, 0f, 0f, 5f,
                        0f, 0f, 1.25f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            MONOCHROME -> {
                val matrix = ColorMatrix().apply {
                    setToSaturation(0f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            SEPIA -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            CYBERPUNK -> {
                // High contrast with neon magenta & teal
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0.2f, 0f, 20f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0.1f, 0f, 1.4f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            DRAMA -> {
                // High contrast cinematic
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -20f,
                        0f, 1.35f, 0f, 0f, -20f,
                        0f, 0f, 1.35f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
        }
    }
}
