package me.siv.toolshot.tooltip

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderTarget

interface GuiRendererInterface {
    fun `toolShot$render`(gpuBufferSlice: GpuBufferSlice, renderTarget: RenderTarget)
}