package com.almarel.orion.brush.fawe.builtins;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import org.bukkit.entity.Player;

/**
 * Single Source of Truth for parsing FAWE inputs (Patterns, Masks, Vectors).
 * Uses native WorldEdit engine, supporting advanced syntaxes like 'hand' and 'slot1'.
 */
@Singleton
public class FaweParserService {

    @Inject
    public FaweParserService() {
        // Stateless Service
    }

    /**
     * Validates a pattern without returning it.
     * @throws IllegalArgumentException if the pattern is invalid (contains FAWE error message).
     */
    public void validatePattern(Player bukkitPlayer, String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be empty.");
        }

        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(bukkitPlayer);

        try {
            ParserContext context = new ParserContext();
            context.setActor(wePlayer);
            context.setWorld(wePlayer.getWorld());
            context.setSession(WorldEdit.getInstance().getSessionManager().get(wePlayer));

            PatternFactory factory = WorldEdit.getInstance().getPatternFactory();
            factory.parseFromInput(input, context);
        } catch (InputParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Validates a mask without returning it.
     * @throws IllegalArgumentException if the mask is invalid.
     */
    public void validateMask(Player bukkitPlayer, String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Mask cannot be empty.");
        }

        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(bukkitPlayer);

        try {
            ParserContext context = new ParserContext();
            context.setActor(wePlayer);
            context.setWorld(wePlayer.getWorld());
            context.setSession(WorldEdit.getInstance().getSessionManager().get(wePlayer));

            WorldEdit.getInstance().getMaskFactory().parseFromInput(input, context);
        } catch (InputParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid mask syntax.");
        }
    }

    /**
     * Parses a string into a FAWE Pattern. Returns null if parsing fails.
     */
    public Pattern parsePattern(Player bukkitPlayer, String input) {
        if (input == null || input.isEmpty()) return null;

        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(bukkitPlayer);

        try {
            ParserContext context = new ParserContext();
            context.setActor(wePlayer);
            context.setWorld(wePlayer.getWorld());
            context.setSession(WorldEdit.getInstance().getSessionManager().get(wePlayer));

            PatternFactory factory = WorldEdit.getInstance().getPatternFactory();
            return factory.parseFromInput(input, context);
        } catch (InputParseException e) {
            return null;
        }
    }

    /**
     * Parses a string into a FAWE Mask. Returns null if parsing fails.
     */
    public Mask parseMask(Player bukkitPlayer, String input) {
        if (input == null || input.isEmpty()) return null;

        com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(bukkitPlayer);

        try {
            ParserContext context = new ParserContext();
            context.setActor(wePlayer);
            context.setWorld(wePlayer.getWorld());
            context.setSession(WorldEdit.getInstance().getSessionManager().get(wePlayer));

            return WorldEdit.getInstance().getMaskFactory().parseFromInput(input, context);
        } catch (Exception e) {
            return null;
        }
    }

    public double parseRadius(String input, double def) {
        if (input == null) return def;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Parses a 2D Vector (e.g. for Arceon tools).
     * Formats: "5" -> (5,5) or "5,10" -> (5,10).
     */
    public Vector2 parseVector2(String input, double defX, double defZ) {
        if (input == null || input.isEmpty()) return Vector2.at(defX, defZ);
        try {
            if (input.contains(",")) {
                String[] parts = input.split(",");
                double x = Double.parseDouble(parts[0]);
                double z = Double.parseDouble(parts[1]);
                return Vector2.at(x, z);
            } else {
                double val = Double.parseDouble(input);
                return Vector2.at(val, val);
            }
        } catch (NumberFormatException e) {
            return Vector2.at(defX, defZ);
        }
    }

    /**
     * Parses a 3D Vector.
     * Formats: "5" -> (5,5,5) or "5,10,5" -> (5,10,5).
     */
    public Vector3 parseVector3(String input, double def) {
        if (input == null || input.isEmpty()) return Vector3.at(def, def, def);
        try {
            if (input.contains(",")) {
                String[] parts = input.split(",");
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double z = Double.parseDouble(parts[2]);
                    return Vector3.at(x, y, z);
                }
            }
            double val = Double.parseDouble(input);
            return Vector3.at(val, val, val);
        } catch (NumberFormatException e) {
            return Vector3.at(def, def, def);
        }
    }
}