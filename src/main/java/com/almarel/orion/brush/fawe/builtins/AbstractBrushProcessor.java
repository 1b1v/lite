package com.almarel.orion.brush.fawe.builtins;

import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBrushProcessor implements BrushProcessor {

    protected final String permission;
    protected final FaweParserService parserService;

    // Добавили инъекцию сервиса через конструктор абстрактного класса
    public AbstractBrushProcessor(String permission, FaweParserService parserService) {
        this.permission = permission;
        this.parserService = parserService;
    }

    protected abstract Brush createBrush(Player player, ParsingResult result);

    @Override
    public boolean process(Player player, BrushTool tool, String[] args) {
        ParsingResult result = parseArguments(player, args);
        Brush brush = createBrush(player, result);
        if (brush == null) return false;

        configureTool(tool, brush, result);
        return true;
    }

    private void configureTool(BrushTool tool, Brush brush, ParsingResult result) {
        tool.setBrush(brush, permission);
        applySettings(tool, result);
    }

    private ParsingResult parseArguments(Player player, String[] args) {
        ParsingResult res = new ParsingResult();
        res.player = player;

        partitionArguments(args, res.flags, res.args);
        populateStandardParameters(player, res);

        return res;
    }

    private void partitionArguments(String[] args, List<String> flags, List<String> cleanArgs) {
        for (String arg : args) {
            if (arg.startsWith("-")) {
                flags.add(arg.toLowerCase());
            } else {
                cleanArgs.add(arg);
            }
        }
    }

    private void populateStandardParameters(Player player, ParsingResult res) {
        if (!res.args.isEmpty()) {
            org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(player);
            res.pattern = parserService.parsePattern(bukkitPlayer, res.args.get(0));
        }
        if (res.args.size() > 1) {
            res.radius = parserService.parseRadius(res.args.get(1), 5);
        }
    }

    private org.bukkit.entity.Player resolveBukkitPlayer(Player player) {
        return Bukkit.getPlayer(player.getUniqueId());
    }

    /**
     * Стандартная реализация: Arg 0 = Pattern, Arg 1 = Radius
     */
    protected void applySettings(BrushTool tool, ParsingResult result) {
        if (result.pattern != null) {
            tool.setFill(result.pattern);
        }
        tool.setSize(result.radius);
    }

    @Override
    public String getPermission() {
        return permission;
    }

    // Убрали 'static', чтобы класс имел доступ к parserService из внешнего класса
    protected class ParsingResult {
        public Player player;
        public List<String> args = new ArrayList<>();
        public List<String> flags = new ArrayList<>();

        public Pattern pattern = null;
        public double radius = 5;

        public boolean hasFlag(String flag) { return flags.contains(flag.toLowerCase()); }

        public int getIntArg(int index, int def) {
            if (index >= args.size()) return def;
            return (int) parserService.parseRadius(args.get(index), def);
        }

        public String getArg(int index) {
            if (index >= args.size()) return null;
            return args.get(index);
        }
    }
}