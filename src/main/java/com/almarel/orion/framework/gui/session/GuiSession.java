package com.almarel.orion.framework.gui.session;

import com.almarel.orion.framework.gui.data.enums.BrowserTab;
import com.almarel.orion.framework.gui.data.enums.BrushTab;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * Хранит временные данные интерфейса для конкретного игрока.
 * (Страницы, вкладки, настройки отображения).
 * Данные живут только пока игрок на сервере.
 */
@Getter
@Setter
public class GuiSession {

    // --- Double Click & Drag/Swap State ---
    private long lastClickTime = 0;
    private String lastClickKey = null;
    private String swapSourceKey = null;

    public void clearSwapSourceKey() {
        this.swapSourceKey = null;
    }

    public boolean isSwapping() {
        return this.swapSourceKey != null;
    }

    // --- Enums ---
    public enum MacroFilter { PRESETS, PERSONAL }
    public enum ConstructFilter { PRESETS, PERSONAL }

    // --- Browser State ---
    private BrowserTab activeTab = BrowserTab.FAWE_SCHEMATICS;
    private File currentSchemDir;
    private File currentFavDir;

    // --- Shared Groups State ---
    private String activeSharedGroupId = null;
    private String activeSharedGroupName = null;
    private String activeSharedGroupOwnerUuid = null;

    private int page = 1;
    private boolean sortFoldersFirst = true;

    // --- Clipboard Preview State ---
    private String viewingBrushId = null;
    private int viewingBrushIndex = 0;
    private Material viewingBrushMaterial = null;

    // --- Menu State ---
    private BrushTab activeBrushTab = BrushTab.SETTINGS;
    private MenuType lastMenuType = MenuType.CLIPBOARD;

    // Запоминает, был ли игрок в Списке Кистей или в Настройках Кисти.
    private MenuType lastToolsMenuType = MenuType.LIST;

    // --- Anti-Spam / UX ---
    private boolean isFormulaExpanded = false;
    private long lastErrorTime = 0;
    private int macroPage = 0;
    private boolean scrollModeEnabled = true;

    private int groupListPage = 0;

    // --- Filters State ---
    private MacroFilter macroFilter = MacroFilter.PERSONAL;
    private ConstructFilter constructFilter = ConstructFilter.PERSONAL;

    // --- Selection State ---
    private String selectedConstructForAction = null;

    public GuiSession(File schemRoot, File favRoot) {
        this.currentSchemDir = schemRoot;
        this.currentFavDir = favRoot;
    }

    // ==========================================
    // MANUAL METHODS (Custom Logic & Clearers)
    // ==========================================

    public void clearActiveSharedGroupId() {
        this.activeSharedGroupId = null;
    }
}