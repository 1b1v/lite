package com.almarel.orion.selection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;

@Singleton
public class SelectionManagementService {

    @Inject
    public SelectionManagementService() {
    }

    /**
     * Устанавливает кубоидное выделение вокруг игрока.
     * Чистый адаптер — никаких сообщений.
     */
    public void setPlayerSelection(Player player, int rx, int ry, int rz) {
        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        World world = resolveSelectionWorld(localSession, player);

        BlockVector3 center = BukkitAdapter.asBlockVector(player.getLocation());
        CuboidRegionSelector selector = createCenteredCuboidSelector(world, center, rx, ry, rz);

        applySelection(localSession, wePlayer, world, selector);
    }

    private World resolveSelectionWorld(LocalSession localSession, Player player) {
        World world = localSession.getSelectionWorld();
        return (world != null) ? world : BukkitAdapter.adapt(player.getWorld());
    }

    private CuboidRegionSelector createCenteredCuboidSelector(World world, BlockVector3 center, int rx, int ry, int rz) {
        BlockVector3 min = center.subtract(rx, ry, rz);
        BlockVector3 max = center.add(rx, ry, rz);
        return new CuboidRegionSelector(world, min, max);
    }

    private void applySelection(LocalSession localSession,
                                com.sk89q.worldedit.entity.Player wePlayer,
                                World world,
                                CuboidRegionSelector selector) {
        localSession.setRegionSelector(world, selector);
        localSession.dispatchCUISelection(wePlayer);
    }
}