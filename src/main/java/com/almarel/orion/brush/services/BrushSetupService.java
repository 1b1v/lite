package com.almarel.orion.brush.services;

import com.almarel.orion.brush.data.enums.BrushRotationMode;
import com.almarel.orion.brush.fawe.FaweIntegrationService;
import com.almarel.orion.brush.presentation.parsing.BrushParseFlags;
import com.almarel.orion.brush.state.BrushStateManager;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.almarel.orion.math.TransformMathUtils;
import com.google.inject.Inject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class BrushSetupService {

    private static final String DEFAULT_ANCHOR_MODE = "below";
    private static final int DEFAULT_COPY_RANGE = 200;
    private static final double[] IDENTITY_MATRIX = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};

    private final BrushStateManager stateManager;
    private final MessageManager messageManager;
    private final FaweIntegrationService faweIntegrationService;
    private final SmartMaterialParser materialParser;

    @Inject
    public BrushSetupService(BrushStateManager stateManager,
                             MessageManager messageManager,
                             FaweIntegrationService faweIntegrationService,
                             SmartMaterialParser materialParser) {
        this.stateManager = stateManager;
        this.messageManager = messageManager;
        this.faweIntegrationService = faweIntegrationService;
        this.materialParser = materialParser;
    }



    private void unbindConflicting(Player p, Material tool) {
        stateManager.removeBrushSettings(p, tool);
        stateManager.unbindExternalBrushes(p, tool);
    }

}