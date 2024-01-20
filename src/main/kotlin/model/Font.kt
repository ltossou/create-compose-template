package model

import com.squareup.kotlinpoet.FileSpec
import util.InputUtils

data class Font(
    val name: String = "default"
) {

    companion object : DataInput<Font> {
        override fun fromInput(modelsSpecs: List<FileSpec>): Font {
            println("\n🗃 Configure font...")
            val name = InputUtils.promptString(
                "Enter Google Font name (e.g Inter. Press Enter to skip and keep default font)",
                false,
                default = "default"
            )
            return Font(name = name)
        }
    }
}