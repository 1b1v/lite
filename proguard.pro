# ==============================================================================
#                 ORION ENGINE — PROGUARD OBFUSCATION CONFIG
# ==============================================================================
# Architecture: Clean Architecture / Google Guice Strict DI / Paper 1.21.8
# Target Platform: PaperMC / FAWE 2.16.0
# ==============================================================================


# [SECTION-01] COMPILER & RUNTIME CORE DIRECTIVES

# Shrinking and optimization are handled via Maven/JVM brushParseFlags.
# ProGuard focuses purely on name obfuscation and byte-flow control.
-dontshrink
-dontoptimize
-dontwarn
-ignorewarnings

# Deterministic obfuscation dictionary using unprintable/similar symbol spaces
-obfuscationdictionary proguard_dictionary.txt
-classobfuscationdictionary proguard_dictionary.txt
-packageobfuscationdictionary proguard_dictionary.txt

# Invariant: Aggressive overload & package flattening break Paper runtime remapping
# -overloadaggressively
# -repackageclasses ''
# -allowaccessmodification

# Essential JVM metadata required for Guice (@Inject), Bukkit (@EventHandler),
# Jackson/Gson serialization, and clean production crash analysis.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable


# [SECTION-02] BUKKIT ENTRYPOINT

# JavaPlugin is loaded by Bukkit's JavaPluginLoader using strict class reflection.
# Must retain public/protected visibility for the primary plugin class.
-keep public class com.almarel.orion.Orion {
    public protected *;
}


# [SECTION-03] GOOGLE GUICE DEPENDENCY INJECTION (STRICT DI)

# 03.1. Modules: Class names are obfuscated! Only the zero-arg constructor is
# preserved so FeatureModuleScanner can instantiate dynamic feature modules via reflection.
-keepclassmembers class * implements com.google.inject.Module {
    public <init>();
}

# 03.2. Retain brush feature package hierarchy so FeatureModuleScanner can locate classes
-keeppackagenames com.almarel.orion.brush.custom.**

# 03.3. Preserves DI injection points across all application services
-keepclassmembers class * {
    @com.google.inject.Inject <init>(...);
    @com.google.inject.Inject <fields>;
}


# [SECTION-04] RUNTIME REFLECTION HOOKS (BOOTSTRAP & AUDIT)

# Specific classes referenced via getSimpleName() checks inside Bootstrap / Listeners.
-keep class com.almarel.orion.framework.command.infrastructure.GlobalCommandTabListener
-keep class com.almarel.orion.shortcut.AliasTabListener


# [SECTION-05] FAWE INTEGRATION (BRIDGE & EXTENSIONS)

# FAWE and Piston resolve custom patterns, adapters, and brushes via class metadata.
-keep class com.almarel.orion.pattern.clipboard.FullCopyPatternParser {
    *;
}
-keep class com.almarel.orion.pattern.clipboard.FullCopyPattern {
    *;
}
-keep class com.almarel.orion.brush.fawe.adapters.** {
    *;
}


# [SECTION-06] BUKKIT EVENT BUS

# 06.1. Methods annotated with @EventHandler must preserve their signature for EventExecutor
-keepclassmembers class * {
    @org.bukkit.event.EventHandler *;
}

# 06.2. Custom plugin events must retain getHandlers() / getHandlerList() static methods
-keep class **.*Event {
    *;
}


# [SECTION-07] SERIALIZATION MODELS (GSON DTO & PERSISTENCE)

# Field names must remain intact to prevent schema corruption in SQLite/JSON storages.
# 07.1. Framework models and configuration data
-keep class com.almarel.orion.framework.data.** {
    *;
}

# 07.2. Domain entities and presets across all features (e.g. brush states, macros)
-keep class com.almarel.orion.**.data.** {
    *;
}

# 07.3. Standard enum serialization support
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 07.4. Brush states outside **.data.** packages (move into data/ to drop this rule)
-keep class com.almarel.orion.brush.fawe.types.** {
    *;
}


# [SECTION-08] SHADED & RELOCATED THIRD-PARTY LIBRARIES

# 08.1. Libby Runtime Dependency Downloader (relies on internal ClassLoader reflection)
-keep class com.almarel.orion.lib.libby.** {
    *;
}

# 08.2. bStats Metrics (shaded and relocated; background dispatch threads and payloads)
-keep class com.almarel.orion.bstats.** {
    *;
}