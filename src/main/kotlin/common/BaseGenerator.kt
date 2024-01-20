package common

import util.InputUtils
import com.squareup.kotlinpoet.FileSpec
import di.PackageProvider
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

abstract class BaseGenerator(protected val packageProvider: PackageProvider, protected val projectPath: Path) {

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
        relativePath = Path.of(fileSpec.packageName.replace("${packageProvider.root}.", "").replace(".", File.separator)),
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
        ifExport(Path.of(packageName.replace("${packageProvider.root}.", "").replace(".", File.separator)),
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

    private fun Path.getAbsolutePathFromProject() =
        (projectPath.toAbsolutePath() / "app/src/main/java" / packageProvider.root.replace(
            ".",
            File.separator
        )
                / this)
}