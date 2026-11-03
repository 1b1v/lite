package com.almarel.orion.selection;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.framework.command.AbstractCommand;

public class SelectionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SelectionManagementService.class).in(Scopes.SINGLETON);

    }
}