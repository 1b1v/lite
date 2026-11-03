package com.almarel.orion.recolor.presentation.command;

import com.almarel.orion.framework.command.AbstractCommand;
import com.almarel.orion.framework.command.infrastructure.FaweSuggestionProvider;
import com.almarel.orion.framework.data.OrionPermissions;
import com.almarel.orion.framework.gui.GuiWindowManager;
import com.almarel.orion.framework.message.MessageManager;
import com.almarel.orion.framework.message.OrionMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * //recolor — только открытие GUI. Ручной debug-синтаксис
 * (//recolor <old> <new>) удалён: GUI полностью закрывает эту потребность.
 * Вся логика "как открыть меню" (проверка права, проверка selection,
 * синхронизация с GuiSession.lastMenuType) живёт в GuiWindowManager —
 * команда лишь точка входа, чтобы //recolor корректно участвовал в
 * общей системе "последнее активное меню" (/1b1v).
 */
@Singleton
public class RecolorCommand extends AbstractCommand {

    public static final List<String> ALIASES_RECOLOR = List.of(
            "//recolor", "/recolor", "recolor",
            "//recol", "/recol", "recol"
    );
    private static final String PERMISSION = OrionPermissions.RECOLOR_USE;

    private final MessageManager messageManager;
    private final GuiWindowManager guiWindowManager;

    @Inject
    public RecolorCommand(FaweSuggestionProvider faweSuggestionProvider,
                          MessageManager messageManager,
                          GuiWindowManager guiWindowManager) {
        super(faweSuggestionProvider);
        this.messageManager = messageManager;
        this.guiWindowManager = guiWindowManager;
    }

    @Override
    public @NotNull String getPermission() {
        return PERMISSION;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }

    @Override
    public boolean process(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (!checkPerm(player, PERMISSION)) return true;
        guiWindowManager.openRecolorMenu(player);
        return true;
    }

    @Override
    public @NotNull Collection<String> getCommands() {
        return ALIASES_RECOLOR;
    }

    private boolean checkPerm(Player p, String perm) {
        if (!p.hasPermission(perm)) {
            messageManager.send(p, OrionMessage.CMD_NO_PERM);
            return false;
        }
        return true;
    }
}