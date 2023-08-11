package util

fun String.toSnakeCase() = split(Regex("(?=\\p{Upper})")).joinToString(separator = "_").toLowerCase()

fun String.toCamelCase(): String = split("_")
    .mapIndexed { index, s -> if (index > 0) s.capitalize() else s }
    .joinToString(separator = "")