package com.fractalwrench.json2kotlin

import com.google.gson.JsonSyntaxException
import java.io.InputStream
import java.io.OutputStream

/**
 * Converts JSON to Kotlin
 */
class Kotlin2JsonConverter(private val buildDelegate: SourceBuildDelegate = GsonBuildDelegate()) {

    private val jsonReader = JsonReader()
    private val sourceFileWriter = SourceFileWriter()
    private val treeTraverser = ReverseJsonTreeTraverser()

    /**
     * Converts an InputStream of JSON to Kotlin source code, writing the result to the OutputStream.
     */
    fun convert(input: InputStream, output: OutputStream, args: ConversionArgs): ConversionInfo {
        try {
            val jsonRoot = jsonReader.readJsonTree(input, args)
            val jsonStack = treeTraverser.traverse(jsonRoot, args.rootClassName)
            val generator = TypeSpecGenerator(buildDelegate, ::defaultGroupingStrategy)
            val typeSpecs = generator.generateTypeSpecs(jsonStack)

            val name = typeSpecs.peek().build().name
            val fileSpec = sourceFileWriter.writeSourceFile(typeSpecs, args, output)
            return ConversionInfo("${args.packageName.orEmpty().replace(".", "/")}/$name.kt", file = fileSpec)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid JSON supplied", e)
        }
    }

}