package com.almarel.orion.framework.integration.worldedit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
public class WorldEditSessionResolver {

    private final WorldEdit worldEdit;

    @Inject
    public WorldEditSessionResolver(WorldEdit worldEdit) {
        this.worldEdit = worldEdit;
    }

    public Optional<WeSession> resolve(Player player) {
        var wePlayer = BukkitAdapter.adapt(player);
        if (wePlayer == null) return Optional.empty();

        LocalSession session = worldEdit.getSessionManager().get(wePlayer);
        if (session == null) return Optional.empty();

        return Optional.of(new WeSession(wePlayer, session));
    }

    public record WeSession(com.sk89q.worldedit.entity.Player wePlayer, LocalSession session) {}
}