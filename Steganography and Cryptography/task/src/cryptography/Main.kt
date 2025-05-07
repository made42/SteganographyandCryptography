package cryptography

import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

import kotlin.experimental.xor

private const val CMD_HIDE = "hide"
private const val CMD_SHOW = "show"
private const val CMD_EXIT = "exit"

private const val MSG_INPUT_IMAGE_FILE = "Input image file"
private const val MSG_OUTPUT_IMAGE_FILE = "Output image file"
private const val MSG_MESSAGE_TO_HIDE = "Message to hide"
private const val MSG_PASSWORD = "Password"

private val suffixBytes = byteArrayOf(0, 0, 3)
private val suffix = suffixBytes.joinToString("") { Integer.toBinaryString(it.toInt()).padStart(Byte.SIZE_BITS, '0') }

fun main() {
    while (true) {
        println("Task ($CMD_HIDE, $CMD_SHOW, $CMD_EXIT):")
        when (val input = readln()) {
            CMD_HIDE -> {
                val filenameIn = promptUserWithMessage(MSG_INPUT_IMAGE_FILE)
                val filenameOut = promptUserWithMessage(MSG_OUTPUT_IMAGE_FILE)
                val message = promptUserWithMessage(MSG_MESSAGE_TO_HIDE)
                val password = promptUserWithMessage(MSG_PASSWORD)

                val image = try {
                    ImageIO.read(File(filenameIn))
                } catch (_: Exception) {
                    println("Can't read input file!")
                    continue
                }

                val encryptedBytesArray = message.encodeToByteArray().mapIndexed { index, byte ->
                    byte.xor(password.encodeToByteArray()[index % password.length])
                }.toByteArray() + suffixBytes

                if (encryptedBytesArray.size * Byte.SIZE_BITS > image.width * image.height) {
                    println("The input image is not large enough to hold this message.")
                    continue
                }

                for (y in 0..image.height) {
                    val rowIndex = y * image.width
                    for (x in 0..image.width) {
                        val pixelIndex = rowIndex + x
                        val byteIndex = pixelIndex / Byte.SIZE_BITS
                        val bitIndex = pixelIndex % Byte.SIZE_BITS

                        if (byteIndex < encryptedBytesArray.size) {
                            val bit = (encryptedBytesArray[byteIndex].toInt() shr ((Byte.SIZE_BITS - 1) - bitIndex)) and 1
                            val color = Color(image.getRGB(x, y)).let { Color(it.red, it.green, it.blue.and(254).or(bit)) }
                            image.setRGB(x, y, color.rgb)
                        }
                    }
                }

                try {
                    ImageIO.write(image, "png", File(filenameOut))
                } catch (_: Exception) {
                    println("Can't write output file!")
                    continue
                }

                println("Message saved in $filenameOut image.")
            }
            CMD_SHOW -> {
                val filename = promptUserWithMessage(MSG_INPUT_IMAGE_FILE)
                val password = promptUserWithMessage(MSG_PASSWORD)

                val image = try {
                    ImageIO.read(File(filename))
                } catch (_: Exception) {
                    println("Can't read input file!")
                    continue
                }

                var encryptedBits = ""
                loop@ for (y in 0..image.height) {
                    for (x in 0..image.width) {
                        encryptedBits += image.getRGB(x, y) and 1
                        if (encryptedBits.endsWith(suffix)) {
                            break@loop
                        }
                    }
                }

                val passwordBytes = password.encodeToByteArray()
                val bytes = encryptedBits.chunked(Byte.SIZE_BITS).map { it.toByte(2) }.mapIndexed { index, byte ->
                    byte xor passwordBytes[index % password.length]
                }.toByteArray()

                val message = String(bytes)
                println("Message:\n$message")
            }
            CMD_EXIT -> break
            else -> println("Wrong task: $input")
        }
    }
    println("Bye!")
}

private fun promptUserWithMessage(message: String): String {
    println("$message:")
    return readln().trim()
}
