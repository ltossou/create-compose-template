package model

import com.squareup.kotlinpoet.FileSpec

interface DataInput<T> {
    fun fromInput(modelsSpecs: List<FileSpec> = emptyList()): T
}