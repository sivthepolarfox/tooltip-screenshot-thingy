package me.siv.toolshot.mixins;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/*
 * Taken from https://github.com/comp500/ScreenshotToClipboard under MIT license
 */
@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void awtHack(String[] strings, CallbackInfo ci) {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            System.setProperty("java.awt.headless", "false");
        }
    }
}
