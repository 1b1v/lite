package com.almarel.orion.brush.data.capabilities;

import org.bukkit.Material;
import java.util.Set;

public interface IgnoreListable {
    Set<Material> getIgnoreMaterials();
    void setIgnoreMaterials(Set<Material> mats);
    boolean isIgnoreWhitelist();
    void setIgnoreWhitelist(boolean whitelist);

    boolean isUseTargetBlock();
    void setUseTargetBlock(boolean use);
}