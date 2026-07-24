package com.rodvarled.admin.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

/** Entrada escalonada (fade + slide) para elementos de una lista, según el índice. */
fun staggeredEnter(index: Int, baseDelayMs: Int = 60, durationMs: Int = 300, startDelayMs: Int = 0): EnterTransition {
    val delay = startDelayMs + index * baseDelayMs
    return fadeIn(tween(durationMs, delayMillis = delay)) +
        slideInVertically(tween(durationMs, delayMillis = delay)) { it / 8 }
}
