package net.sixunderscore.mocha2d.util;

public class DeltaTime {
    private long lastTime;
    private float deltaTime;

    public DeltaTime() {
        this.lastTime = System.nanoTime();
        this.deltaTime = 0;
    }

    public void update() {
        long currentTime = System.nanoTime();
        this.deltaTime = (currentTime - this.lastTime) / 1_000_000_000f;
        this.lastTime = currentTime;
    }

    public float get() {
        return this.deltaTime;
    }
}
