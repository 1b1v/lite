package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.capabilities.*;
import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.brush.presentation.parsing.FaweCommandBuilder;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class ArceonBoulderState extends ArceonConstructBrushState implements Sizeable, Smoothable, AxisSizeable, Hollowable,
        RotationModeable, Gravityable {

    // --- Уникальные параметры Boulder ---
    public double sizeZ = 10.0; // Бывший length (Arceon "Size")
    public int smoothIterations = 3;  // Бывший smoothness (Arceon "Amount")
    public double sizeY = 10.0; // Бывший height (Arceon "Height")
    public double sizeX = 10.0; // Бывший width (Arceon "Width")

    public boolean hollow = false;
    public boolean gravity = false;
    public BrushRotationMode rotationMode = BrushRotationMode.OFF;

    public ArceonBoulderState(Material tool, String mainCommandLabel, String arceonBrushName, String[] args) {
        super(tool, mainCommandLabel, arceonBrushName, args);
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Overrides)
    // ==========================================

    @Override
    public double getSize() {
        return sizeZ;
    }

    @Override
    public void setSize(double newSize) {
        double roundedTargetSize = Math.max(1.0, Math.round(newSize));

        if (this.sizeZ <= 0.0) {
            this.sizeZ = roundedTargetSize;
            this.sizeX = roundedTargetSize;
            this.sizeY = roundedTargetSize;
            return;
        }

        double ratio = roundedTargetSize / this.sizeZ;
        this.sizeX = Math.max(1.0, Math.round(this.sizeX * ratio));
        this.sizeY = Math.max(1.0, Math.round(this.sizeY * ratio));
        this.sizeZ = roundedTargetSize;
    }

    @Override
    public void setSmoothIterations(int smoothIterations) {
        this.smoothIterations = Math.max(0, Math.min(5, smoothIterations));
    }

    @Override
    public double updateAxisSize(String axis, double delta) {
        double limit = 50.0;
        switch (axis) {
            case "X":
                sizeX = Math.max(1.0, Math.min(sizeX + delta, limit));
                return sizeX;
            case "Y":
                sizeY = Math.max(1.0, Math.min(sizeY + delta, limit));
                return sizeY;
            default: // Z
                sizeZ = Math.max(1.0, Math.min(sizeZ + delta, limit));
                return sizeZ;
        }
    }

    @Override
    public void setRotationMode(BrushRotationMode mode) {
        this.rotationMode = mode != null ? mode : BrushRotationMode.OFF;
    }

    @Override
    public Set<BrushRotationMode> getAvailableRotationModes() {
        return EnumSet.of(BrushRotationMode.OFF, BrushRotationMode.ANGLE);
    }

    // Ломбок для boolean делает isGravity(), а интерфейс требует hasGravity(), поэтому пишем вручную
    @Override public boolean hasGravity() { return gravity; }
    @Override public void setGravity(boolean gravity) { this.gravity = gravity; }

    @Override
    public String getFullCommandString() {
        return new FaweCommandBuilder(mainCommandLabel)
                .withAlias(arceonBrushName)
                .withArg(pattern != null && !pattern.isEmpty() ? pattern : "stone")
                .withArg(sizeZ + "," + sizeZ)
                .withArg(smoothIterations)
                .withArg(sizeY + "," + sizeY)
                .withArg(sizeX + "," + sizeX)
                .withFlag("-h", hollow)
                .withFlag("-a", rotationMode == BrushRotationMode.ANGLE)
                .withFlag("-f", gravity)
                .build();
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return configStore.getCommon().tabs().getTitleSettings();
    }
}