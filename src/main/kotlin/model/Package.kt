package model

import di.PackageProvider
import util.getAbsolutePathFromProject
import util.remove
import java.io.File
import java.nio.file.Path

data class Package(val name: String) {
    fun toRelativeDirectory(packageProvider: PackageProvider) = Path.of(
        name.remove("${packageProvider.root}.")
            .remove(packageProvider.root)
            .replace(".", File.separator)
    )

    fun toProjectDirectory(packageProvider: PackageProvider, projectPath: Path) =
        toRelativeDirectory(packageProvider).getAbsolutePathFromProject(projectPath, packageProvider)
}
