package com.fractalwrench.json2kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.*

/**
 * Writes a collection of types to a source file OutputStream.
 */
internal class SourceFileWriter {

    /**
     * Writes a collection of types to a source file OutputStream.
     */
    fun writeSourceFile(stack: Stack<TypeSpec.Builder>, args: ConversionArgs, output: OutputStream): FileSpec {
        val packageName = args.packageName ?: ""
        val sourceFile = FileSpec.builder(packageName, args.rootClassName)

        var i = 0
        var rootClass: TypeSpec.Builder? = null
        while (stack.isNotEmpty()) {
            if (i == 0) {
                rootClass = stack.pop()
                i++
                continue
            }
            rootClass?.addType(stack.pop().build())
            // sourceFile.addType(stack.pop())
            i++
        }
        rootClass?.build()?.let { sourceFile.addType(it) }

        // BufferedWriter(OutputStreamWriter(output)).use {
            sourceFile.build().writeTo(OutputStreamWriter(output))
            // it.flush()
        // }
        return sourceFile.build()
    }

}