package me.siv.toolshot.clipboard

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class TransferableImage(private val image: Image): Transferable {
    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor.equals(DataFlavor.imageFlavor)) {
            return image
        }
        throw UnsupportedFlavorException(flavor)
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        for (dataFlavor in transferDataFlavors) {
            if (flavor.equals(dataFlavor)) return true
        }
        return false
    }
}