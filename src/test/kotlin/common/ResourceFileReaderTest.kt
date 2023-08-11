package common

import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceFileReaderTest {

    private val reader = ResourceFileReader()

    @Test
    fun testRealFileRead() {
        val contents = reader.readContents("test.txt")
        assertEquals("File contents don't match", "Hello World!", contents)
    }

}