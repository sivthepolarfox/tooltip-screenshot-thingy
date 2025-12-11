package me.siv.tooltipscreenshot

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderTarget

interface GuiRendererInterface {
    fun `tooltipScreenshot$render`(gpuBufferSlice: GpuBufferSlice, renderTarget: RenderTarget)
}