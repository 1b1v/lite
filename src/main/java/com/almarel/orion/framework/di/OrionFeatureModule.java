package com.almarel.orion.framework.di;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.infrastructure.ForeignCommandIndexService;
import com.almarel.orion.framework.command.infrastructure.TakeoverVisibilityListener;
import com.almarel.orion.framework.command.preprocessing.CommandPreprocessorStep;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.integration.versioncheck.CurrentEditionUpdateNotification;
import com.almarel.orion.framework.integration.versioncheck.LitePromotionNotificationService;
import com.almarel.orion.framework.integration.versioncheck.command.CheckUpdateSubcommand;
import com.almarel.orion.framework.integration.versioncheck.command.UpdateNotifySubcommand;
import com.almarel.orion.framework.command.subcommands.ReloadSubcommand;
import com.almarel.orion.framework.command.subcommands.WikiSubcommand;
import com.almarel.orion.framework.command.CommandInterceptor;
import com.almarel.orion.framework.command.infrastructure.GlobalCommandTabListener;
import com.almarel.orion.brush.fawe.ExternalCommandParser;
import com.almarel.orion.framework.notification.LoginNotification;
import org.bukkit.event.Listener;

public class OrionFeatureModule extends AbstractModule {
    @Override
    protected void configure() {
        // Утилиты парсинга
        bind(ExternalCommandParser.class).in(Scopes.SINGLETON);

        // Multibinder для нотификаций при входе
        Multibinder<LoginNotification> notificationBinder = Multibinder.newSetBinder(binder(), LoginNotification.class);
        notificationBinder.addBinding().to(CurrentEditionUpdateNotification.class);
        notificationBinder.addBinding().to(LitePromotionNotificationService.class);

        // Регистрация системных слушателей
        Multibinder<Listener> listenerBinder = Multibinder.newSetBinder(binder(), Listener.class);
        listenerBinder.addBinding().to(CommandInterceptor.class);
        listenerBinder.addBinding().to(ForeignCommandIndexService.class);
        listenerBinder.addBinding().to(GlobalCommandTabListener.class);
        listenerBinder.addBinding().to(TakeoverVisibilityListener.class);

        Multibinder<OrionSubCommand> subCommandBinder = Multibinder.newSetBinder(binder(), OrionSubCommand.class);
        subCommandBinder.addBinding().to(ReloadSubcommand.class);
        subCommandBinder.addBinding().to(WikiSubcommand.class);
        subCommandBinder.addBinding().to(UpdateNotifySubcommand.class);
        subCommandBinder.addBinding().to(CheckUpdateSubcommand.class);

        Multibinder<CommandPreprocessorStep> preprocessorBinder =
                Multibinder.newSetBinder(binder(), CommandPreprocessorStep.class);


        // Регистрация самостоятельных команд (AbstractOrionCommand)
        Multibinder<AbstractCommand> commandBinder = Multibinder.newSetBinder(binder(), AbstractCommand.class);
    }
}