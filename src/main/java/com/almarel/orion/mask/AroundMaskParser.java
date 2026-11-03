package com.almarel.orion.mask;

import com.fastasyncworldedit.core.extension.factory.parser.RichParser;
import com.fastasyncworldedit.core.math.MutableBlockVector3;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.almarel.orion.framework.data.OrionPermissions;

import javax.annotation.Nonnull;
import java.util.BitSet;
import java.util.stream.Stream;

public class AroundMaskParser extends RichParser<Mask> {

    private static final int MAX_RADIUS = 10;
    private static final long MAX_VOLUME = 40_000_000L;

    public AroundMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#around", "#ar");
    }

    @Override
    protected Stream<String> getSuggestions(String argumentInput, int index, ParserContext context) {
        if (!hasPermission(context)) {
            return Stream.empty();
        }

        if (index == 0) {
            return worldEdit.getMaskFactory().getSuggestions(argumentInput, context).stream();
        }
        if (index == 1) {
            return Stream.of("1", "2", "3", "5");
        }
        return Stream.empty();
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!matchesThisPrefix(input)) {
            return super.parseFromInput(input, context);
        }
        if (!hasPermission(context)) {
            throw new InputParseException("No permission for #around mask.");
        }
        return super.parseFromInput(input, context);
    }

    private boolean matchesThisPrefix(String input) {
        String prefix = extractPrefix(input);
        return prefix.equalsIgnoreCase("#around") || prefix.equalsIgnoreCase("#ar");
    }

    private String extractPrefix(String input) {
        int bracketIndex = input.indexOf('[');
        return bracketIndex >= 0 ? input.substring(0, bracketIndex) : input;
    }

    @Override
    protected Mask parseFromInput(@Nonnull String[] arguments, ParserContext context) throws InputParseException {
        validateArgumentCount(arguments);

        Mask inner = worldEdit.getMaskFactory().parseFromInput(arguments[0], context);
        int radius = parseRadius(arguments);

        Region region = requireRegion(context);
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int sizeX = max.x() - min.x() + 1;
        int sizeY = max.y() - min.y() + 1;
        int sizeZ = max.z() - min.z() + 1;

        long volume = validateVolume(sizeX, sizeY, sizeZ);
        BitSet snapshot = generateSnapshot(inner, min, max, sizeX, sizeZ, volume);

        return new AroundMask(snapshot, radius, min.x(), min.y(), min.z(), sizeX, sizeY, sizeZ);
    }

    // --- Private Logic ---

    private boolean hasPermission(ParserContext context) {
        return context.getActor() == null || context.getActor().hasPermission(OrionPermissions.MASK_AROUND);
    }

    private void validateArgumentCount(String[] arguments) throws InputParseException {
        if (arguments.length < 1 || arguments.length > 2) {
            throw new InputParseException("Usage: around[mask][radius] (e.g. around[stone] or around[stone][2])");
        }
    }

    private Region requireRegion(ParserContext context) throws InputParseException {
        Region region = context.getSelection();
        if (region == null) {
            throw new InputParseException("around requires a region selection to pre-compute.");
        }
        return region;
    }

    private int parseRadius(String[] arguments) throws InputParseException {
        int radius = 1;
        if (arguments.length == 2) {
            try {
                radius = Integer.parseInt(arguments[1]);
            } catch (NumberFormatException e) {
                throw new InputParseException("Invalid radius: " + arguments[1]);
            }
        }
        if (radius <= 0) {
            throw new InputParseException("Radius must be positive");
        }
        return Math.min(radius, MAX_RADIUS);
    }

    private long validateVolume(int sizeX, int sizeY, int sizeZ) throws InputParseException {
        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            throw new InputParseException("#around max selection volume is " + MAX_VOLUME + ". Yours is " + volume);
        }
        return volume;
    }

    private BitSet generateSnapshot(Mask inner, BlockVector3 min, BlockVector3 max, int sizeX, int sizeZ, long volume) throws InputParseException {
        try {
            BitSet snapshot = new BitSet((int) volume);
            MutableBlockVector3 mutable = new MutableBlockVector3();

            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    for (int x = min.x(); x <= max.x(); x++) {
                        if (inner.test(mutable.setComponents(x, y, z))) {
                            snapshot.set(computeFlatIndex(x, y, z, min, sizeX, sizeZ));
                        }
                    }
                }
            }
            return snapshot;
        } catch (Throwable t) {
            throw new InputParseException("Error during snapshot generation: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
    }

    private int computeFlatIndex(int x, int y, int z, BlockVector3 min, int sizeX, int sizeZ) {
        return (x - min.x()) + (z - min.z()) * sizeX + (y - min.y()) * sizeX * sizeZ;
    }
}