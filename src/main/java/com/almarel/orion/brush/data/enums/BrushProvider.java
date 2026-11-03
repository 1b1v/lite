package com.almarel.orion.brush.data.enums;

public enum BrushProvider {
    ORION("1b1v"),
    FAWE("FAWE"),
    ARCEON("Arceon"),
    EZEDITS("EzEdits");

    private final String displayName;

    BrushProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}