package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.lang.Exception
import javax.imageio.ImageIO
import kotlin.math.pow

class IOExceptionReadFile(message:String): IOException(message)
class IOExceptionWriteFile(message:String): IOException(message)

class CryptoApp() {
    private var inputFile = File("")
    private var outputFile = File("")
    private var inputImage = BufferedImage(1,1,BufferedImage.TYPE_INT_RGB)
    private var outputImage = BufferedImage(1,1,BufferedImage.TYPE_INT_RGB)

    fun mainOperation() {
        while (true) {
            println("Task (hide, show, exit):")
            when(val action = readln()) {
                "hide" -> hide()
                "show" -> show()
                "exit" -> {
                    println("Bye!")
                    break
                }
                else -> println("Wrong task: $action")
            }
        }
    }

    private fun setLSBtoOne(image: BufferedImage): BufferedImage {

        for (i in 0 until image.width) {
            for (j in 0 until image.height) {
                val rgb = image.getRGB(i, j)

                image.setRGB(i, j, 0x010101 or rgb)
            }
        }
        return image
    }

    fun hideMessage(image: BufferedImage, message: ByteArray): BufferedImage {
        val masks = MutableList<Int>(8) { 0 }
        for (i in masks.indices) {
            masks[i] = 1 shl (masks.lastIndex - i)
        }

        var k = 0
        var indByte = 0
        for (j in 0 until image.height) {
            for (i in 0 until image.width) {
                val color = Color(image.getRGB(i, j))
                val prevBlue = color.blue

                val currentBit = (message[k].toInt() and masks[indByte]) shr (masks.lastIndex - indByte)

                val blue = ((color.blue shr 1) shl 1) or currentBit

                image.setRGB(i, j, Color(color.red, color.green, blue).rgb)
                if (k == message.lastIndex && indByte == 7) {
                    return image
                }
                indByte++

                if (indByte == 8) {
                    indByte = 0
                    k++
                }
            }
        }
        return image
    }



    fun hide() {
        try {
            println("Input image file: ")
            try {
                inputFile = File(readln())
                inputImage = ImageIO.read(inputFile)
            } catch (e: Exception) {
                throw IOExceptionReadFile("Can't read input file!")
            }
            println("Output image file: ")
            try {
                outputFile = File(readln())
                println("Message to hide: ")
                val message = readln()
                var messageByteArray = message.encodeToByteArray()
                val endString = byteArrayOf(0, 0, 3)
                messageByteArray = messageByteArray

                val maxInfo = inputImage.height * inputImage.width

                println("Password:")
                val password = readln().encodeToByteArray()
                val encMessage = encryptDecrypt(messageByteArray, password)

                if (encMessage.plus(endString).size * 8 > maxInfo) {
                    println("The input image is not large enough to hold this message.")
                    return
                }


                outputImage = hideMessage(inputImage, encMessage.plus(endString))
                val extension = outputFile.name.split(".").last()
                ImageIO.write(outputImage, extension, outputFile)
                println("Message saved in ${outputFile.invariantSeparatorsPath} image.")

            } catch (e: Exception) {
                throw IOExceptionWriteFile("Can't read output file!")
            }
        } catch (e: IOExceptionReadFile) {
            println(e.message)
            return
        } catch (e: IOExceptionWriteFile) {
            println(e.message)
            return
        }
    }

    fun encryptDecrypt(message: ByteArray, password: ByteArray): ByteArray {
        var encMessage = ByteArray(0)
        for (i in message.indices) {
            val encByte = (message[i].toInt() xor password[i % password.size].toInt()).toByte()
            encMessage = encMessage.plus( encByte )
        }
        return encMessage
    }

    fun extractMessage(image: BufferedImage): ByteArray {
        var messageByte = ByteArray(0)

        var k = 0
        var indByte = 0
        var currByte = 0
        for (j in 0 until image.height) {
            for (i in 0 until image.width) {
                val color = Color(image.getRGB(i, j))
                val bit = color.blue and 1

                currByte += (bit shl (7 - indByte))

                indByte++

                if (indByte == 8) {
                    messageByte = messageByte.plus(currByte.toByte())
                    currByte = 0
                    indByte = 0
                    k++
                    if (messageByte.size >= 3) {
                        val lastThree = messageByte.drop(messageByte.size - 3).toByteArray()
                        if (lastThree.contentEquals(byteArrayOf(0, 0, 3))) {
                            return messageByte.dropLast(3).toByteArray()
                        }
                    }
                }


            }
        }
        return ByteArray(0)
    }

    fun show() {
        println("Input image file: ")
        try {
            inputFile = File(readln())
            inputImage = ImageIO.read(inputFile)
            val encMessage = extractMessage(inputImage)
            println("Password:")
            val password = readln().encodeToByteArray()
            val message = encryptDecrypt(encMessage, password).toString(Charsets.UTF_8)
            println("Message:")
            println(message)
        } catch (e: Exception) {
            println("Can't read input file!")
        }

    }




}

fun main() {
    val app = CryptoApp()
    app.mainOperation()
}