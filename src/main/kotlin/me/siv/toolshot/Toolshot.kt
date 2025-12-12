package me.siv.toolshot

import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import me.siv.toolshot.config.Config
import me.siv.toolshot.mixins.KeyMappingAccessor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.resources.ResourceLocation
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val MODID = "toolshot"

object Toolshot : ClientModInitializer, Logger by LoggerFactory.getLogger(MODID) {
    val mc: Minecraft = Minecraft.getInstance()

    val configurator = Configurator("toolshot")
    var config: ResourcefulConfig? = null

    private val categoryResource = ResourceLocation.fromNamespaceAndPath(MODID, "main")
    val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(categoryResource)

    val COPY: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.toolshot.copy",
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
        )
    )

    override fun onInitializeClient() {
        config = Config.register(configurator)
        ScreenEvents.BEFORE_INIT.register { client, screen, _, _ ->
            ScreenKeyboardEvents.afterKeyPress(screen).register { _, event ->
                // TODO: Make chat available again but only copy when a tooltip is visible
                if (screen is ChatScreen) return@register
                if (event.key == (COPY as KeyMappingAccessor).key.value) {
                    if (TooltipUtil.canRender) {
                        TooltipUtil.copyTooltipToClipboard(TooltipUtil.currentFont!!, TooltipUtil.currentTooltipLines!!, TooltipUtil.currentResourceLocation)
                    }
                }
            }
        }
    }
}