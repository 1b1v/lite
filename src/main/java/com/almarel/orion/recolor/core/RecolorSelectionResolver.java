// TYPE B: FULL FILE
package com.almarel.orion.recolor.core;

import com.almarel.orion.framework.integration.worldedit.WorldEditSessionResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class RecolorSelectionResolver {

    private final WorldEditSessionResolver sessionResolver;

    @Inject
    public RecolorSelectionResolver(WorldEditSessionResolver sessionResolver) {
        this.sessionResolver = sessionResolver;
    }

    public Optional<RecolorSelection> resolve(Player player) {
        return sessionResolver.resolve(player)
                .flatMap(ctx -> extractSelection(ctx.session()));
    }

    private Optional<RecolorSelection> extractSelection(LocalSession localSession) {
        var world = localSession.getSelectionWorld();
        if (world == null) {
            return Optional.empty();
        }

        try {
            var region = localSession.getSelection(world);
            return Optional.of(new RecolorSelection(world, region));
        } catch (IncompleteRegionException e) {
            return Optional.empty();
        }
    }
}