package me.siv.toolshot.mixins;

import me.siv.toolshot.tooltip.TooltipRenderState;
import me.siv.toolshot.tooltip.TooltipUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(
            method = "setTooltipForNextFrameInternal",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiGraphics;deferredTooltip:Ljava/lang/Runnable;", opcode = Opcodes.PUTFIELD)
    )
    private void onRenderTooltip(
            Font font,
            List<ClientTooltipComponent> list,
            int i,
            int j,
            ClientTooltipPositioner clientTooltipPositioner,
            ResourceLocation resourceLocation,
            boolean bl,
            CallbackInfo ci
    ) {
        var lastState = TooltipUtil.INSTANCE.getLastState();

        if (lastState == null || !lastState.equals(list, resourceLocation, font)) {
            TooltipUtil.INSTANCE.setLastState(new TooltipRenderState(list, resourceLocation, font));
        }
    }
}
