package dev.jvald.selectandchat.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive shape, type and motion, written out by hand.
 *
 * material3 1.4.0 ships the Expressive *components* publicly but keeps the Expressive
 * motion scheme and the emphasized type roles internal (they carry Kotlin's `$material3`
 * name mangling), and its `Shapes` still only has the five classic tokens. Those are only
 * public in the 1.5.0 alphas, which would drag in AGP 9 and an alpha dependency. The spec
 * itself is simple enough to express directly, so this file does that.
 */

/**
 * Expressive corners are noticeably rounder than the baseline (4/8/12/16/28), and the
 * jump between steps is bigger so different surfaces read as different shapes rather
 * than as one uniform radius.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp()),
    small = RoundedCornerShape(12.dp()),
    medium = RoundedCornerShape(18.dp()),
    large = RoundedCornerShape(26.dp()),
    extraLarge = RoundedCornerShape(36.dp()),
)

/**
 * "Emphasized" in the Expressive spec means more weight, and tighter tracking as text
 * gets larger. Body copy deliberately stays at normal weight: emphasising everything
 * emphasises nothing, and long text gets harder to read.
 */
val ExpressiveTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        ),
        displayMedium = displayMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        displaySmall = displaySmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
        ),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Medium),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/**
 * Expressive motion is spring-based rather than curve-based: things settle with a little
 * overshoot instead of easing to a stop. These are the two springs the app uses.
 */
object ExpressiveMotion {

    /** For things that move a visible distance, such as a screen change. */
    fun <T> spatial() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** For things that only fade or recolour, where overshoot would look like a glitch. */
    fun <T> effects() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
