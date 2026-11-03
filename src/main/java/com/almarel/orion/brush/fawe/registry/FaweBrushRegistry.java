package com.almarel.orion.brush.fawe.registry;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.almarel.orion.brush.fawe.builtins.BrushProcessor;
import com.almarel.orion.brush.fawe.builtins.ToolProcessor;
import com.almarel.orion.brush.fawe.processors.arceon.*;
import com.almarel.orion.brush.fawe.processors.fawe.*;
import com.almarel.orion.brush.fawe.processors.fawetools.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Словарь и реестр всех нативных кистей (FAWE/Arceon).
 * Отвечает ТОЛЬКО за маппинг строковых алиасов на процессоры и хранение их свойств (Конструкты).
 */
@Singleton
public class FaweBrushRegistry {

    private final Map<String, BrushProcessor> processors = new HashMap<>();
    private final Map<String, ToolProcessor> toolProcessors = new HashMap<>();
    private final Set<String> ezeditsBrushAliases = new HashSet<>();
    private final Set<String> arceonAliases = new HashSet<>();
    private final Set<String> constructSupportedAliases = new HashSet<>();

    public enum ToolStateKind { REPL, FLOOD, STACKER, PLAIN }

    private final Map<String, ToolStateKind> toolStateKinds = new HashMap<>();

    private final JavaPlugin plugin;

    @Inject
    public FaweBrushRegistry(Injector injector, JavaPlugin plugin) {
        this.plugin = plugin;
        registerFaweTools(injector);
        registerFaweBrushes(injector);
        registerArceonBrushes(injector);
        initMetadataSets();
    }

    private void registerFaweTools(Injector injector) {
        safeRegisterTool(injector, FarwandToolProcessor.class, "farwand", "warwand");
        safeRegisterTool(injector, TreeToolProcessor.class, "tree");
        safeRegisterTool(injector, ReplToolProcessor.class, "repl", "replacer");
        safeRegisterTool(injector, FloodFillToolProcessor.class, "flood", "floodfill");
        safeRegisterTool(injector, InfoToolProcessor.class, "info");
        safeRegisterTool(injector, CyclerToolProcessor.class, "cycler");
        safeRegisterTool(injector, StackerToolProcessor.class, "stacker");
        safeRegisterTool(injector, LrBuildToolProcessor.class, "lrbuild");
        safeRegisterTool(injector, InspectToolProcessor.class, "inspect");
    }

    private void registerFaweBrushes(Injector injector) {
        safeRegisterBrush(injector, SphereBrushProcessor.class, "sphere", "s");
        safeRegisterBrush(injector, CylinderBrushProcessor.class, "cyl", "cylinder", "c");
        safeRegisterBrush(injector, SurfaceBrushProcessor.class, "surface");
        safeRegisterBrush(injector, GravityBrushProcessor.class, "gravity", "grav");
        safeRegisterBrush(injector, ExtinguishBrushProcessor.class, "ex", "extinguish");
        safeRegisterBrush(injector, BlobBrushProcessor.class, "blob", "rock");
        safeRegisterBrush(injector, CliffBrushProcessor.class, "cliff", "flatcylinder");
        safeRegisterBrush(injector, BlendBallBrushProcessor.class, "blendball", "bb", "blend");
        safeRegisterBrush(injector, ErodeBrushProcessor.class, "erode");
        safeRegisterBrush(injector, CircleBrushProcessor.class, "circle");
        safeRegisterBrush(injector, LineBrushProcessor.class, "line", "l");
        safeRegisterBrush(injector, SplineBrushProcessor.class, "spline", "spl", "curve");
    }

    private void registerArceonBrushes(Injector injector) {
        safeRegisterArceonBrush(injector, ArceonBoulderProcessor.class, "boulder");
        safeRegisterArceonBrush(injector, ArceonSpikeProcessor.class, "spike");
        safeRegisterArceonBrush(injector, ArceonCropProcessor.class, "crop", "crops");
        safeRegisterArceonBrush(injector, ArceonVineProcessor.class, "vine");
        safeRegisterArceonBrush(injector, ArceonCubeProcessor.class, "cube", "cuboid");
        safeRegisterArceonBrush(injector, ArceonRevolveProcessor.class, "revolve", "rev");
        safeRegisterArceonBrush(injector, ArceonErodeBlendProcessor.class, "erodeblend", "eb");
    }

