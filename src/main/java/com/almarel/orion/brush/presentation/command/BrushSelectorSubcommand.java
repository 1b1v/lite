package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.gui.GuiWindowManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BrushSelectorSubcommand implements OrionSubCommand {

    private final GuiWindowManager guiWindowManager;

    @Inject
    public BrushSelectorSubcommand(GuiWindowManager guiWindowManager) {
        this.guiWindowManager = guiWindowManager;
    }

    @Override
    public String getName() { return "brushes"; }

    @Override
    public List<String> getAliases() { return Collections.singletonList("bs"); }

    /**
     * Floor CORE_USE уже проверен централизованно в OrionCommand.hasBasicAccess().
     * Домен Brush пока не имеет отдельного USE-гейта (модель "browse free,
     * gate on select" — доступ к конкретным кистям регулируется точечными
     * правами: BRUSH_PLUMP_USE, BRUSH_CRYSTAL_USE и т.д.). Если решим ввести
     * BRUSH_USE как floor для вкладки — вернуть сюда проверку.
     */
    @Override
    public String getPermission() { return null; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        guiWindowManager.openBrushSelector(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}