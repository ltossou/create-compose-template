package common

import java.io.InputStream

class ResourceFileReader {
    fun readContents(resourceName: String): String {
        val resource = ResourceFileReader::class.java.classLoader.getResource(resourceName)
        return resource?.readText() ?: ""
    }

    fun inputStream(resourceName: String): InputStream? {
        return ResourceFileReader::class.java.classLoader.getResourceAsStream(resourceName)
    }
}