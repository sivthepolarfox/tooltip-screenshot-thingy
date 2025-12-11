package me.siv.tooltipscreenshot

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.MacosUtil
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import me.siv.tooltipscreenshot.clipboard.ClipboardUtil
import me.siv.tooltipscreenshot.clipboard.MacOsCompat
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.fog.FogRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.Channels
import java.util.function.Function
import javax.imageio.ImageIO
import kotlin.math.max

object TooltipUtil {
    var currentFont: Font? = null
    var currentTooltipLines: List<ClientTooltipComponent>? = null
    var currentResourceLocation: ResourceLocation? = null

    val canRender get() = currentFont != null && currentTooltipLines != null

    val TOOLTIP_LAYER: Function<RenderTarget, RenderType> = Function { rt ->
        RenderType.create(
            "tooltip_screenshot_text",
            786432,
            false,
            false,
            RenderPipelines.TEXT,
            RenderType.CompositeState.builder().setTextureState(RenderStateShard.NO_TEXTURE)
                .setOutputState(RenderStateShard.OutputStateShard("tooltip_screenshot_text") { rt })
                .setLightmapState(RenderStateShard.LIGHTMAP).createCompositeState(false)
        )
    }

    fun copyTooltipToClipboard(font: Font, list: List<ClientTooltipComponent>, rl: ResourceLocation?) {
        println("Called copyTooltipToClipboard: font=$font, list=$list, rl=$rl")
        var fullWidth = 0
        var height = if (list.size == 1) -2 else 0

        list.forEach { component ->
            val componentWidth = component.getWidth(font)
            fullWidth = max(fullWidth, componentWidth)

            height += component.getHeight(font)
        }
        println("Calculated dimensions: fullWidth=$fullWidth, height=$height")

        val device: GpuDevice = RenderSystem.getDevice()
        val encoder: CommandEncoder = device.createCommandEncoder()
        var renderTarget: RenderTarget
        try {
            renderTarget = TextureTarget(null, fullWidth, height, false)
        } catch (e: Exception) {
            println("Couldnt initialize render target")
            e.printStackTrace()
            return
        }

        val consoomer = OverrideVertexProvider(ByteBufferBuilder(256), renderTarget)

        val renderState = GuiRenderState()
        val context = GuiGraphics(TooltipScreenshot.mc, renderState)

        val renderer = GuiRenderer(renderState, consoomer, SubmitNodeStorage(), TooltipScreenshot.mc.gameRenderer.featureRenderDispatcher, emptyList())

        encoder.clearColorTexture(renderTarget.colorTexture, 0)
        context.pose().scale(
            TooltipScreenshot.mc.window.guiScaledWidth / fullWidth.toFloat(),
            TooltipScreenshot.mc.window.guiScaledHeight / height.toFloat(),
        )

        TooltipRenderUtil.renderTooltipBackground(context, 0, 0, fullWidth, height, rl)
        var yOffset = 0

        list.forEach { component ->
            component.renderText(context, font, 0, yOffset)
            yOffset += component.getHeight(font) + (if (component == list.first()) 2 else 0)
        }

        yOffset = 0

        list.forEach { component ->
            component.renderImage(font, 0, yOffset, fullWidth, height, context)
            yOffset += component.getHeight(font) + (if (component == list.first()) 2 else 0)
        }
        (renderer as GuiRendererInterface).`tooltipScreenshot$render`(TooltipScreenshot.mc.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE), renderTarget)
        println("Finished rendering tooltip")

        consoomer.finishDrawing()
        saveImageToClipboard(renderTarget, MacosUtil.IS_MACOS, true)
        println("Saved image to clipboard")

        renderer.close()
    }

    fun saveImageToClipboard(renderTarget: RenderTarget, macOs: Boolean, shouldSave: Boolean = true) {
        val width = renderTarget.width
        val height = renderTarget.height
        println("Trying to save image of size ${width}x${height}")

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
                    if (shouldSave || macOs) {
                        val screenshotDir = File("screenshots/chat")
                        screenshotDir.mkdirs()
                        val file = File(screenshotDir, "tooltip_${System.currentTimeMillis()}.png")
                        image.writeToFile(file)
                        if (macOs) {
                            copied = MacOsCompat.doCopyMacOS(file.absolutePath)
                            if (!shouldSave) file.delete()
                        }
                    }
                    if (!macOs) {
                        val outputStream = ByteArrayOutputStream()
                        val channel = Channels.newChannel(outputStream)
                        image.writeToChannel(channel)
                        channel.close()

                        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                        val bufferedImage = ImageIO.read(inputStream)
                        copied = ClipboardUtil.copy(bufferedImage)
                    }
                    if (copied) {
                        TooltipScreenshot.mc.gui.chat.addMessage(Component.literal("Mrrow"))
                    } else {
                        TooltipScreenshot.mc.gui.chat.addMessage(Component.literal("Blehh :("))
                    }
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

class OverrideVertexProvider(bufferAllocator: ByteBufferBuilder, rt: RenderTarget) :
    MultiBufferSource.BufferSource(
        bufferAllocator,
        Object2ObjectSortedMaps.emptyMap<RenderType?, ByteBufferBuilder?>()
    ) {
    private val currentLayer: RenderType = TooltipUtil.TOOLTIP_LAYER.apply(rt)
    var bufferBuilder: BufferBuilder = BufferBuilder(this.sharedBuffer, currentLayer.mode(), currentLayer.format())

    override fun getBuffer(renderType: RenderType?): VertexConsumer {
        return this.bufferBuilder
    }

    fun finishDrawing() {
        this.startedBuilders[this.currentLayer] = this.bufferBuilder
        this.endBatch(this.currentLayer)
    }
}