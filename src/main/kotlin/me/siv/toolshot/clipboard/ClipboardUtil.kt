package me.siv.toolshot.clipboard

import com.ibm.icu.text.SimpleDateFormat
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.MacosUtil
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import me.siv.toolshot.Toolshot
import net.minecraft.network.chat.Component
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.Channels
import java.util.Date
import javax.imageio.ImageIO

/**
 * Utility class taken from https://github.com/DeDiamondPro/ChatShot/blob/master/src/main/java/dev/dediamondpro/chatshot/util/clipboard/ClipboardUtil.java
 */
object ClipboardUtil {
    fun copy(image: BufferedImage): Boolean {
        return copy(TransferableImage(image))
    }

    fun copy(transferable: TransferableImage): Boolean {
        if (MacosUtil.IS_MACOS) return false
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(transferable, null)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun saveImageToClipboard(renderTarget: RenderTarget, shouldSave: Boolean, subFolder: String, imagePrefix: String) {
        val width = renderTarget.width
        val height = renderTarget.height

        val gpuTexture = renderTarget.colorTexture
        val gpuBuffer = RenderSystem.getDevice().createBuffer(
            null,
            GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_MAP_READ,
            renderTarget.width * renderTarget.height * (renderTarget.colorTexture?.format?.pixelSize() ?: 4)
        )
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, {
            try {
                val readView = encoder.mapBuffer(gpuBuffer, true, false)
                val image = NativeImage(width, height, false)

                for (k in 0..<height) {
                    for (l in 0..<width) {
                        val m = readView.data().getInt((l + k * width) * gpuTexture!!.format.pixelSize())
                        image.setPixelABGR(l, height - k - 1, m)
                    }
                }
                try {
                    var copied = false
                    if (shouldSave || MacosUtil.IS_MACOS) {
                        val screenshotDir = File("screenshots/$subFolder")
                        screenshotDir.mkdirs()

                        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                        val currentDate = sdf.format(Date())

                        val file = File(screenshotDir, "${imagePrefix}_$currentDate.png")
                        image.writeToFile(file)
                        if (MacosUtil.IS_MACOS) {
                            copied = MacOsCompat.doCopyMacOS(file.absolutePath)
                            if (!shouldSave) file.delete()
                        }
                    }
                    if (!MacosUtil.IS_MACOS) {
                        val outputStream = ByteArrayOutputStream()
                        val channel = Channels.newChannel(outputStream)
                        image.writeToChannel(channel)
                        channel.close()

                        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                        val bufferedImage = ImageIO.read(inputStream)
                        copied = copy(bufferedImage)
                    }
                    val message = if (copied) {
                        Component.translatable("toolshot.copy_success")
                    } else {
                        Component.translatable("toolshot.copy_failure")
                    }
                    Toolshot.mc.gui.chat.addMessage(Component.literal("§7[Toolshot] ").append(message))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } finally {
                gpuBuffer.close()
                renderTarget.destroyBuffers()
            }
        }, 0)
    }
}