package api

import di.PackageProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import util.DataProvider

class RepositoryGeneratorTest {

    lateinit var generator: RepositoryGenerator

    @BeforeEach
    fun setUp() {
        generator = RepositoryGenerator(
            packageProvider = PackageProvider("com.ltossou.app"),
        )
    }

    @Test
    fun generate() {
        println(generator.generate(api = DataProvider.API.SEATGEEK, localSource = true, entities = emptyList(),
            models = emptyMap()
        ).build())
    }
}