package common

import com.squareup.kotlinpoet.FileSpec
import di.PackageProvider
import model.Package
import util.DataProvider
import util.InputUtils
import util.getAbsolutePathFromProject
import util.remove
import java.io.File
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.name

abstract class Generator(protected val packageProvider: PackageProvider, protected val projectPath: Path) {

    private val DEFAULT_TEMPLATE_PROPERTIES = mapOf(
        TemplatePropertyKeys.APP_NAME to projectPath.name,
        TemplatePropertyKeys.PACKAGE_NAME to packageProvider.root
    )

    fun ifExport(relativePath: Path, filename: String = "", action: (relativePath: Path) -> Unit) {
        if (InputUtils.getBoolean(
                "✏ Export $filename to ${relativePath.getAbsolutePathFromProject()}?",
                default = true
            )
        ) {
            action(relativePath)
        }
    }

    fun askToExport(
        fileSpec: FileSpec, index: Int = 1,
        count: Int = 1
    ) = askToExport(
        relativePath = Path.of(
            fileSpec.packageName.replace("${packageProvider.root}.", "").replace(".", File.separator)
        ),
        fileSpec = fileSpec,
        index = index,
        count = count
    )

    fun askToExport(
        packageName: String,
        filename: String,
        fileContent: String,
        index: Int = 1,
        count: Int = 1
    ) =
        ifExport(
            Package(packageName).toRelativeDirectory(packageProvider),
            "$filename.kt"
        ) {
            writeFileToProject(filename, fileContent, it, index, count)
        }

    fun askToExport(
        relativePath: Path, fileSpec: FileSpec, index: Int = 1,
        count: Int = 1
    ) {
        ifExport(relativePath, fileSpec.name + ".kt") { writeFileToProject(fileSpec.name, fileSpec, it, index, count) }
    }

    fun writeFileToProject(
        fileName: String,
        fileContent: String,
        relativePath: Path,
        index: Int = 1,
        count: Int = 1
    ) {
        val filename = "$fileName.kt"
        val targetPath = relativePath.getAbsolutePathFromProject()
        println("🚚 Preparing source $filename @ $targetPath ($index/$count) ...")
        targetPath.createDirectories()
        File(targetPath.toFile(), filename).writeText(fileContent)
        println("✔ Finished [$targetPath${File.separator}$filename]")
    }

    fun writeFileToProject(
        className: String,
        fileSpec: FileSpec,
        relativePath: Path,
        index: Int = 1,
        count: Int = 1
    ) {
        val filename = "$className.kt"
        val targetPath = relativePath.getAbsolutePathFromProject()
        println("🚚 Preparing source $filename @ $targetPath ($index/$count) ...")
        targetPath.createDirectories()
        fileSpec.writeTo(projectPath.toAbsolutePath() / "app/src/main/java")
        println("✔ Finished [$targetPath${File.separator}$filename]")
    }

    fun createFromTemplateFile(
        templateFile: String,
        fileName: String,
        properties: Map<String, Any>,
        destPackage: String
    ) {
        createFromTemplate(DataProvider.getTemplate(templateFile), fileName, properties, destPackage)
    }

    fun createFromTemplate(template: String, fileName: String, properties: Map<String, Any>, destPackage: String) {
        val fileContent = evaluateConditions(
            (properties + DEFAULT_TEMPLATE_PROPERTIES).entries.fold(template) { acc, (key, value) ->
                acc.replace("%%${key}%%", value.toString())
            }, properties
        )
        println(fileContent)
        askToExport(destPackage, fileName, fileContent)
    }

    fun evaluateConditions(fileContent: String, properties: Map<String, Any>): String {
        var formatted = fileContent
        val p: Pattern = Pattern.compile("\\#if(.*?)\\#end", Pattern.DOTALL /* Multiline */)
        val m: Matcher = p.matcher(fileContent)
        while (m.find()) {
            val ifElseCondition = m.group(0)
            val condition = ifElseCondition.remove("#if").remove("#end")
                .substringBefore(")")
                .substringAfter("(")
            val property = condition.substringBefore("==").trim().replace("\${", "").remove("}")
            if (properties.containsKey(property)) {
                val value = condition.substringAfter("==").remove("\"").trim()
                if (properties[property] == value) {
                    formatted =
                        formatted.replace(ifElseCondition, ifElseCondition.substringAfter(")").substringBefore("#end").trim())
                    continue
                }
            }
            val singleLineCondition = ifElseCondition.remove("\r\n")
            formatted = formatted.replace(ifElseCondition, singleLineCondition).remove(singleLineCondition)
        }
        return formatted
    }

    private fun Path.getAbsolutePathFromProject() = getAbsolutePathFromProject(projectPath, packageProvider.root)
}