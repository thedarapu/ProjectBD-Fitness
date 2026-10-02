package net.darapu.projectbd.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import net.darapu.projectbd.R
import net.darapu.projectbd.domain.models.Exercise
import net.darapu.projectbd.domain.models.MuscleGroup

import androidx.annotation.DrawableRes

@Composable
fun MuscleGroupMap(exercise: Exercise, modifier: Modifier = Modifier) {
    // Determine if we should show the front or back view
    val muscleList = (exercise.primaryMuscles + exercise.secondaryMuscles).toSet()
    val isBackView = muscleList.any { 
        it == MuscleGroup.BACK || it == MuscleGroup.HAMSTRINGS || it == MuscleGroup.TRICEPS || it == MuscleGroup.CALVES
    }

    Box(
        modifier = modifier
            .aspectRatio(0.5f) // Optimized for 700:1400 anatomy viewport
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Base Body Outline
        Image(
            painter = painterResource(id = if (isBackView) R.drawable.ic_body_back_outline else R.drawable.ic_body_front_outline),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.4f,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )

        // 2. Secondary Muscles (Subtle Highlight)
        exercise.secondaryMuscles.forEach { muscle ->
            val resId = getMuscleDrawable(muscle)
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                )
            }
        }

        // 3. Primary Muscles (Bright Impact Highlight)
        exercise.primaryMuscles.forEach { muscle ->
            val resId = getMuscleDrawable(muscle)
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@DrawableRes
private fun getMuscleDrawable(muscle: MuscleGroup): Int? {
    return when (muscle) {
        MuscleGroup.CHEST -> R.drawable.ic_muscle_chest
        MuscleGroup.BACK -> R.drawable.ic_muscle_back
        MuscleGroup.QUADS -> R.drawable.ic_muscle_quads
        MuscleGroup.HAMSTRINGS -> R.drawable.ic_muscle_hamstrings
        MuscleGroup.SHOULDERS -> R.drawable.ic_muscle_shoulders
        MuscleGroup.BICEPS -> R.drawable.ic_muscle_biceps
        MuscleGroup.TRICEPS -> R.drawable.ic_muscle_triceps
        MuscleGroup.CORE -> R.drawable.ic_muscle_core
        MuscleGroup.CALVES -> R.drawable.ic_muscle_calves
    }
}
