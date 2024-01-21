import di.DependencyProvider
import font.FontGenerator
import mainapp.ApplicationClassGenerator
import model.AndroidLibraries
import util.Color
import util.InputUtils
import util.unzip
import model.Api
import model.Font
import model.Project
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.*

class Roboto(
    githubRepoUrl: String, // Eg :https://github.com/ltossou/compose-desktop-template
    private val srcPackagePath: Path = Path("com") / "myapp",
    private val modules: Array<String> = arrayOf(MAIN_MODULE),
    private val srcDirs: Array<String> = arrayOf("main", "test", "androidTest"),
    branch: String = "master",
    private val isDebug: Boolean = false,
    debugProjectName: String = "Super Project",
    debugPackageName: String = "com.ltossou.superproject",
) {

    companion object {
        const val MAIN_MODULE = "src"
        private val REPLACEABLE_FILE_EXT = arrayOf("kt", "kts", "html", "json", "xml", "gradle")

        fun pullFromGithub(
            githubRepoUrl: String,
            projectName: String,
            packageName: String,
            replaceMap: Map<String, String>,
            isFolder: Boolean = false,
            branch: String = "master",
            isDebug: Boolean = false,
            srcPackagePath: Path = Path("com") / "myapp",
            modules: Array<String> = arrayOf(MAIN_MODULE),
            srcDirs: Array<String> = arrayOf("main", "test", "androidTest")
        ) {
            println("💻 Initializing...")

            val currentDir = if (isDebug) {
                "tmp"
            } else {
                System.getProperty("user.dir")
            }

            // Get source code
            println("⬇️  Downloading template $githubRepoUrl...")
            val fileName = githubRepoUrl.split("/").last()
            val extension = if (isFolder) ".zip" else ""
            val outputFile = Path(currentDir) / "$fileName$extension"
            if (outputFile.notExists()) {
                if (outputFile.parent.notExists()) {
                    outputFile.parent.createDirectories()
                }
                val os = FileOutputStream(outputFile.toFile())
                val templateUrl = "$githubRepoUrl/archive/refs/heads/$branch.zip"
                URL(if (isFolder) templateUrl else githubRepoUrl).openStream().copyTo(os)
                os.close()
            }

            // Unzip
            val extractDir = outputFile.parent
            if (isFolder) {
                println("📦 Unzipping...")
                outputFile.unzip(extractDir)
            }

            // Rename dir
            val targetProjectDir: Path
            if (isFolder) {
                val projectDirName = projectName.lowercase().replace(" ", "-")
                val extractedProjectDir = extractDir / "$fileName-master"
                targetProjectDir = extractDir / projectDirName
                targetProjectDir.toFile().deleteRecursively()
                println("📦 Moving to $targetProjectDir...")
                extractedProjectDir.moveTo(targetProjectDir, overwrite = true)
            } else {
                targetProjectDir = outputFile.parent / projectName
                outputFile.moveTo(targetProjectDir)
            }

            // Move source
            println("🚚 Preparing source and test files (1/2) ...")
            if (isFolder) {
                for (module in modules) {
                    for (type in srcDirs) {
                        val baseSrc = Path(module) / type / "java" // src/main/java
                        val myAppSrcPath =
                            targetProjectDir / "app" / baseSrc / srcPackagePath // current/MyProject/app/src/main/java/com/myapp
                        if (myAppSrcPath.exists()) {
                            val targetSrcPath =
                                targetProjectDir / "app" / baseSrc / packageName.replace(
                                    ".",
                                    File.separator
                                ) // current/MyProject/app/src/main/java/package
                            targetSrcPath.createDirectories()
                            myAppSrcPath.moveTo(targetSrcPath, overwrite = true)
                            val srcParent = myAppSrcPath.parent.toFile()
                            if (srcParent.listFiles()?.isEmpty() == true) {
                                srcParent.deleteRecursively()
                            }
                        }
                    }
                }
            } else {
                val baseSrc = Path("src") / "main" / "java" // src/main/java
                val myAppSrcPath =
                    targetProjectDir / "app" / baseSrc / srcPackagePath // current/MyProject/app/src/main/java/com/myapp
                if (myAppSrcPath.exists()) {
                    val targetSrcPath =
                        targetProjectDir / "app" / baseSrc / packageName.replace(
                            ".",
                            File.separator
                        ) // current/MyProject/app/src/main/java/package
                    targetSrcPath.createDirectories()
                    myAppSrcPath.moveTo(targetSrcPath, overwrite = true)
                }

                println("🚚 Verifying file contents (2/2) ...")

                if (isFolder) {
                    targetProjectDir.toFile().walk().forEach { file ->
                        if (REPLACEABLE_FILE_EXT.contains(file.extension)) {
                            var newContent = file.readText()
                                .replace("com.lt", packageName)
                            for ((key, value) in replaceMap) {
                                newContent = newContent
                                    .replace(
                                        key, value
                                    )
                            }
                            file.writeText(newContent)
                        }
                    }
                } else {
                    targetProjectDir.toFile().apply {
                        var newContent = readText()
                            .replace("com.lt", packageName)
                        for ((key, value) in replaceMap) {
                            newContent = newContent
                                .replace(
                                    key, value
                                )
                        }
                        writeText(newContent)
                    }
                }
                // Give execute permission to ./gradlew
                val gradlewFile = targetProjectDir / "gradlew"
                if (gradlewFile.exists()) {
                    gradlewFile.toFile().setExecutable(true, false)
                }

                // Acknowledge
                if (!isDebug) {
                    println("♻️  Removing temp files...")
                    outputFile.deleteIfExists()
                }

                println("✔️  Finished. [Project Dir: '$targetProjectDir']")
            }
        }
    }

    private var projectPath: Path
    private val templateUrl = "$githubRepoUrl/archive/refs/heads/$branch.zip"
    private val repoName: String
    private val extractedDirName: String
    val projectName: String
    val packageName: String
    val projectDirName: String
    private lateinit var dependencyProvider: DependencyProvider

    init {
        repoName = githubRepoUrl.split("/").last()
        extractedDirName = "$repoName-master"

        val outputDir = getOutputDirInput()
        projectPath = getProjectNameInput(outputDir)
        util.println(Color.CYAN, "Project path: $projectPath")
        projectName = projectPath.toFile().name

        packageName = if (projectPath.notExists()) {
            // Ask package name
            getPackageNameInput(debugPackageName)
        } else {
            (findProjectPackageName(projectPath) ?: debugPackageName).also {
                println("Package name: $it")
            }
        }

        // Dir name will be always in kebab case
        projectDirName = projectName.lowercase().replace(" ", "-")

        dependencyProvider = DependencyProvider(packageName, projectPath)
    }

    private fun findProjectPackageName(projectPath: Path): String? {
        return (projectPath / "app").toFile().walk()
            .find { file -> file.name.startsWith("build.gradle") }
            ?.let { file ->
                file.readLines().find { line -> line.trim().startsWith("namespace") }
                    ?.substringAfterLast(" ")
                    ?.replace("\"", "")
                    ?.trim()
            }
    }

    private fun getPackageNameInput(debugPackageName: String) = if (isDebug) {
        debugPackageName
    } else {
        InputUtils.promptString("Enter package name", true)
    }

    private fun getProjectNameInput(parentFolder: Path): Path {
        if (isDebug) {
            return parentFolder / "DebugProject"
        }
        val defaultAndroidDir = Path(System.getProperty("user.home")) / "AndroidStudioProjects"
        if (parentFolder == defaultAndroidDir) {
            println("0. New project")
            defaultAndroidDir.toFile().listFiles()?.let { androidProjects ->
                androidProjects.forEachIndexed { index, file -> println("${index + 1}. ${file.name}") }
                // defaultAndroidDir.toFile().walk().forEachIndexed { index, file ->
                //     print("${index + 1}. ${file.name}")
                // }
                val projectSelection = InputUtils.getInt(
                    "Choose project #",
                    0,
                    defaultAndroidDir.toFile().length().toInt()
                )
                if (projectSelection != 0) {
                    return parentFolder / androidProjects[projectSelection - 1].name
                }
            }
        }
        return parentFolder / InputUtils.promptString("Enter project name", true)
    }

    private fun getOutputDirInput(): Path {
        println("1. ${System.getProperty("user.home")}/AndroidStudioProjects")
        println("2. ${System.getProperty("user.dir")}")
        println("3. Other")
        val selDirIndex = InputUtils.getInt(
            "📁 Choose output directory #",
            1,
            3
        )
        return when (selDirIndex) {
            1 -> Path(System.getProperty("user.home")) / "AndroidStudioProjects"
            2 -> Path(System.getProperty("user.dir"))
            3 -> Path(InputUtils.promptString("Enter absolute path", true))
            else -> throw IllegalArgumentException("Unknown option $selDirIndex")
        }
    }


    fun createApi(projectPath: Path) {
        Api.input().generate(projectPath, packageName)
    }

    fun start() {
        println("💻 Initializing...")

        val projectBuilder = Project.Builder(projectName)
            .setPath(projectPath)
            .setPackageName(packageName)

        // Android libs
        projectBuilder.setLibraries(selectLibraries())

        // Font
        val fontResult = generateFont()
        projectBuilder.setFont(if (fontResult is FontGenerator.Result.Success) fontResult.font else Font.DEFAULT)

        // MainApp
        generateApplicationClass(projectBuilder.libraries)

        createApi(projectPath)
    }

    private fun selectLibraries(): List<AndroidLibraries> {
        val libraries = AndroidLibraries.values()
        libraries.forEachIndexed { index, lib -> println("${index + 1}. ${lib.name}") }
        val options = InputUtils.promptString("Choose the libraries you're using (separated by space)", true)
            .split(" ")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 1 && it <= libraries.size + 1 }
        return options.map { libraries[it - 1] }
    }

    private fun generateFont(): FontGenerator.Result {
        val fontResult = dependencyProvider.fontGenerator.start()
        if (fontResult is FontGenerator.Result.Success && fontResult.warnings.isNotEmpty()) {
            util.println(Color.YELLOW, "Warnings:\n\t *${fontResult.warnings.joinToString(separator = "\n\t*")}")
        }
        return fontResult
    }

    private fun generateApplicationClass(libraries: List<AndroidLibraries>): ApplicationClassGenerator.Result {
        val appResult = dependencyProvider.appClassGenerator.start(libraries)
        if (appResult is ApplicationClassGenerator.Result.Success && appResult.warnings.isNotEmpty()) {
            util.println(Color.YELLOW, "Warnings:\n\t *${appResult.warnings.joinToString(separator = "\n\t*")}")
        }
        return appResult
    }

    fun start(
        replaceMap: Map<String, String>,
        isAndroid: Boolean = false
    ) {
        println("💻 Initializing...")

        val currentDir = if (isDebug) {
            "tmp"
        } else {
            System.getProperty("user.dir")
        }

        // Get source code
        println("⬇️  Downloading template...")
        val outputFile = Path(currentDir) / "${repoName}.zip"
        if (outputFile.notExists()) {
            if (outputFile.parent.notExists()) {
                outputFile.parent.createDirectories()
            }
            val os = FileOutputStream(outputFile.toFile())
            URL(templateUrl).openStream().copyTo(os)
            os.close()
        }

        // Unzip
        val extractDir = outputFile.parent
        println("📦 Unzipping...")
        outputFile.unzip(extractDir)

        // Rename dir
        val extractedProjectDir = extractDir / extractedDirName
        val targetProjectDir = extractDir / projectDirName
        targetProjectDir.toFile().deleteRecursively()
        extractedProjectDir.moveTo(targetProjectDir, overwrite = true)

        // Move source
        println("🚚 Preparing source and test files (1/2) ...")
        for (module in modules) {
            for (type in srcDirs) {
                val baseSrc = when {
                    isAndroid -> {
                        Path(module) / type / "java"
                    }

                    module == MAIN_MODULE -> {
                        // main module
                        Path(module) / type / "kotlin"
                    }

                    else -> {
                        Path(module) / "src" / type / "kotlin"
                    }
                }
                val myAppSrcPath = if (isAndroid) {
                    targetProjectDir / "app" / baseSrc / srcPackagePath
                } else {
                    targetProjectDir / baseSrc / srcPackagePath
                }
                if (myAppSrcPath.exists()) {
                    val targetSrcPath = if (isAndroid) {
                        targetProjectDir / "app" / baseSrc / packageName.replace(".", File.separator)
                    } else {
                        targetProjectDir / baseSrc / packageName.replace(".", File.separator)
                    }
                    targetSrcPath.createDirectories()
                    myAppSrcPath.moveTo(targetSrcPath, overwrite = true)
                    val srcParent = myAppSrcPath.parent.toFile()
                    if (srcParent.listFiles()?.isEmpty() == true) {
                        srcParent.deleteRecursively()
                    }
                }

                // Moving benchmark directory for android
                if (isAndroid) {
                    val myBenchmarkSrcPath = targetProjectDir / "benchmark" / baseSrc / srcPackagePath
                    if (myBenchmarkSrcPath.exists()) {
                        val targetSrcPath =
                            targetProjectDir / "benchmark" / baseSrc / packageName.replace(".", File.separator)
                        targetSrcPath.createDirectories()
                        myBenchmarkSrcPath.moveTo(targetSrcPath, overwrite = true)
                        val srcParent = myBenchmarkSrcPath.parent.toFile()
                        if (srcParent.listFiles()?.isEmpty() == true) {
                            srcParent.deleteRecursively()
                        }
                    }
                }
            }
        }

        println("🚚 Verifying file contents (2/2) ...")

        targetProjectDir.toFile().walk().forEach { file ->
            if (REPLACEABLE_FILE_EXT.contains(file.extension)) {
                var newContent = file.readText()
                for ((key, value) in replaceMap) {
                    newContent = newContent.replace(
                        key, value
                    )
                }
                file.writeText(newContent)
            }
        }

        // Give execute permission to ./gradlew
        val gradlewFile = targetProjectDir / "gradlew"
        gradlewFile.toFile().setExecutable(true, false)

        // Acknowledge
        if (!isDebug) {
            println("♻️  Removing temp files...")
            outputFile.deleteIfExists()
        }

        println("✔️  Finished. [Project Dir: '$targetProjectDir']")
    }

}