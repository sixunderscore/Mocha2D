package net.sixunderscore.mocha2d.graphics;

import net.sixunderscore.mocha2d.graphics.resources.textures.TextureData;
import net.sixunderscore.mocha2d.util.*;
import net.sixunderscore.mocha2d.util.FpsHelper;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;

public class Window {
    private static long window;
    private static int fbWidth;
    private static int fbHeight;
    private static float xScale;
    private static float yScale;
    private static RenderBackend renderBackend;
    private static Screen screen;
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

        GLFW.glfwSetFramebufferSizeCallback(window, (_, newWidth, newHeight) -> {
            fbWidth = newWidth;
            fbHeight = newHeight;
            camera.adjustProjection();
            screen.onWindowResized();
            renderBackend.onWindowResize();
        });

        GLFW.glfwGetWindowSize(window, widthArr, heightArr);
        xScale = (float) fbWidth / widthArr[0];
        yScale = (float) fbHeight / heightArr[0];

        GLFW.glfwSetWindowSizeCallback(window, (_, newWidth, newHeight) -> {
            xScale = (float) fbWidth / newWidth;
            yScale = (float) fbHeight / newHeight;
        });

        setWindowIcon(settings.getWindowIconPath());

        screen = initialScreen;
        inputCallbackManager = new InputCallbackManager(window, screen);
        camera = new OrthographicCamera();
        fpsHelper = new FpsHelper(settings.getFpsCap());
        deltaTime = new DeltaTime();

        renderBackend = new RenderBackend(window, settings.getTextureFiles(), settings.getTtfFiles());

        byte[] clearColor = settings.getClearColor();
        setClearColor(clearColor[0], clearColor[1], clearColor[2]);

        screen.init(renderBackend.getResourceManager());
    }

    private static void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            deltaTime.update();
            fpsHelper.updateCount();

            GLFW.glfwPollEvents();

            renderBackend.render(camera, screen);

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
        screen.init(renderBackend.getResourceManager());
        inputCallbackManager.setCallbacks(window, screen);
    }

    public static void setClearColor(byte r, byte g, byte b) {
        renderBackend.setClearColor(r, g, b);
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
        renderBackend.close();
        GLFW.glfwSetErrorCallback(null).free();
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
