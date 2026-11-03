package com.almarel.orion.brush.custom.plump;

import com.almarel.orion.brush.custom.plump.data.PlumpBrushState;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.brush.api.BrushFeature;
import com.almarel.orion.brush.handler.BrushHandler;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.brush.procedural.generator.PlumpGenerator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlumpFeature implements BrushFeature<PlumpBrushState> {

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public @NotNull String getId() { return "plump"; }

    @Override
    public @NotNull Material getIcon() { return Material.SLIME_BALL; }

    @Override
    public @NotNull Class<PlumpBrushState> getSettingsClass() { return PlumpBrushState.class; }

    @Override
    public @NotNull PlumpBrushState createDefaultSettings() { return new PlumpBrushState(); }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission(OrionPermissions.BRUSH_PLUMP_USE);
    }

    public static class Module extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), new TypeLiteral<BrushFeature<?>>() {})
                    .addBinding()
                    .to(PlumpFeature.class)
                    .in(Scopes.SINGLETON);

            Multibinder.newSetBinder(binder(), BrushSubCommandHandler.class)
                    .addBinding()
                    .to(PlumpBrushCommand.class)
                    .in(Scopes.SINGLETON);

            bind(PlumpGenerator.class).in(Scopes.SINGLETON);

            Multibinder.newSetBinder(binder(), BrushHandler.class)
                    .addBinding()
                    .to(PlumpBrushHandler.class)
                    .in(Scopes.SINGLETON);
        }
    }
}