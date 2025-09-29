package net.sixunderscore.mocha2d;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.textures.TextureData;
import net.sixunderscore.mocha2d.graphics.textures.TextureManager;
import net.sixunderscore.mocha2d.util.*;
import net.sixunderscore.mocha2d.util.FpsHelper;
import net.sixunderscore.mocha2d.util.Screen;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.input.KeyListener;
import net.sixunderscore.mocha2d.input.MouseListener;
import net.sixunderscore.mocha2d.vulkan.util.SwapChain;
import net.sixunderscore.mocha2d.vulkan.util.ViewportScissor;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK14;

public class Window {
    private static long window;
    private static int width;
    private static int height;
    private static long surface;
    private static ViewportScissor viewportScissor;
    private static SwapChain swapChain;
    private static boolean shouldRebuildSwapChain = false;
    private static TextureManager texManager;
    private static Screen screen;
    private static BatchRenderer batch;
    private static OrthographicCamera camera;
    private static KeyListener keyListener;
    private static MouseListener mouseListener;
    private static FpsHelper fpsHelper;
    private static DeltaTime deltaTime;

    public static void start(WindowSettings settings, Screen initialScreen) {
        init(settings, initialScreen);
        loop();
        cleanUp();
    }

    private static void init(WindowSettings settings, Screen initialScreen) {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new IllegalStateException("Unable to find Vulkan loader");
        }

        VulkanManager.init();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, settings.isResizeable() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

        width = settings.getInitialWidth();
        height = settings.getInitialHeight();
        window = GLFW.glfwCreateWindow(width, height, settings.getWindowName(), MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create Window");
        }

        GLFW.glfwSetFramebufferSizeCallback(window, Window::updateViewport);
        keyListener = new KeyListener(window);
        mouseListener = new MouseListener(window);
        setWindowIcon(settings.getWindowIconPath());

        surface = createSurface();
        viewportScissor = new ViewportScissor();
        swapChain = new SwapChain(surface, viewportScissor.getScissor().extent(), settings.isVSyncEnabled());
        texManager = new TextureManager(settings.getTextureFiles(), settings.getTtfFiles());

        screen = initialScreen;
        screen.init(texManager);
        camera = new OrthographicCamera();
        batch = new BatchRenderer(texManager, swapChain);
        fpsHelper = new FpsHelper(settings.getFpsCap());
        deltaTime = new DeltaTime();
    }

    private static void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            if (shouldRebuildSwapChain) {
                viewportScissor.update();
                swapChain.rebuild(batch.getInFlightFences(), surface, viewportScissor.getScissor().extent());
                shouldRebuildSwapChain = false;
            }

            deltaTime.update();
            fpsHelper.updateCount();

            GLFW.glfwPollEvents();

            screen.update(keyListener, mouseListener);
            screen.render(batch);

            batch.draw(camera, swapChain, texManager, viewportScissor);

            fpsHelper.cap();
        }
    }

    private static void setWindowIcon(String path) {
        if (!path.isEmpty()) {
            try (TextureData textureData = ResourceUtils.loadAndDecodeImage(path);
                 GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
                icons.get(0).set(textureData.width(), textureData.height(), textureData.data());

                GLFW.glfwSetWindowIcon(window, icons);
            }
        }
    }

    private static long createSurface() {
        long[] surfaceArr = new long[1];
        if (GLFWVulkan.glfwCreateWindowSurface(VulkanManager.getInstance(), window, null, surfaceArr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create Vulkan surface");
        }

        return surfaceArr[0];
    }

    private static void updateViewport(long window, int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        camera.adjustProjection();
        shouldRebuildSwapChain = true;
    }

    public static void setScreen(Screen newScreen) {
        screen.cleanUp();
        screen = newScreen;
        screen.init(texManager);
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
        batch.close();
        texManager.close();
        viewportScissor.close();
        swapChain.close();
        KHRSurface.vkDestroySurfaceKHR(VulkanManager.getInstance(), surface, null);
        VulkanManager.cleanUp();
        GLFW.glfwSetErrorCallback(null).free();

        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
