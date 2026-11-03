package com.almarel.orion.brush.presentation.gui.selector;

import com.google.inject.Inject;
import com.almarel.orion.brush.data.BrushState;
import com.almarel.orion.brush.data.presentation.Headerable;
import com.almarel.orion.framework.gui.common.GuiItemService.ItemBuilder;
import com.almarel.orion.framework.gui.configurationlegacy.BrushListGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.CommonGuiConfig;
import com.almarel.orion.framework.gui.configurationlegacy.Palette;
import com.almarel.orion.framework.gui.session.GuiSession;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BrushItemRenderer {

    private final BrushListGuiConfig config;
    private final CommonGuiConfig commonGuiConfig;
    private final BrushDescriptionService descriptionService;

    @Inject
    public BrushItemRenderer(BrushListGuiConfig config,
                             CommonGuiConfig commonGuiConfig,
                             BrushDescriptionService descriptionService
    ) {
        this.config = config;
        this.commonGuiConfig = commonGuiConfig;
        this.descriptionService = descriptionService;
    }

    /**
     * @param session         общая GuiSession — нужна только ради isScrollModeEnabled() (домен Clipboard Preview).
     * @param selectorSession selector-специфичная сессия (selectedBrushKey, formulaExpanded).
     */
    public ItemBuilder render(BrushState state, Player player, GuiSession session, BrushSelectorSession selectorSession) {
        if (state instanceof Headerable headered) {
            return renderHeadered(headered, state, selectorSession);
        }




        return buildUniversalItem(state, player, selectorSession);
    }

    private ItemBuilder renderHeadered(Headerable headered, BrushState state, BrushSelectorSession selectorSession) {
        String statusText = resolveStatusText(state);
        String displayName = headered.getDisplayHeader() + " " + statusText;

        List<String> lore = new ArrayList<>(descriptionService.getParameters(state, null, false));
        appendControls(lore, state);

        return finalizeBuilder(state, displayName, lore, selectorSession);
    }

    private ItemBuilder finalizeBuilder(BrushState state, String displayName, List<String> lore, BrushSelectorSession selectorSession) {
        ItemBuilder builder = new ItemBuilder(state.getTool()).name(displayName).lore(lore);

        if (selectorSession.getSelectedBrushKey() != null && selectorSession.getSelectedBrushKey().equals(state.getStorageKey())) {
            builder.glow();
        } else if (state.isPersistent()) {
            builder.glow();
        }

        return builder;
    }

    private void appendControls(List<String> lore, BrushState state) {
        lore.add(" ");
        lore.add("  §fLMB §7- Equip brush");
        lore.add("  §fDouble LMB §7- Move brush");

        appendSortPriorityControlHint(lore, state, state.getSortPriority());

        lore.add("  §fRMB §7- Delete brush");
        lore.add("  §fShift+RMB §7- Open Settings");
    }

    private void appendSortPriorityControlHint(List<String> lore, BrushState state, int level) {
        if (level == 1) {
            lore.add("  §fShift+LMB §7- Make permanent");
            return;
        }


        lore.add("  §fShift+LMB §7- Make temporary");
    }

    private boolean isDefaultPreset(BrushState state) {
        boolean isDefault = false;
        return isDefault;
    }

    /**
     * Единая точка определения статуса кисти для GUI.
     * Приоритет пользователя (Вариант B): как только кисть сделана Permanent,
     * метка "Default" скрывается, даже если это потомок глобального пресета.
     */
    private String resolveStatusText(BrushState state) {
        if (state.isPersistent()) {
            return config.status().getPerm();
        }
        return isDefaultPreset(state) ? config.status().getDefault() : config.status().getTemp();
    }

    private ItemBuilder buildUniversalItem(BrushState state, Player player, BrushSelectorSession selectorSession) {
        String baseName = resolveUniversalBaseName(state);
        String statusLine = resolveStatusText(state);
        String localName = baseName + " " + statusLine;

        List<String> lore = new ArrayList<>(descriptionService.getParameters(state, player, false));
        appendControls(lore, state);

        return finalizeBuilder(state, localName, lore, selectorSession);
    }

    private String resolveUniversalBaseName(BrushState state) {
        return switch (state.getBrushType()) {
            case COPY -> config.headers().getCopy();
            case PLUMP -> config.headers().getPlump();
            case CRYSTAL -> config.headers().getCrystal();
            default -> Palette.VAL_HIGH + state.getClass().getSimpleName().replace("BrushState", "");
        };
    }



}