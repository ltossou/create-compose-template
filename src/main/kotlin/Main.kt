import util.Color
import util.println
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.div

private const val IS_DEBUG = false
private const val VERSION = "2023.07.28"

enum class Platform(val title: String) {
    Android("🤖 Android"),
    Desktop("🖥  Desktop"),
    Web("🌐 Web"),
    Wasm("🌐 Wasm"),
    ChromeExt("🔌 Chrome extension"),
    DesktopGame("🎮 Desktop (game)"),
    Terminal("⌨️  Terminal")
}

fun main(args: Array<String>) {
    println(Color.GREEN, "Initializing create-compose-app (v$VERSION)")
    val platform = Platform.Android
    // if (IS_DEBUG) {
    //     Platform.Android
    // } else {
    //     println(Color.YELLOW, "Choose platform")
    //     val platforms = Platform.entries.toTypedArray()
    //
    //     for ((index, p) in platforms.withIndex()) {
    //         println("${index + 1}) ${p.title}")
    //     }
    //     val selPlatformIndex = InputUtils.getInt(
    //         "Choose platform #",
    //         1,
    //         platforms.size
    //     )
    //
    //     platforms[selPlatformIndex - 1]
    // }

    println(Color.CYAN, "Platform: $platform")
    createAndroidApp()
}

fun createAndroidApp() {
    val roboto = Roboto(
        githubRepoUrl = "https://github.com/ltossou/ComposeAndroidTemplate",
        isDebug = IS_DEBUG,
        srcPackagePath = Path("com") / "ltossou" / "composeandroidtemplate"
    )

    roboto.start()

    val replaceMap = mapOf(
        "rootProject.name = \"compose-android-template\"" to "rootProject.name = \"${roboto.projectDirName}\"", // settings.build.gradle
        "com.ltossou.composeandroidtemplate" to roboto.packageName,
        "<string name=\"app_name\">compose-android-template</string>" to "<string name=\"app_name\">${roboto.projectName}</string>",
        "ComposeAndroidTemplate" to roboto.projectName.replace("[^\\w]+".toRegex(), "_"),
        "versionCode 20211003" to "versionCode ${genVersionCode()}"
    )

//    corvette.start(replaceMap, isAndroid = true)
}

fun genVersionCode(): String {
    return SimpleDateFormat("yyyyMMdd").format(Date())
}