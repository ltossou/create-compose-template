package mainapp

import common.Generator
import common.TemplatePropertyKeys
import di.PackageProvider
import model.AndroidLibraries
import util.InputUtils
import util.replace
import util.replaceLine
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * Generates:
 *  * MainApplication.kt
 */
class ApplicationClassGenerator(projectPath: Path, packageProvider: PackageProvider) :
    Generator(packageProvider, projectPath) {

    sealed class Result {
        data class Success(val warnings: List<String>) : Result()
        data object Skip : Result()
    }

    fun start(libraries: List<AndroidLibraries>): Result {
        println("\n👑 Configure Application class...")
        val file = projectPath.toAbsolutePath() / "app/src/main/java" / packageProvider.root.replace(
            ".",
            File.separator
        ) / "MainApplication.kt"
        if (!InputUtils.getBoolean(
                "${if (file.exists()) "Overwrite" else "Create"} MainApplication.kt?",
                default = false
            )
        ) {
            return Result.Skip
        }

        generate(libraries)
        val warnings = addToManifest()
        return Result.Success(warnings = warnings)
    }

    fun generate(libraries: List<AndroidLibraries>): List<String> {
        println("💻 Generating MainApplication.kt...")
        createFromTemplateFile(
            "MainApplication",
            "MainApplication",
            mapOf(
                TemplatePropertyKeys.DEPENDENCY_INJECTION to if (libraries.contains(AndroidLibraries.HILT)) AndroidLibraries.HILT.name else "",
                TemplatePropertyKeys.USE_LIBRARY_TIMBER to libraries.contains(AndroidLibraries.TIMBER).toString(),
                "USE_LIBRARY_" + AndroidLibraries.FLIPPER to libraries.contains(AndroidLibraries.FLIPPER).toString(),
                "USE_LIBRARY_" + AndroidLibraries.WORKMANAGER to (libraries.contains(AndroidLibraries.WORKMANAGER) && libraries.contains(
                    AndroidLibraries.HILT
                )).toString()
            ),
            packageProvider.root
        )

        return listOf()
    }

    /**
     * Adds the Application class to AndroidManifest
     */
    private fun addToManifest(): List<String> {
        println("💻 Adding MainApplication.kt to AndroidManifest...")

        val manifest = File((projectPath.toAbsolutePath() / "app/src/main").toFile(), "AndroidManifest.xml")
        if (!manifest.exists()) {
            return listOf("TODO: ${manifest.absolutePath} not fount. Add android:name=\".MainApplication\" in AndroidManifest.xml")
        }
        val lines = manifest.readLines()
        var inApplicationTag = false
        var applicationNameLine: String? = null
        for (line in lines) {
            if (line.contains("<application")) {
                inApplicationTag = true
            } else if (line.contains("<activity")) {
                inApplicationTag = false
                break
            } else if (inApplicationTag) {
                if (line.contains("android:name")) {
                    applicationNameLine = line
                }
            }
        }
        if (applicationNameLine != null) {
            manifest.replace(applicationNameLine, "android:name=\".MainApplication\"")
        } else {
            manifest.replaceLine(
                "<application\n" +
                        "        android:name=\".MainApplication\""
            ) { it.contains("<application") }
        }
        return emptyList()
    }

}

fun main(args: Array<String>) {
    ApplicationClassGenerator(
        packageProvider = PackageProvider("com.ltossou.app"),
        projectPath = Path.of("TestProject")
    )
        .start(listOf(AndroidLibraries.HILT))
}