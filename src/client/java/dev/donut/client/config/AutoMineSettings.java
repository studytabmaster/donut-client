// language: Java, file: src/client/java/dev/donut/client/config/AutoMineSettings.java
package dev.donut.client.config;

public final class AutoMineSettings {
    public boolean enabled = false;

    public void sanitize() {
        // No numeric fields to clamp. Pathfinding limits live in PathPlanner constants.
    }
}
