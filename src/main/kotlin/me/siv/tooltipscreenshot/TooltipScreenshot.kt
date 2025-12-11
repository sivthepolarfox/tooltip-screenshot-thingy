package me.siv.tooltipscreenshot

import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import me.siv.tooltipscreenshot.config.Config
import me.siv.tooltipscreenshot.mixins.KeyMappingAccessor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MODID = "tooltipscreenshot"

object TooltipScreenshot : ClientModInitializer, Logger by LoggerFactory.getLogger(MODID) {

    val configurator = Configurator("tooltipscreenshot")
    var config: ResourcefulConfig? = null

    private val categoryResource = ResourceLocation.fromNamespaceAndPath(MODID, "main")
    val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(categoryResource)

    val mc: Minecraft = Minecraft.getInstance()

    val COPY: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.tooltipscreenshot.copy",
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        )
    )

    override fun onInitializeClient() {
        config = Config.register(configurator)
        ScreenEvents.BEFORE_INIT.register { client, screen, _, _ ->
            ScreenKeyboardEvents.afterKeyPress(screen).register { _, event ->
                if (screen is ChatScreen) return@register
                client.gui.chat.addMessage(Component.literal("Clicked key: ${event.key} (COPY: 70)"))
                if (event.key == (COPY as KeyMappingAccessor).key.value) {
                    if (TooltipUtil.canRender) {
                        TooltipUtil.copyTooltipToClipboard(TooltipUtil.currentFont!!, TooltipUtil.currentTooltipLines!!, TooltipUtil.currentResourceLocation)
                    }
                }
            }
        }
    }
}