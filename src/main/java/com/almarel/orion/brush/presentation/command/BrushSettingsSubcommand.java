package com.almarel.orion.brush.presentation.command;

import com.google.inject.Inject;
import com.almarel.orion.framework.command.subcommands.OrionSubCommand;
import com.almarel.orion.framework.gui.data.enums.MenuType;
import com.almarel.orion.framework.gui.GuiWindowManager;
import com.almarel.orion.framework.gui.session.GuiSession;
import com.almarel.orion.framework.gui.session.GuiStateManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BrushSettingsSubcommand implements OrionSubCommand {

    private final GuiWindowManager guiWindowManager;
    private final GuiStateManager sessionManager;

    @Inject
    public BrushSettingsSubcommand(GuiWindowManager guiWindowManager, GuiStateManager sessionManager) {
        this.guiWindowManager = guiWindowManager;
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() { return "brush"; }

    @Override
    public List<String> getAliases() { return Collections.singletonList("b"); }

    /**
     * CORE_USE уже гарантирован централизованным floor-gate в
     * OrionCommand.hasBasicAccess() — повторная проверка здесь избыточна.
     * TODO: если будет введено BRUSH_USE (домен пока не имеет отдельного
     * USE-гейта, решение не принято) — вернуть проверку сюда и в
     * BrushSelectorSubcommand симметрично.
     */
    @Override
    public String getPermission() { return null; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return true;
        GuiSession session = sessionManager.getSession(player);
        session.setLastMenuType(MenuType.SETTINGS);
        guiWindowManager.openBrushSettings(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}