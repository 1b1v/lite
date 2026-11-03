package com.almarel.orion.generation;

import com.almarel.orion.generation.onion.OnionGenerationService;
import com.almarel.orion.generation.platform.PlatformGenerationService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.generation.onion.OnionCommand;
import com.almarel.orion.generation.platform.PlatformCommand;

public class GenerationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PlatformGenerationService.class).in(Scopes.SINGLETON);
        bind(OnionGenerationService.class).in(Scopes.SINGLETON);

        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
        commandBinder.addBinding().to(PlatformCommand.class);
        commandBinder.addBinding().to(OnionCommand.class);
    }
}