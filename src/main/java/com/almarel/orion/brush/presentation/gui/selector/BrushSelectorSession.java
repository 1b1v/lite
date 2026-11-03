package com.almarel.orion.brush.presentation.gui.selector;

import java.util.List;

/**
 * Приватное UI-состояние GUI-селектора кистей для одного игрока:
 * фильтр, страница, снапшот сортировки, выбранная для контекстного
 * меню кисть, раскрытие формулы.
 *
 * Вынесено из общего GuiSession (framework.gui.session) — эти поля
 * специфичны ТОЛЬКО для selector-фичи и не должны захламлять общий
 * инфраструктурный объект (см. аудит: isFormulaExpanded раньше жил
 * в GuiSession ради одного конкретного типа кисти — FormulaBrushState).
 *
 * Жизненный цикл — как у RecolorGuiSession: создаётся лениво при
 * первом открытии меню, живёт пока игрок на сервере (см. Store).
 */
public class BrushSelectorSession {

    public enum BrushFilter { PERSONAL, PRESETS }

    private List<String> selectorSnapshot = null;
    private BrushFilter brushFilter = BrushFilter.PERSONAL;
    private int brushListPage = 0;
    private String selectedBrushKey = null;
    private boolean formulaExpanded = false;

    public List<String> getSelectorSnapshot() {
        return selectorSnapshot;
    }

    public void setSelectorSnapshot(List<String> selectorSnapshot) {
        this.selectorSnapshot = selectorSnapshot;
    }

    public void clearSelectorSnapshot() {
        this.selectorSnapshot = null;
    }

    public boolean hasSelectorSnapshot() {
        return selectorSnapshot != null && !selectorSnapshot.isEmpty();
    }

    public BrushFilter getBrushFilter() {
        return brushFilter;
    }

    public void setBrushFilter(BrushFilter brushFilter) {
        this.brushFilter = brushFilter;
    }

    public int getBrushListPage() {
        return brushListPage;
    }

    public void setBrushListPage(int brushListPage) {
        this.brushListPage = brushListPage;
    }

    public String getSelectedBrushKey() {
        return selectedBrushKey;
    }

    public void setSelectedBrushKey(String selectedBrushKey) {
        this.selectedBrushKey = selectedBrushKey;
    }

    public void clearSelectedBrushKey() {
        this.selectedBrushKey = null;
    }

    public boolean isFormulaExpanded() {
        return formulaExpanded;
    }

    public void setFormulaExpanded(boolean formulaExpanded) {
        this.formulaExpanded = formulaExpanded;
    }
}