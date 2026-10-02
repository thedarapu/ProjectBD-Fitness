package net.darapu.projectbd.domain.models

enum class ExperienceLevel(val displayName: String, val description: String) {
    BEGINNER("Beginner", "Less than 1 year of consistent training. Focus on form and linear progression."),
    INTERMEDIATE("Intermediate", "1-3 years of consistent training. Needs more complex programming for gains."),
    ADVANCED("Advanced", "3+ years of consistent training. Gains are slower and require high specialization.")
}

enum class EquipmentProfile(val displayName: String, val description: String) {
    FULL_GYM("Full Gym", "Access to barbells, dumbbells, cables, and machines."),
    DUMBBELLS_ONLY("Dumbbells Only", "Limited to dumbbells and bench/adjustable seat."),
    BODYWEIGHT("Bodyweight", "No equipment available. Focus on calisthenics.")
}
