package com.almarel.orion.framework.gui.configurationlegacy;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigurationGuiStore {

    private final CommonGuiConfig common;
    private final BrushListGuiConfig brushList;
    private final BrushGuiConfig brushGui; // Переименовано и очищено
    private final CopyPasteGuiConfig cp;
    private final ProceduralGuiConfig pro;
    private final ConstructGuiConfig construct;
    private final CommandGuiConfig command;
    private final ClipboardGuiConfig clipboard;

    public ConfigurationGuiStore(FileConfiguration source) {
        this.common = new CommonGuiConfig(source);
        this.brushList = new BrushListGuiConfig(source);
        this.brushGui = new BrushGuiConfig(source);
        this.cp = new CopyPasteGuiConfig(source);
        this.pro = new ProceduralGuiConfig(source);
        this.construct = new ConstructGuiConfig(source);
        this.command = new CommandGuiConfig(source);
        this.clipboard = new ClipboardGuiConfig(source);
    }

    public void reload(FileConfiguration newSource) {
        common.updateSource(newSource);
        brushList.updateSource(newSource);
        brushGui.updateSource(newSource);
        cp.updateSource(newSource);
        pro.updateSource(newSource);
        construct.updateSource(newSource);
        command.updateSource(newSource);
        clipboard.updateSource(newSource);
    }

    public CommonGuiConfig getCommon() { return common; }
    public BrushListGuiConfig getBrushList() { return brushList; }
    public BrushGuiConfig getBrushGui() { return brushGui; }
    public CopyPasteGuiConfig getCP() { return cp; }
    public ProceduralGuiConfig getPro() { return pro; }
    public ConstructGuiConfig getConstruct() { return construct; }
    public CommandGuiConfig getCommand() { return command; }
    public ClipboardGuiConfig getClipboard() { return clipboard; }
}