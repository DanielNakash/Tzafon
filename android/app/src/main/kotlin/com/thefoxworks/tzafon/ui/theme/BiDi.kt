package com.thefoxworks.tzafon.ui.theme

/**
 * FR-DESIGN-3.6.1 — first-strong-character direction test for a user-authored
 * string. Mirrors what Compose's TextDirection.Content resolves at the text
 * layer, but returned as a plain boolean so the caller can drive row-level
 * layout (LocalLayoutDirection). Whitespace and neutral characters are skipped
 * until a directional character is found; empty or purely-neutral strings
 * resolve to LTR (the app's ambient direction).
 */
fun String.isRtl(): Boolean {
    for (ch in this) {
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE -> return true
            Character.DIRECTIONALITY_LEFT_TO_RIGHT,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE,
            Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE -> return false
            else -> Unit // whitespace, digits, neutrals — keep scanning
        }
    }
    return false
}
