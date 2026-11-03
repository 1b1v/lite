package com.almarel.orion.brush.fawe.data;

import com.almarel.orion.brush.data.presentation.TitledBrush;
import com.almarel.orion.brush.data.enums.OrionBrushType;
import com.almarel.orion.framework.gui.configurationlegacy.ConfigurationGuiStore;
import org.bukkit.Material;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class EzEditsBrushState extends FaweBrushState implements TitledBrush {

    public EzEditsBrushState(Material tool, String label, String[] args) {
        super(tool, label, args);
    }

    @Override
    public OrionBrushType getBrushType() {
        return OrionBrushType.FAWE;
    }

    @Override
    public String getMenuTitle(ConfigurationGuiStore configStore) {
        return configStore.getCommon().tabs().getTitleSettings();
    }
}