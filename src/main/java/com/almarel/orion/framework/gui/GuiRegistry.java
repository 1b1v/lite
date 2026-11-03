package com.almarel.orion.framework.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.brush.presentation.gui.api.TabProvider;
import com.almarel.orion.brush.presentation.gui.api.WidgetProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class GuiRegistry {

    private final List<WidgetProvider> widgetProviders = new CopyOnWriteArrayList<>();


    // [NEW] Guice сам отдаст нам провайдеры из Multibinder прямо при создании Реестра!
    @Inject
    public GuiRegistry(
            Set<WidgetProvider> injectedWidgets
    ) {
        this.widgetProviders.addAll(injectedWidgets);

    }

    public List<WidgetProvider> getWidgetProviders() {
        return widgetProviders;
    }

}