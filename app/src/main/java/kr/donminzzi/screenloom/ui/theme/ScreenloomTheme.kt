package kr.donminzzi.screenloom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFFFF8E9)
val ElevatedPaper = Color(0xFFFFFFFF)
val Ink = Color(0xFF18213D)
val MutedInk = Color(0xFF667087)
val Cobalt = Color(0xFF566EFF)
val Coral = Color(0xFFFF6B4A)
val Sun = Color(0xFFFFD466)
val Mint = Color(0xFF6BD7B3)
val Outline = Color(0xFFE6DCCB)
val SelectedWash = Color(0xFFFFF0B9)

private val ScreenloomColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Paper,
    secondary = Coral,
    onSecondary = Ink,
    tertiary = Sun,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = ElevatedPaper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF7F4ED),
    onSurfaceVariant = MutedInk,
    outline = Outline,
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
