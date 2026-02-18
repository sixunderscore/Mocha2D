package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.render.BatchRenderer;
import net.sixunderscore.mocha2d.graphics.resources.textures.TextureData;
import net.sixunderscore.mocha2d.graphics.resources.ResourceManager;
import net.sixunderscore.mocha2d.util.*;
import net.sixunderscore.mocha2d.util.FpsHelper;
import net.sixunderscore.mocha2d.vulkan.VulkanManager;
import net.sixunderscore.mocha2d.vulkan.util.SwapChain;
import net.sixunderscore.mocha2d.vulkan.util.ViewportScissor;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK14;

public class Window {
    private static long window;
    private static int fbWidth;
    private static int fbHeight;
    private static float xScale;
    private static float yScale;
    private static long surface;
    private static ViewportScissor viewportScissor;
    private static SwapChain swapChain;
    private static boolean shouldRebuildSwapChain = false;
    private static ResourceManager resourceManager;
    private static Screen screen;
    private static BatchRenderer batch;
    private static OrthographicCamera camera;
    private static InputCallbackManager inputCallbackManager;
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

        window = GLFW.glfwCreateWindow(settings.getInitialWidth(), settings.getInitialHeight(), settings.getWindowName(), MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to create Window");
        }

        int[] widthArr = new int[1];
        int[] heightArr = new int[1];
        GLFW.glfwGetFramebufferSize(window, widthArr, heightArr);
        fbWidth = widthArr[0];
        fbHeight = heightArr[0];

        GLFW.glfwSetFramebufferSizeCallback(window, (window, newWidth, newHeight) -> {
            fbWidth = newWidth;
            fbHeight = newHeight;
            camera.adjustProjection();
            screen.onWindowResized();
            shouldRebuildSwapChain = true;
        });

        GLFW.glfwGetWindowSize(window, widthArr, heightArr);
        xScale = (float) fbWidth / widthArr[0];
        yScale = (float) fbHeight / heightArr[0];

        GLFW.glfwSetWindowSizeCallback(window, (window, newWidth, newHeight) -> {
            xScale = (float) fbWidth / newWidth;
            yScale = (float) fbHeight / newHeight;
        });

        setWindowIcon(settings.getWindowIconPath());

        long[] surfaceArr = new long[1];
        if (GLFWVulkan.glfwCreateWindowSurface(VulkanManager.getInstance(), window, null, surfaceArr) != VK14.VK_SUCCESS) {
            throw new IllegalStateException("Failed to create Vulkan surface");
        }
        surface = surfaceArr[0];
        viewportScissor = new ViewportScissor();
        swapChain = new SwapChain(surface, viewportScissor.getScissor().extent());
        resourceManager = new ResourceManager(settings.getTextureFiles(), settings.getTtfFiles());

        screen = initialScreen;
        screen.init(resourceManager);
        inputCallbackManager = new InputCallbackManager(window, screen);
        camera = new OrthographicCamera();
        batch = new BatchRenderer(resourceManager, swapChain, settings.getClearColor());
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
            screen.render(batch);

            batch.draw(camera, swapChain, resourceManager, viewportScissor);

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

    public static void setScreen(Screen newScreen) {
        screen.cleanUp();
        screen = newScreen;
        screen.init(resourceManager);
        inputCallbackManager.setCallbacks(window, screen);
    }

    public static void setClearColor(Color color) {
        batch.setClearColor(color);
    }

    public static void setFpsCap(int fpsCap) {
        fpsHelper.setFpsCap(fpsCap);
    }

    public static int getWidth() {
        return fbWidth;
    }

    public static int getHeight() {
        return fbHeight;
    }

    public static float getXScale() {
        return xScale;
    }

    public static float getYScale() {
        return yScale;
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
        resourceManager.close();
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
