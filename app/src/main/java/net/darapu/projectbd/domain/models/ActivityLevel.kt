package net.darapu.projectbd.domain.models

enum class ActivityLevel(val multiplier: Float, val description: String) {
    SEDENTARY(1.2f, "Little or no exercise"),
    LIGHTLY_ACTIVE(1.375f, "Light exercise 1-3 days/week"),
    MODERATELY_ACTIVE(1.55f, "Moderate exercise 3-5 days/week"),
    VERY_ACTIVE(1.725f, "Hard exercise 6-7 days/week"),
    EXTRA_ACTIVE(1.9f, "Very hard exercise & physical job")
}
