package net.sixunderscore.mocha2d;

import net.sixunderscore.mocha2d.graphics.RenderContext;
import net.sixunderscore.mocha2d.graphics.Window;
import net.sixunderscore.mocha2d.util.RenderSettings;
import net.sixunderscore.mocha2d.util.WindowSettings;
import org.lwjgl.sdl.SDLInit;

public class Mocha2D {
    public static Window WINDOW;
    public static RenderContext RENDER_CONTEXT;

    public static void init(WindowSettings windowSettings) {
        int flags = SDLInit.SDL_INIT_VIDEO;
        if (!SDLInit.SDL_Init(flags)) {
            throw new IllegalStateException("Failed to initialize SDL");
        }

        WINDOW = new Window(windowSettings);
        RENDER_CONTEXT = new RenderContext();
    }

    public static void start(RenderSettings renderSettings) {
        RENDER_CONTEXT.init(renderSettings);
        WINDOW.initRendering(renderSettings);

        WINDOW.loop();

        cleanUp();
    }

    private static void cleanUp() {
        WINDOW.close();
        RENDER_CONTEXT.close();
    }
}
