package com.almarel.orion.brush.data.presentation;

import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;

/**
 * Позволяет состоянию кисти самому определять свой заголовок в GUI.
 * Устраняет хардкод instanceof в роутерах.
 */
public interface TitledBrush {
    String getMenuTitle(ConfigurationGuiStore configStore);
}