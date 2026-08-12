package kr.donminzzi.screenloom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF090B10)
val WarmWhite = Color(0xFFF5F1E8)
val Cobalt = Color(0xFF5B7CFA)
val Coral = Color(0xFFFF7A6E)

private val ScreenloomColors = darkColorScheme(
    primary = Cobalt,
    secondary = Coral,
    background = Ink,
    surface = Color(0xFF151922),
    surfaceVariant = Color(0xFF202633),
    onPrimary = Color.White,
    onSecondary = Ink,
    onBackground = WarmWhite,
    onSurface = WarmWhite,
    onSurfaceVariant = Color(0xFFBCC3D2),
    outline = Color(0xFF353C4B),
)

private val ScreenloomTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1).sp,
        lineHeight = 39.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 23.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    ),
)

private val ScreenloomShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun ScreenloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScreenloomColors,
        typography = ScreenloomTypography,
        shapes = ScreenloomShapes,
        content = content,
    )
}
