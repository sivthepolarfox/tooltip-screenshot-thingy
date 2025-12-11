package me.siv.toolshot.clipboard

import ca.weblite.objc.Client
import ca.weblite.objc.Proxy
import com.mojang.blaze3d.platform.MacosUtil

object MacOsCompat {
    /**
     * Taken from https://github.com/DeDiamondPro/ChatShot/blob/1b72e99b646cc4c995ea40281cfbf369d3fb1616/src/main/java/dev/dediamondpro/chatshot/util/clipboard/MacOSCompat.java
     */
    fun doCopyMacOS(path: String?): Boolean {
        if (!MacosUtil.IS_MACOS) {
            return false
        }

        val client: Client = Client.getInstance()
        val url: Proxy? = client.sendProxy("NSURL", "fileURLWithPath:", path)

        val image: Proxy = client.sendProxy("NSImage", "alloc")
        image.send("initWithContentsOfURL:", url)

        var array: Proxy = client.sendProxy("NSArray", "array")
        array = array.sendProxy("arrayByAddingObject:", image)

        val pasteboard: Proxy = client.sendProxy("NSPasteboard", "generalPasteboard")
        pasteboard.send("clearContents")
        return pasteboard.sendBoolean("writeObjects:", array)
    }
}