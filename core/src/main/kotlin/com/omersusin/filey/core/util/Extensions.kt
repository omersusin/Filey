package com.omersusin.filey.core.util

import java.io.File

fun String.toFile(): File = File(this)

fun File.sizeRecursive(): Long {
    if (isFile) return length()
    return walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

fun String.parentPath(): String {
    return File(this).parent ?: "/"
}
