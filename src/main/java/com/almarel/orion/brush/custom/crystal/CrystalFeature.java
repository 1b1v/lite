package com.almarel.orion.brush.custom.crystal;

import com.almarel.orion.brush.custom.crystal.data.CrystalBrushState;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.almarel.orion.brush.api.BrushFeature;
import com.almarel.orion.brush.handler.BrushHandler;
import com.almarel.orion.brush.presentation.command.handler.BrushSubCommandHandler;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.brush.procedural.generator.CrystalGenerator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CrystalFeature implements BrushFeature<CrystalBrushState> {

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public @NotNull String getId() { return "crystal"; }

    @Override
    public @NotNull Material getIcon() { return Material.AMETHYST_CLUSTER; }

    @Override
    public @NotNull Class<CrystalBrushState> getSettingsClass() { return CrystalBrushState.class; }

    @Override
    public @NotNull CrystalBrushState createDefaultSettings() { return new CrystalBrushState(); }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission(OrionPermissions.BRUSH_CRYSTAL_USE);
    }

    public static class Module extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), new TypeLiteral<BrushFeature<?>>() {})
                    .addBinding()
                    .to(CrystalFeature.class)
                    .in(Scopes.SINGLETON);

            Multibinder.newSetBinder(binder(), BrushSubCommandHandler.class)
                    .addBinding()
                    .to(CrystalBrushCommand.class)
                    .in(Scopes.SINGLETON);

            bind(CrystalGenerator.class).in(Scopes.SINGLETON);

            Multibinder.newSetBinder(binder(), BrushHandler.class)
                    .addBinding()
                    .to(CrystalBrushHandler.class)
                    .in(Scopes.SINGLETON);
        }
    }
}