package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.resources.textures.TextureData;
import net.sixunderscore.mocha2d.util.*;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class Window {
    private static long window;
    private static int width;
    private static int height;
    private static float displayScale;
    private static RenderSystem renderSystem;
    private static Screen screen;
    private static OrthographicCamera camera;
    private static FpsHelper fpsHelper;
    private static DeltaTime deltaTime;

    public static void start(WindowSettings settings, Screen initialScreen) {
        init(settings, initialScreen);
        loop();
        cleanUp();
    }

    private static void init(WindowSettings settings, Screen initialScreen) {
        if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO)) {
            throw new IllegalStateException("Failed to initialize SDL");
        }

        long flags = SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY | SDLVideo.SDL_WINDOW_VULKAN;
        if (settings.isResizeable()) {
            flags |= SDLVideo.SDL_WINDOW_RESIZABLE;
        }

        window = SDLVideo.SDL_CreateWindow(settings.getWindowName(), settings.getInitialWidth(), settings.getInitialHeight(), flags);
        if (window == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create window");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuff = stack.mallocInt(1);
            IntBuffer heightBuff = stack.mallocInt(1);

            SDLVideo.SDL_GetWindowSizeInPixels(window, widthBuff, heightBuff);
            width = widthBuff.get(0);
            height = heightBuff.get(0);

            displayScale = SDLVideo.SDL_GetWindowDisplayScale(window);

            setWindowIcon(settings.getWindowIconPath());

            screen = initialScreen;
            camera = new OrthographicCamera();
            fpsHelper = new FpsHelper(settings.getFpsCap());
            deltaTime = new DeltaTime();
            renderSystem = new RenderSystem(stack, window, settings.getTextureFiles(), settings.getTtfFiles());
        }

        byte[] clearColor = settings.getClearColor();
        setClearColor(clearColor[0], clearColor[1], clearColor[2]);

        screen.init(renderSystem.getResourceManager());
    }

    private static void loop() {
        boolean running = true;
        SDL_Event event = SDL_Event.malloc();

        while (running) {
            deltaTime.update();
            fpsHelper.updateCount();

            if (!handleInput(event)) {
                running = false;
                continue;
            }

            renderSystem.render(screen, camera);

            fpsHelper.cap();
        }

        event.free();
    }

    private static boolean handleInput(SDL_Event event) {
        while (SDLEvents.SDL_PollEvent(event)) {
            switch (event.type()) {
                case SDLEvents.SDL_EVENT_QUIT -> {
                    return false;
                }
                case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                    width = event.window().data1();
                    height = event.window().data2();
                    camera.adjustProjection();
                    screen.onWindowResized();
                    renderSystem.onWindowResized();
                }
                case SDLEvents.SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> displayScale = SDLVideo.SDL_GetWindowDisplayScale(window);
                case SDLEvents.SDL_EVENT_TEXT_INPUT -> screen.onTextTyped(event.text().textString());
                case SDLEvents.SDL_EVENT_MOUSE_MOTION -> {
                    SDL_MouseMotionEvent motion = event.motion();
                    float x = motion.x() * displayScale;
                    float y = Window.getHeight() - (motion.y() * displayScale);
                    screen.onMouseMoved(x, y);
                }
                case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                    SDL_MouseButtonEvent button = event.button();
                    screen.onMouseClicked(button.button(), button.down());
                }
                case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
                    SDL_MouseWheelEvent wheel = event.wheel();
                    screen.onMouseScrolled(wheel.x(), wheel.y());
                }
                case SDLEvents.SDL_EVENT_KEY_UP, SDLEvents.SDL_EVENT_KEY_DOWN -> {
                    SDL_KeyboardEvent keyboard = event.key();
                    screen.onKeyPressed(keyboard.key(), keyboard.scancode(), keyboard.down(), keyboard.mod());
                }
            }
        }

        return true;
    }

    private static void setWindowIcon(String path) {
        if (!path.isEmpty()) {
            try (TextureData textureData = ResourceUtils.loadAndDecodeImage(path)) {
                SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(
                        textureData.width(),
                        textureData.height(),
                        SDLPixels.SDL_PIXELFORMAT_RGBA32,
                        textureData.data(),
                        textureData.width() * 4
                );

                if (surface != null) {
                    SDLVideo.SDL_SetWindowIcon(window, surface);
                    SDLSurface.SDL_DestroySurface(surface);
                }
            }
        }
    }

    public static void setScreen(Screen newScreen) {
        screen.cleanUp();
        screen = newScreen;
        screen.init(renderSystem.getResourceManager());
    }

    public static void setClearColor(byte r, byte g, byte b) {
        renderSystem.setClearColor(r, g, b);
    }

    public static void enableTextInput() {
        SDLKeyboard.SDL_StartTextInput(window);
    }

    public static void disableTextInput() {
        SDLKeyboard.SDL_StopTextInput(window);
    }

    public static void setFpsCap(int fpsCap) {
        fpsHelper.setFpsCap(fpsCap);
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static float getDeltaTime() {
        return deltaTime.get();
    }

    public static int getFpsCount() {
        return fpsHelper.getCount();
    }

    private static void cleanUp() {
        screen.cleanUp();
        camera.close();
        renderSystem.close();
        SDLVideo.SDL_DestroyWindow(window);
        SDLInit.SDL_Quit();
    }
}
