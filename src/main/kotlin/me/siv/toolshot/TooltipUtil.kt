package me.siv.toolshot

import com.ibm.icu.text.SimpleDateFormat
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
import me.siv.toolshot.clipboard.ClipboardUtil
import me.siv.toolshot.clipboard.MacOsCompat
import me.siv.toolshot.config.Config
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.fog.FogRenderer
import net.minecraft.network.chat.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.Channels
import java.util.*
import java.util.function.Function
import javax.imageio.ImageIO
import kotlin.math.max

object TooltipUtil {
    var lastState: TooltipRenderState? = null

    val TOOLTIP_LAYER: Function<RenderTarget, RenderType> = Function { rt ->
        RenderType.create(
            "tooltip_screenshot",
            786432,
            false,
            false,
            RenderPipelines.TEXT,
            RenderType.CompositeState.builder().setTextureState(RenderStateShard.NO_TEXTURE)
                .setOutputState(RenderStateShard.OutputStateShard("tooltip_screenshot") { rt })
                .setLightmapState(RenderStateShard.LIGHTMAP).createCompositeState(false)
        )
    }

    fun copyTooltipToClipboard(tooltipRenderState: TooltipRenderState) {
        val (list, rl, font) = tooltipRenderState
        var fullWidth = 0
        var height = if (list.size == 1) -2 else 0

        list.forEach { component ->
            val componentWidth = component.getWidth(font)
            fullWidth = max(fullWidth, componentWidth)

            height += component.getHeight(font)
        }

        val device: GpuDevice = RenderSystem.getDevice()
        val encoder: CommandEncoder = device.createCommandEncoder()
        var renderTarget: RenderTarget
        try {
            renderTarget = TextureTarget(null, (fullWidth + 24) * Config.scale, (height + 24) * Config.scale, false)
        } catch (e: Exception) {
            Toolshot.error("Failed to create render target")
            e.printStackTrace()
            return
        }

        val minecraft = Toolshot.mc
        val globalUniform = GlobalSettingsUniform()

        minecraft.gameRenderer

        globalUniform.update(
            minecraft.window.width,
            minecraft.window.height,
            minecraft.options.glintStrength().get(),
            minecraft.level?.gameTime ?: 0L,
            minecraft.deltaTracker,
            minecraft.options.menuBackgroundBlurriness,
        )
        val consumer = OverrideVertexProvider(ByteBufferBuilder(256), renderTarget)

        val renderState = GuiRenderState()
        val context = GuiGraphics(minecraft, renderState)

        val renderer = GuiRenderer(
            renderState,
            consumer,
            //? if > 1.21.8 {
            SubmitNodeStorage(),
            minecraft.gameRenderer.featureRenderDispatcher,
            //?}
            emptyList()
        )

        encoder.clearColorTexture(renderTarget.colorTexture, 0)
        context.pose().scale(
            minecraft.window.guiScaledWidth / (fullWidth + 24).toFloat(),
            minecraft.window.guiScaledHeight / (height + 24).toFloat(),
        )

        TooltipRenderUtil.renderTooltipBackground(context, 0 + 12, 0 + 12, fullWidth, height, rl)
        var yOffset = 0

        list.forEach { component ->
            component.renderText(context, font, 0 + 12, yOffset + 12)
            yOffset += component.getHeight(font) + (if (component == list.first()) 2 else 0)
        }

        yOffset = 0

        list.forEach { component ->
            component.renderImage(font, 0 + 12, yOffset + 12, fullWidth, height, context)
            yOffset += component.getHeight(font) + (if (component == list.first()) 2 else 0)
        }
        context./*? if > 1.21.8 {*/renderDeferredElements()/*? } else {*//*renderDeferredTooltip()*//*?}*/
        (renderer as GuiRendererInterface).`toolShot$render`(minecraft.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE), renderTarget)

        consumer.finishDrawing()
        saveImageToClipboard(renderTarget, MacosUtil.IS_MACOS, Config.saveFile)

        globalUniform.close()
        renderer.close()
    }

    fun saveImageToClipboard(renderTarget: RenderTarget, macOs: Boolean, shouldSave: Boolean) {
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
                    if (shouldSave || macOs) {
                        val screenshotDir = File("screenshots/tooltip")
                        screenshotDir.mkdirs()

                        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                        val currentDate = sdf.format(Date())

                        val file = File(screenshotDir, "tooltip_$currentDate.png")
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

class OverrideVertexProvider(bufferAllocator: ByteBufferBuilder, rt: RenderTarget) : MultiBufferSource.BufferSource(
    bufferAllocator,
    Object2ObjectSortedMaps.emptyMap()
) {
    private val currentLayer: RenderType = TooltipUtil.TOOLTIP_LAYER.apply(rt)
    var bufferBuilder: BufferBuilder = BufferBuilder(this.sharedBuffer, currentLayer.mode(), currentLayer.format())

    override fun getBuffer(renderType: RenderType): VertexConsumer {
        return this.bufferBuilder
    }

    fun finishDrawing() {
        this.startedBuilders[this.currentLayer] = this.bufferBuilder
        this.endBatch(this.currentLayer)
    }
}