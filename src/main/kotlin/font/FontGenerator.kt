package font

import com.squareup.kotlinpoet.FileSpec
import common.Generator
import di.PackageProvider
import model.Font
import util.println
import java.nio.file.Path

/**
 * Generates:
 *  * Type.kt
 */
class FontGenerator(projectPath: Path, packageProvider: PackageProvider) : Generator(packageProvider, projectPath) {

    sealed class Result {
        data class Success(val font: Font, val warnings: List<String>) : Result()
        data object Skip : Result()
    }

    fun start(): Result {
        val font = Font.fromInput()
        if (font.name == "default") {
            return Result.Skip
        }
        val warnings = generate(font)
        return Result.Success(font = font, warnings = warnings)
    }

    fun generate(font: Font): List<String> {
        println("\n🚚 Preparing ${font.name} font ...")
        println("💻 Generating Type.kt...")
        createFromTemplateFile("Type",
            "Type",
            mapOf("FONT_NAME" to font.name),
            packageProvider.fontTypography()
        )

        return listOf(
            "TODO: Add     MaterialTheme(\n" +
                    "        colorScheme = colorScheme,\n" +
                    "        typography = ${font.name}FontFamily.typography(),\n" +
                    "        content = content\n" +
                    "    ) to Theme.kt"
        )
    }

    private fun generateTypography(font: Font): FileSpec {
        val className = "Type"
        println("💻 Generating $className...")

        return FileSpec.builder(packageProvider.fontTypography(), className)
            .addCode(FontTemplate.TEMPLATE)
            .build()
            .println()
    }

}

fun main(args: Array<String>) {
    FontGenerator(
        packageProvider = PackageProvider("com.ltossou.app"),
        projectPath = Path.of("TestProject")
    )
        .start()
}