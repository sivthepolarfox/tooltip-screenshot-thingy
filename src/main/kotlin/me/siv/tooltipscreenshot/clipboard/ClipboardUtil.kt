package me.siv.tooltipscreenshot.clipboard

import com.mojang.blaze3d.platform.MacosUtil
import java.awt.Toolkit
import java.awt.image.BufferedImage

object ClipboardUtil {
    fun copy(image: BufferedImage): Boolean {
        return copy(TransferableImage(image))
    }

    fun copy(transferable: TransferableImage): Boolean {
        if (MacosUtil.IS_MACOS) return false
        try {
            val headless = System.getProperty("java.awt.headless")
            System.setProperty("java.awt.headless", "false")
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(transferable, null)
            System.setProperty("java.awt.headless", headless)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}