package com.fractalwrench.json2kotlin

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.*

@RunWith(Parameterized::class)
open class JsonConverterTest(val expectedFilename: String, val jsonFilename: String) {

    private val fileReader = ResourceFileReader()
    private val jsonConverter = Kotlin2JsonConverter()
    internal lateinit var json: InputStream

    @Before
    fun setUp() {
        json = fileReader.inputStream(jsonFilename)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "File {0}")
        fun filenamePairs(): Collection<Array<String>> {
            val dir = File(JsonConverterTest::class.java.classLoader.getResource("valid").file)
            val arrayList = ArrayList<Array<String>>()
            findTestCaseFiles(dir, dir, arrayList)
            return arrayList
        }
        /**
         * Recurses through the resources directory and add test case files to the collection
         */
        private fun findTestCaseFiles(baseDir: File, currentDir: File, testCases: MutableCollection<Array<String>>) {
            currentDir.listFiles().forEach {
                if (it.extension == "json") {
                    val name = it.nameWithoutExtension
                    val json = File(currentDir, "$name.json")
                    val kt = File(currentDir, "${name}Example.kt")

                    Assert.assertTrue("Expected to find test case file " + json, json.exists())
                    Assert.assertTrue("Expected to find test case file " + kt, kt.exists())
                    val element = arrayOf(normalisePath(kt, baseDir), normalisePath(json, baseDir))
                    testCases.add(element)

                } else if (it.isDirectory) {
                    findTestCaseFiles(baseDir, it, testCases)
                }
            }
        }

        private fun normalisePath(json: File, baseDir: File) = json.relativeTo(baseDir.parentFile).path
    }


    /**
     * Takes a JSON file and converts it into the equivalent Kotlin class, then compares to expected output.
     */
    @Test
    fun testJsonToKotlinConversion() {
        val outputStream = ByteArrayOutputStream()
        val rootClassName = classNameForFile(expectedFilename)
        jsonConverter.convert(json, outputStream, ConversionArgs(rootClassName))

        val generatedSource = String(outputStream.toByteArray()).standardiseNewline()
        val expectedContents = fileReader.readContents(expectedFilename).standardiseNewline()
        val msg = "Generated file doesn't match expected file \'$expectedFilename\'"
        Assert.assertEquals(msg, expectedContents, generatedSource)
    }

    @Test
    fun testStackOrder() {
        val readJsonTree = JsonReader().readJsonTree(json, ConversionArgs())
        val stack = ReverseJsonTreeTraverser().traverse(readJsonTree, "Foo")
        var maxLevel = 0
        stack.forEach {
            if (maxLevel > it.level) {
                Assert.fail("Stack is not in correct order: $it")
            }
            maxLevel = it.level
        }
    }

    private fun classNameForFile(filename: String): String
            = filename.replace(".kt", "").substringAfterLast(File.separator)
}