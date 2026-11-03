package com.almarel.orion.brush.data.enums;

public enum ScrollAction {
    SCALE("scroll.action.scale"),
    SCALE_AXIS("scroll.action.scale-axis"),
    SIZE_AXIS("scroll.action.size-axis"), // [NEW] Абсолютный размер по осям
    ROTATION("scroll.action.rotation"),
    BUFFER_SLOT("scroll.action.buffer-slot"),
    SIZE("scroll.action.size");

    private final String configKey;

    ScrollAction(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }
}