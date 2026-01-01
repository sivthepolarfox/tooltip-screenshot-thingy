package me.siv.toolshot

import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import me.siv.toolshot.config.Config
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
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
    //? if > 1.21.8 {
    val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(categoryResource)
    //? }

    val COPY: KeyMapping = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.toolshot.copy",
            GLFW.GLFW_KEY_UNKNOWN,
            /*? if > 1.21.8 {*/CATEGORY,/*?} else {*//*"key.category.toolshot.main"*//*?}*/
        )
    )

    override fun onInitializeClient() {
        config = Config.register(configurator)
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenKeyboardEvents.allowKeyPress(screen).register { /*? if > 1.21.8 {*/ _, event /*?} else {*//*screen, key, scancode, modifiers*//*?}*/ ->
                if (COPY.matches(/*? if > 1.21.8 {*/event/*?} else {*//*key, scancode*//*?}*/)) {
                    val state = TooltipUtil.lastState ?: return@register true
                    TooltipUtil.copyTooltipToClipboard(state)
                    return@register false
                }
                return@register true
            }
        }
    }
}