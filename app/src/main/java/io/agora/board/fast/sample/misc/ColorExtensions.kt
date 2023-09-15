package io.agora.board.fast.sample.misc

import android.graphics.Color

fun Int.toColorArray(): IntArray {
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)

    return intArrayOf(red, green, blue)
}