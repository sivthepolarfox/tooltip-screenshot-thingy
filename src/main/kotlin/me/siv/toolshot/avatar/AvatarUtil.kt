package me.siv.toolshot.avatar

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.CommandEncoder
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
import me.siv.toolshot.Toolshot
import me.siv.toolshot.Toolshot.mc
import me.siv.toolshot.clipboard.ClipboardUtil
import me.siv.toolshot.config.Config
import me.siv.toolshot.tooltip.GuiRendererInterface
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.RenderStateShard.LIGHTMAP
import net.minecraft.client.renderer.RenderStateShard.OVERLAY
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.fog.FogRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.function.Function

object AvatarUtil {

    val Component.stripped: String get() = this.string.replace(Regex("§."), "")

    val AVATAR_LAYER: Function<RenderTarget, RenderType> = Function { rt ->
        RenderType.create(
            "avatar",
            1536,
            RenderPipelines.ENTITY_DECAL,
            RenderType.CompositeState.builder().setTextureState(RenderStateShard.NO_TEXTURE)
                .setOutputState(RenderStateShard.OutputStateShard("avatar") { rt })
                .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false)
        )
    }

    fun meow() {
        val device: GpuDevice = RenderSystem.getDevice()
        val encoder: CommandEncoder = device.createCommandEncoder()
        var renderTarget: RenderTarget
        try {
            renderTarget = TextureTarget(null,  (75 - 26) * Config.scale,  70 * Config.scale, false)
        } catch (e: Exception) {
            Toolshot.error("Failed to create render target")
            e.printStackTrace()
            return
        }

        val globalUniform = GlobalSettingsUniform()

        globalUniform.update(
            mc.window.width,
            mc.window.height,
            mc.options.glintStrength().get(),
            mc.level?.gameTime ?: 0L,
            mc.deltaTracker,
            mc.options.menuBackgroundBlurriness,
        )

        val renderState = GuiRenderState()
        val context = GuiGraphics(mc, renderState)

        val consumer = OverrideVertexProvider(ByteBufferBuilder(256), renderTarget)

        val renderer = GuiRenderer(
            renderState,
            consumer,
            //? if > 1.21.8 {
            SubmitNodeStorage(),
            mc.gameRenderer.featureRenderDispatcher,
            //?}
            emptyList()
        )

        encoder.clearColorTexture(renderTarget.colorTexture, 0)

        val quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())

        val livingEntity = mc.player ?: return
        val vector3f = Vector3f(0.0f, livingEntity.bbHeight / 2.0f + 0.0625f * livingEntity.scale, 0.0f)

        getGuiEntityRenderState(
            context,
            0,
            0,
            75 - 26,
            70,
            30f / livingEntity.scale,
            vector3f,
            quaternionf,
            null,
            livingEntity,
        )

        (renderer as GuiRendererInterface).`toolShot$render`(mc.gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE), renderTarget)

        consumer.finishDrawing()
        ClipboardUtil.saveImageToClipboard(renderTarget, Config.saveFile, "avatars", "avatar_${livingEntity.name.stripped}")

        globalUniform.close()
        renderer.close()

        mc.execute {
            mc.gui.chat.addMessage(Component.literal("Meow"))
        }
    }

    fun getGuiEntityRenderState(
        guiGraphics: GuiGraphics,
        i: Int,
        j: Int,
        k: Int,
        l: Int,
        f: Float,
        vector3f: Vector3f,
        quaternionf: Quaternionf,
        quaternionf1: Quaternionf?,
        livingEntity: LivingEntity
    ) {
        val entityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
        val entityRenderer = entityRenderDispatcher.getRenderer(livingEntity)
        val entityRenderState: EntityRenderState = entityRenderer.createRenderState(livingEntity, 1.0f)
        entityRenderState.lightCoords = 15728880
        entityRenderState.hitboxesRenderState = null
        entityRenderState.shadowPieces.clear()
        entityRenderState.outlineColor = 0
        // val state =
        // return GuiEntityRenderState(
        //     entityRenderState,
        //     vector3f,
        //     quaternionf,
        //     quaternionf1,
        //     i,
        //     j,
        //     k,
        //     l,
        //     f,
        //     guiGraphics.scissorStack.peek()
        // )
        guiGraphics.submitEntityRenderState(entityRenderState, f, vector3f, quaternionf, quaternionf1, i, j, k, l)
    }
}

class OverrideVertexProvider(bufferAllocator: ByteBufferBuilder, rt: RenderTarget) : MultiBufferSource.BufferSource(
    bufferAllocator,
    Object2ObjectSortedMaps.emptyMap()
) {
    private val currentLayer: RenderType = AvatarUtil.AVATAR_LAYER.apply(rt)
    var bufferBuilder: BufferBuilder = BufferBuilder(this.sharedBuffer, currentLayer.mode(), currentLayer.format())

    override fun getBuffer(renderType: RenderType): VertexConsumer {
        return this.bufferBuilder
    }

    fun finishDrawing() {
        this.startedBuilders[this.currentLayer] = this.bufferBuilder
        this.endBatch(this.currentLayer)
    }
}