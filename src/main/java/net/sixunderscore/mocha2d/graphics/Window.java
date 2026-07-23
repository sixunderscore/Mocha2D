package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.resources.textures.TextureData;
import net.sixunderscore.mocha2d.graphics.util.OrthographicCamera;
import net.sixunderscore.mocha2d.util.*;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class Window implements AutoCloseable {
    private final long window;
    private int width;
    private int height;
    private float displayScale;
    private RenderSystem renderSystem;
    private Screen screen;
    private final OrthographicCamera camera;
    private FpsHelper fpsHelper;
    private final DeltaTime deltaTime;

    public Window(WindowSettings settings) {
        long flags = SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY | SDLVideo.SDL_WINDOW_VULKAN;
        if (settings.isResizeable()) {
            flags |= SDLVideo.SDL_WINDOW_RESIZABLE;
        }

        this.window = SDLVideo.SDL_CreateWindow(settings.getWindowName(), settings.getInitialWidth(), settings.getInitialHeight(), flags);
        if (this.window == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create window");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuff = stack.mallocInt(1);
            IntBuffer heightBuff = stack.mallocInt(1);

            SDLVideo.SDL_GetWindowSizeInPixels(this.window, widthBuff, heightBuff);
            this.width = widthBuff.get(0);
            this.height = heightBuff.get(0);
        }

        this.displayScale = SDLVideo.SDL_GetWindowDisplayScale(this.window);

        this.setIcon(settings.getWindowIconPath());

        this.screen = settings.getInitialScreen();
        this.camera = new OrthographicCamera(this.width, this.height);
        this.deltaTime = new DeltaTime();
    }

    public void initRendering(RenderSettings settings) {
        this.fpsHelper = new FpsHelper(settings.getFpsCap());
        this.renderSystem = new RenderSystem(this.window, settings.getTextureFiles(), settings.getTtfFiles());

        byte[] clearColor = settings.getClearColor();
        this.setClearColor(clearColor[0], clearColor[1], clearColor[2]);

        this.screen.init(this.renderSystem.getResourceManager());
    }

    public void loop() {
        boolean running = true;
        SDL_Event event = SDL_Event.malloc();

        while (running) {
            this.deltaTime.update();
            this.fpsHelper.updateCount();

            if (!handleInput(event)) {
                running = false;
                continue;
            }

            this.renderSystem.render(this.screen, this.camera);

            this.fpsHelper.cap();
        }

        event.free();
    }

    private boolean handleInput(SDL_Event event) {
        while (SDLEvents.SDL_PollEvent(event)) {
            switch (event.type()) {
                case SDLEvents.SDL_EVENT_QUIT -> {
                    return false;
                }
                case SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                    SDL_WindowEvent window = event.window();
                    this.width = window.data1();
                    this.height = window.data2();
                    this.camera.adjustProjection();
                    this.screen.onWindowResized();
                    this.renderSystem.onWindowResized();
                }
                case SDLEvents.SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> this.displayScale = SDLVideo.SDL_GetWindowDisplayScale(window);
                case SDLEvents.SDL_EVENT_TEXT_INPUT -> this.screen.onTextTyped(event.text().textString());
                case SDLEvents.SDL_EVENT_MOUSE_MOTION -> {
                    SDL_MouseMotionEvent motion = event.motion();
                    float x = motion.x() * this.displayScale;
                    float y = this.height - (motion.y() * this.displayScale);
                    this.screen.onMouseMoved(x, y);
                }
                case SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP, SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                    SDL_MouseButtonEvent button = event.button();
                    this.screen.onMouseClicked(button.button(), button.down());
                }
                case SDLEvents.SDL_EVENT_MOUSE_WHEEL -> {
                    SDL_MouseWheelEvent wheel = event.wheel();
                    this.screen.onMouseScrolled(wheel.x(), wheel.y());
                }
                case SDLEvents.SDL_EVENT_KEY_UP, SDLEvents.SDL_EVENT_KEY_DOWN -> {
                    SDL_KeyboardEvent keyboard = event.key();
                    this.screen.onKeyPressed(keyboard.key(), keyboard.scancode(), keyboard.down(), keyboard.mod());
                }
            }
        }

        return true;
    }

    public void setIcon(String path) {
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
                    SDLVideo.SDL_SetWindowIcon(this.window, surface);
                    SDLSurface.SDL_DestroySurface(surface);
                }
            }
        }
    }

    public void setScreen(Screen newScreen) {
        this.screen.cleanUp();
        this.screen = newScreen;
        this.screen.init(this.renderSystem.getResourceManager());
    }

    public void setClearColor(byte r, byte g, byte b) {
        this.renderSystem.setClearColor(r, g, b);
    }

    public void enableTextInput() {
        SDLKeyboard.SDL_StartTextInput(this.window);
    }

    public void disableTextInput() {
        SDLKeyboard.SDL_StopTextInput(this.window);
    }

    public void setFpsCap(int fpsCap) {
        this.fpsHelper.setFpsCap(fpsCap);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public float getDeltaTime() {
        return this.deltaTime.get();
    }

    public int getFpsCount() {
        return this.fpsHelper.getCount();
    }

    @Override
    public void close() {
        this.screen.cleanUp();
        this.camera.close();
        this.renderSystem.close();
        SDLVideo.SDL_DestroyWindow(this.window);
        SDLInit.SDL_Quit();
    }
}
