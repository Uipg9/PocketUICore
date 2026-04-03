package com.pocketuicore.render;

import com.pocketcanvascore.render.PocketCanvas;

/**
 * Bridges PocketCanvas into PocketUICore's legacy RenderContext API.
 */
public final class RenderContexts {
    private RenderContexts() {}

    public static RenderContext from(PocketCanvas canvas) {
        return new PocketCanvasAdapter(canvas);
    }
}
