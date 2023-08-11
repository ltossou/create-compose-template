package util

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.io.File
import java.net.URL
import javax.imageio.ImageIO


object CoverGenerator {
    private const val WIDTH = 1500
    private const val HEIGHT = 450
    private const val RANDOM_IMAGE_URL = "https://source.unsplash.com/random/${WIDTH}x${HEIGHT}?nature"
    private val transBlack by lazy { Color(0, 0, 0, 130) }


    private val coverFont by lazy {
        val fontRes = CoverGenerator::class.java.getResourceAsStream("/DoctorGlitch.otf")
        Font.createFont(Font.TRUETYPE_FONT, fontRes)
    }


    fun generate(projectName: String, fontSize: Int = 130, customFont: String? = null): File {
        // Download image
        val image = downloadImage()
        return draw(image, projectName, fontSize, customFont)
    }

    fun draw(
        inputFile: File,
        text: String,
        _fontSize: Int,
        customFont: String?,
        isFromTest: Boolean = false
    ): File {
        val fontSize = _fontSize.toFloat()
        val inputImage = ImageIO.read(inputFile)

        return inputImage.createGraphics().let { canvas ->
            canvas.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
            );
            canvas.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
            );

            // Trans bg
            canvas.paint = transBlack
            canvas.fillRect(0, 0, inputImage.width, inputImage.height)

            // Title font size
            val font = if (customFont == null) {
                // get default font
                coverFont.deriveFont(Font.PLAIN, fontSize)
            } else {
                Font.createFont(Font.TRUETYPE_FONT, File(customFont))
                    .deriveFont(Font.PLAIN, fontSize)
            }

            canvas.font = font
            val metrics = canvas.fontMetrics

            // Finding center
            val x: Int = (inputImage.width - metrics.stringWidth(text)) / 2
            val y: Int = (inputImage.height - metrics.height) / 2 + metrics.ascent

            canvas.paint = Color.WHITE
            canvas.drawString(text, x, y)

            canvas.dispose()

            // Saving cover
            val outputFile = File("cover.jpeg")
            ImageIO.write(inputImage, "jpeg", outputFile)

            if (isFromTest.not()) {
                inputFile.delete()
            }

            outputFile
        }
    }

    private fun downloadImage(): File {
        val image = ImageIO.read(URL(RANDOM_IMAGE_URL))
        val file = File("cover_background.jpeg")
        ImageIO.write(image, "jpeg", file)
        return file
    }

}

fun main() {
    CoverGenerator.generate("Compose Template")
}