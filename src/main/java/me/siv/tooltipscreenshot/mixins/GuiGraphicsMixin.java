package me.siv.tooltipscreenshot.mixins;

import me.siv.tooltipscreenshot.TooltipUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
    @Inject(
            method = "renderTooltip",
            at = @At("HEAD")
    )
    private void onRenderTooltip(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner clientTooltipPositioner, ResourceLocation resourceLocation, CallbackInfo ci) {
        if (TooltipUtil.INSTANCE.getCurrentFont() != font) {
            TooltipUtil.INSTANCE.setCurrentFont(font);
        }
        if (TooltipUtil.INSTANCE.getCurrentTooltipLines() != list) {
            TooltipUtil.INSTANCE.setCurrentTooltipLines(list);
        }
        if (TooltipUtil.INSTANCE.getCurrentResourceLocation() != resourceLocation) {
            TooltipUtil.INSTANCE.setCurrentResourceLocation(resourceLocation);
        }
    }
}
