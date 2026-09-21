package dev.jvald.selectandchat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import dev.jvald.selectandchat.R

/**
 * The app's icon set: Material Symbols Rounded at weight 400, optical size 24, as served
 * by fonts.google.com/icons.
 *
 * Not `Icons.Default.*`. That is the 2014 Material icon set, which material3 still ships
 * for compatibility — rounder corners and a different stroke weight from everything else
 * in an Expressive layout. The drawables are vendored into `res/drawable` rather than
 * taken from `material-icons-extended`, which is several megabytes of dead weight for the
 * fifteen glyphs this app draws.
 *
 * Each one is a plain black fill that the caller tints, exactly as `Icon` expects, and
 * the two directional glyphs carry `autoMirrored` so they flip in a right-to-left layout.
 */
object AppIcons {
    val Add: Painter
        @Composable get() = painterResource(R.drawable.ic_add)

    val ChatBubble: Painter
        @Composable get() = painterResource(R.drawable.ic_chat_bubble)

    val Check: Painter
        @Composable get() = painterResource(R.drawable.ic_check)

    /** Points into a row that opens something. Mirrored in RTL. */
    val Chevron: Painter
        @Composable get() = painterResource(R.drawable.ic_chevron_right)

    val Close: Painter
        @Composable get() = painterResource(R.drawable.ic_close)

    val Delete: Painter
        @Composable get() = painterResource(R.drawable.ic_delete)

    val Edit: Painter
        @Composable get() = painterResource(R.drawable.ic_edit)

    /** Rotated 180° by the caller when the thing it belongs to is open. */
    val ExpandMore: Painter
        @Composable get() = painterResource(R.drawable.ic_keyboard_arrow_down)

    val Info: Painter
        @Composable get() = painterResource(R.drawable.ic_info)

    val Language: Painter
        @Composable get() = painterResource(R.drawable.ic_language)

    val MoreVert: Painter
        @Composable get() = painterResource(R.drawable.ic_more_vert)

    /** Leaves the app for the contacts editor, so it is the "add" person, not a person. */
    val PersonAdd: Painter
        @Composable get() = painterResource(R.drawable.ic_person_add)

    val OpenInNew: Painter
        @Composable get() = painterResource(R.drawable.ic_open_in_new)

    val Palette: Painter
        @Composable get() = painterResource(R.drawable.ic_palette)

    val Search: Painter
        @Composable get() = painterResource(R.drawable.ic_search)

    val Settings: Painter
        @Composable get() = painterResource(R.drawable.ic_settings)
}
