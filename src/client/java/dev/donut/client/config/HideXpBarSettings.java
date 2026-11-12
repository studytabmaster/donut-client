package dev.donut.client.config;

public final class HideXpBarSettings {
    public boolean enabled = false;
    public boolean favorite = false;

    public void sanitize() {
        // Boolean only.
    }
}
