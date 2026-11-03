package com.almarel.orion.region;

import com.almarel.orion.region.centervertical.CenterVerticalGenerationService;
import com.almarel.orion.region.frame.FrameGenerationService;
import com.almarel.orion.region.polygon.PolygonGenerationService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.region.centervertical.CenterVerticalCommand;
import com.almarel.orion.region.frame.FrameCommand;
import com.almarel.orion.region.polygon.PolygonCommand;

public class RegionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FrameGenerationService.class).in(Scopes.SINGLETON);
        bind(PolygonGenerationService.class).in(Scopes.SINGLETON);
        bind(CenterVerticalGenerationService.class).in(Scopes.SINGLETON);

        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
        commandBinder.addBinding().to(FrameCommand.class);
        commandBinder.addBinding().to(PolygonCommand.class);
        commandBinder.addBinding().to(CenterVerticalCommand.class);
    }
}