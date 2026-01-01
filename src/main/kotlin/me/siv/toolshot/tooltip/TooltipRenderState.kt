package me.siv.toolshot.tooltip

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.resources.ResourceLocation

data class TooltipRenderState(
    var lines: List<ClientTooltipComponent>,
    var background: ResourceLocation?,
    var font: Font,
) {
    fun equals(lines: List<ClientTooltipComponent>, background: ResourceLocation?, font: Font): Boolean {
        return this.lines == lines && this.background == background && this.font == font
    }
}
