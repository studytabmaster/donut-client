// language: Java, file: src/client/java/dev/donut/client/streaming/HideXpBar.java
package dev.donut.client.streaming;

import dev.donut.client.config.ClientConfig;

public final class HideXpBar {
    private final ClientConfig config;
    public HideXpBar(ClientConfig config) { this.config = config; }

    public boolean shouldHide() {
        return config.hideXpBar != null && config.hideXpBar.enabled;
    }
}
