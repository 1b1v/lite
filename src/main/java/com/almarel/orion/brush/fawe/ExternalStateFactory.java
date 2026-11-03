// TYPE B: FULL FILE
package com.almarel.orion.brush.fawe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.enums.BrushProvider;
import com.almarel.orion.brush.fawe.data.*;
import com.almarel.orion.brush.fawe.registry.FaweBrushRegistry;
import org.bukkit.Material;

/**
 * Фабрика для создания объектов BrushState из сырых текстовых команд.
 * Инкапсулирует логику выбора конкретного класса состояния (Arceon, FAWE, EZEdits).
 */
@Singleton
public class ExternalStateFactory {

    private static final String DEFAULT_ALIAS = "unknown";
    private static final String BOULDER_ALIAS = "boulder";

    private final FaweIntegrationService integrationService;
    private final FaweBrushRegistry aliasManager;

    @Inject
    public ExternalStateFactory(FaweIntegrationService integrationService, FaweBrushRegistry aliasManager) {
        this.integrationService = integrationService;
        this.aliasManager = aliasManager;
    }

    public BrushState createState(Material tool, String label, String alias, String[] cleanArgs) {
        String safeAlias = normalizeAlias(alias);

        if (isEzEditsCommand(label)) {
            return createEzEditsState(tool, label, cleanArgs, safeAlias);
        }

        BrushProvider provider = integrationService.identifyProvider(safeAlias);

        if (integrationService.isTool(safeAlias)) {
            return createToolState(tool, label, cleanArgs, safeAlias, provider);
        }

        boolean supportsConstructs = integrationService.supportsConstructs(safeAlias);

        if (provider == BrushProvider.ARCEON) {
            return createArceonState(tool, label, cleanArgs, safeAlias, supportsConstructs, provider);
        }

        return createFaweState(tool, label, cleanArgs, safeAlias, supportsConstructs, provider);
    }

    private String normalizeAlias(String alias) {
        return alias != null ? alias : DEFAULT_ALIAS;
    }

    private boolean isEzEditsCommand(String label) {
        if (label == null) {
            return false;
        }
        String cmd = label.toLowerCase();
        return cmd.startsWith("/ezbr") || cmd.startsWith("//ezbrush");
    }

    private BrushState createEzEditsState(Material tool, String label, String[] cleanArgs, String alias) {
        EzEditsBrushState ezState = new EzEditsBrushState(tool, label, cleanArgs);
        ezState.setFaweBrushName(alias);
        ezState.setProvider(BrushProvider.EZEDITS);
        return ezState;
    }

    private BrushState createToolState(Material tool, String label, String[] cleanArgs, String alias, BrushProvider provider) {
        FaweToolState toolState = switch (aliasManager.classifyToolState(alias)) {
            case REPL -> new FaweReplToolState(tool, label, alias, cleanArgs);
            case FLOOD -> new FaweFloodToolState(tool, label, alias, cleanArgs);
            case STACKER -> new FaweStackerToolState(tool, label, alias, cleanArgs);
            case PLAIN -> new FaweToolState(tool, label, alias, cleanArgs);
        };

        toolState.setProvider(provider);
        return toolState;
    }

    private BrushState createArceonState(Material tool, String label, String[] cleanArgs, String alias, boolean supportsConstructs, BrushProvider provider) {
        // [ORION_CUT: ArceonBoulderState]
        if (BOULDER_ALIAS.equalsIgnoreCase(alias)) {
            ArceonBoulderState boulderState = new ArceonBoulderState(tool, label, alias, cleanArgs);
            boulderState.setProvider(provider);
            return boulderState;
        }
        // [ORION_CUT_END]

        // [ORION_CUT: ArceonConstructBrushState]
        if (supportsConstructs) {
            ArceonConstructBrushState constructState = new ArceonConstructBrushState(tool, label, alias, cleanArgs);
            constructState.setProvider(provider);
            return constructState;
        }
        // [ORION_CUT_END]

        ArceonBrushState basicArceon = new ArceonBrushState(tool, label, alias, cleanArgs);
        basicArceon.setProvider(provider);
        return basicArceon;
    }

    private BrushState createFaweState(Material tool, String label, String[] cleanArgs, String alias, boolean supportsConstructs, BrushProvider provider) {
        // [ORION_CUT: FaweConstructBrushState]
        if (supportsConstructs) {
            FaweConstructBrushState constructState = new FaweConstructBrushState(tool, label, cleanArgs);
            constructState.setFaweBrushName(alias);
            constructState.setProvider(provider);
            return constructState;
        }
        // [ORION_CUT_END]

        FaweBrushState basicFawe = new FaweBrushState(tool, label, cleanArgs);
        basicFawe.setFaweBrushName(alias);
        basicFawe.setProvider(provider);
        return basicFawe;
    }
}