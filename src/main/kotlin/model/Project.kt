package model

import java.nio.file.Path

data class Project(
    val name: String,
    val path: Path,
    val packageName: String,
    val font: Font,
    val libraries: List<AndroidLibraries>,
    val apis: List<Api>,
) {
    class Builder(val name: String) {
        var path: Path = Path.of(".")
        var packageName: String = ""
        var font: Font = Font.DEFAULT
        var libraries: List<AndroidLibraries> = emptyList()
        var apis: List<Api> = emptyList()

        fun setPath(path: Path): Builder {
            this.path = path
            return this
        }

        fun setPackageName(packageName: String): Builder {
            this.packageName = packageName
            return this
        }

        fun setFont(font: Font): Builder {
            this.font = font
            return this
        }

        fun setLibraries(libraries: List<AndroidLibraries>): Builder {
            this.libraries = libraries
            return this
        }

        fun build(): Project = Project(
            name = name,
            path = path,
            packageName = packageName,
            font = font,
            libraries = libraries,
            apis = apis
        )
    }
}
