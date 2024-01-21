package util

import di.PackageProvider
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div

fun Path.getAbsolutePathFromProject(projectPath: Path, packageProvider: PackageProvider) =
    getAbsolutePathFromProject(projectPath, packageProvider.root)

fun Path.getAbsolutePathFromProject(projectPath: Path, rootPackageName: String) =
    (projectPath.toAbsolutePath() / "app/src/main/java" / rootPackageName.replace(".", File.separator) / this)