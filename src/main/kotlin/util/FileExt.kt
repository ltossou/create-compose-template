package util

import java.io.File

fun File.replaceLine(newText: String, findLineCondition: (String) -> Boolean) {
    val existingProperty = readLines().find { line -> findLineCondition(line) }
    if (existingProperty != null) {
        writeText(readText().replace(existingProperty, newText))
    }
}

fun File.replace(oldText: String, newText: String) {
    writeText(readText().replace(oldText, newText))
}