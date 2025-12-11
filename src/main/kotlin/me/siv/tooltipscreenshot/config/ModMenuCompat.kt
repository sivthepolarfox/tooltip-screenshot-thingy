package me.siv.tooltipscreenshot.config

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import me.siv.tooltipscreenshot.TooltipScreenshot
import net.minecraft.client.gui.screens.Screen

class ModMenuCompat : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory<Screen> { parent: Screen? ->
            try {
                ResourcefulConfigScreen.make(TooltipScreenshot.config).build()
            } catch (e: Exception) {
                println("Cannot open config before it is initialized")
                e.printStackTrace()
                parent
            }
        }
    }
}