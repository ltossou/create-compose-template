import org.junit.jupiter.api.Assertions.*
import util.DataProvider
import util.println
import kotlin.test.Test

class JsonConverterTest {

    @Test
    fun generate() {
        val api = DataProvider.API.SEATGEEK
        val modelResult = JsonConverter().generate(
            input = api.endpoints[0].json,
            rootClassName = "${api.endpoints.first().name.capitalize()}Response",
            packageName = "com.ltossou"
        )
        modelResult.file.println()
    }
}