package com.almarel.orion.curve;

import com.almarel.orion.curve.liana.LianaGenerationService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.curve.liana.LianaCommand;
import com.almarel.orion.framework.command.AbstractCommand;

public class CurveModule extends AbstractModule {
    @Override
    protected void configure() {
        // Генераторы кривых (без Singleton, как было в оригинале)
        bind(LianaGenerationService.class);

        // Рендереры и утилиты кривых
        bind(CurvePathService.class).in(Scopes.SINGLETON);

        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
        commandBinder.addBinding().to(LianaCommand.class);
    }
}