    /**
     * ЗАЩИТНЫЙ БАРЬЕР ВЕРСИОННОЙ СОВМЕСТИМОСТИ.
     * <p>
     * Guice при вызове {@code injector.getInstance(clazz)} сканирует ВСЕ сигнатуры методов
     * класса (через {@code Class.getDeclaredMethods()}), включая приватные. Если в сигнатуре
     * встречается тип из WorldEdit/FAWE API, отсутствующий в установленной на сервере версии
     * (пример: {@code TreeType} не существовал в этом пакете до определённой версии WorldEdit),
     * JVM бросает {@link NoClassDefFoundError} при попытке резолвинга этого типа.
     * <p>
     * Это Error, а не Exception, и без явного перехвата он убивает ВЕСЬ Guice-граф,
     * так как FaweBrushRegistry — транзитивная зависимость почти всех GUI-классов.
     * Поэтому здесь мы жертвуем ОДНИМ инструментом/кистью, а не всем плагином.
     */
    private void safeRegisterTool(Injector injector, Class<? extends ToolProcessor> clazz, String... aliases) {
        try {
            registerToolProcessor(injector.getInstance(clazz), aliases);
        } catch (Throwable t) {
            logUnavailable(clazz, t, aliases);
        }
    }

    private void safeRegisterBrush(Injector injector, Class<? extends BrushProcessor> clazz, String... aliases) {
        try {
            registerProcessor(injector.getInstance(clazz), aliases);
        } catch (Throwable t) {
            logUnavailable(clazz, t, aliases);
        }
    }

    private void safeRegisterArceonBrush(Injector injector, Class<? extends BrushProcessor> clazz, String... aliases) {
        try {
            registerArceonProcessor(injector.getInstance(clazz), aliases);
        } catch (Throwable t) {
            logUnavailable(clazz, t, aliases);
        }
    }

    private void logUnavailable(Class<?> clazz, Throwable cause, String... aliases) {
        plugin.getLogger().info(
                "Processor " + clazz.getSimpleName()
                        + " is not available on this WorldEdit/FAWE API version ("
                        + cause.getClass().getSimpleName()
                        + "). Disabled aliases: " + String.join(", ", aliases));
    }

    private void initMetadataSets() {
        toolStateKinds.put("repl", ToolStateKind.REPL);
        toolStateKinds.put("replacer", ToolStateKind.REPL);
        toolStateKinds.put("flood", ToolStateKind.FLOOD);
        toolStateKinds.put("floodfill", ToolStateKind.FLOOD);
        toolStateKinds.put("stacker", ToolStateKind.STACKER);

        constructSupportedAliases.addAll(Set.of(
                "sphere", "s", "cyl", "cylinder", "c", "surface", "gravity", "grav",
                "smooth", "ex", "extinguish", "blob", "rock", "cliff", "flatcylinder",
                "blendball", "bb", "blend", "erode", "circle",
                "boulder", "spike", "crop", "crops", "vine", "cube", "cuboid",
                "revolve", "rev", "erodeblend", "eb"

        ));

        ezeditsBrushAliases.addAll(Set.of(
                "petalpainter", "paletteshift", "gradient", "gradientstroke",
                "place", "pl", "array", "ar"
        ));
    }

    public ToolStateKind classifyToolState(String alias) {
        if (alias == null) return ToolStateKind.PLAIN;
        return toolStateKinds.getOrDefault(alias.toLowerCase(), ToolStateKind.PLAIN);
    }

    private void registerProcessor(BrushProcessor processor, String... aliases) {
        for (String alias : aliases) {
            processors.put(alias.toLowerCase(), processor);
        }
    }

    private void registerToolProcessor(ToolProcessor processor, String... aliases) {
        for (String alias : aliases) {
            toolProcessors.put(alias.toLowerCase(), processor);
        }
    }

    private void registerArceonProcessor(BrushProcessor processor, String... aliases) {
        for (String alias : aliases) {
            String lower = alias.toLowerCase();
            processors.put(lower, processor);
            arceonAliases.add(lower);
        }
    }

    public BrushProcessor getBrushProcessor(String alias) {
        return processors.get(alias.toLowerCase());
    }

    public ToolProcessor getToolProcessor(String alias) {
        return toolProcessors.get(alias.toLowerCase());
    }

    public boolean isArceonBrush(String alias) {
        return arceonAliases.contains(alias.toLowerCase());
    }

    public boolean isEzEditsBrush(String alias) {
        if (alias == null) return false;
        return ezeditsBrushAliases.contains(alias.toLowerCase());
    }

    public boolean supportsConstructs(String alias) {
        if (alias == null) return false;
        return constructSupportedAliases.contains(alias.toLowerCase());
    }

    public boolean hasProcessor(String alias) {
        String lower = alias.toLowerCase();
        return processors.containsKey(lower) || toolProcessors.containsKey(lower);
    }

    public boolean isTool(String alias) {
        if (alias == null) return false;
        return toolProcessors.containsKey(alias.toLowerCase());
    }
}