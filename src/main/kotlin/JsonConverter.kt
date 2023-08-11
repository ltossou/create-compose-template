import com.fractalwrench.json2kotlin.ConversionArgs
import com.fractalwrench.json2kotlin.ConversionInfo
import com.fractalwrench.json2kotlin.Kotlin2JsonConverter
import com.fractalwrench.json2kotlin.MoshiBuildDelegate

class JsonConverter {
    fun generate(input: String, rootClassName: String, packageName: String): ConversionInfo {
        return Kotlin2JsonConverter(buildDelegate = MoshiBuildDelegate())
            .convert(
                input.byteInputStream(),
                System.out,
                ConversionArgs(rootClassName = rootClassName, packageName = packageName)
            )
    }
}

fun main(args: Array<String>) {
    JsonConverter().generate(input = "{\"id\": 3, \"name\": \"Le Chat\", \"data\": [{\"content\": \"sdfsdf\"}]}", rootClassName = "SearchResponse", packageName = "com.lt")
}