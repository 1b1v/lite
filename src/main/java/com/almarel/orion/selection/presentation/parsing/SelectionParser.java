package com.almarel.orion.selection.presentation.parsing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.command.exceptions.CommandException;
import com.almarel.orion.framework.message.OrionMessage;

@Singleton
public class SelectionParser {

    @Inject
    public SelectionParser() {
    }

    // --- DTO ---

    public record OsnSettings(int radiusX, int radiusY, int radiusZ) {}

    // --- PARSERS ---

    public OsnSettings parseOsn(String[] args) {
        OsnRawInput input = extractOsnInput(args);

        if (input.isEmpty()) {
            throw new CommandException(OrionMessage.CMD_OSN_USAGE);
        }

        return resolveOsnRadii(input);
    }

    // --- PRIVATE UTILS FOR OSN ---

    private record OsnRawInput(int globalRadius, Integer hRadius, Integer vRadius, boolean hFlag, boolean vFlag) {
        public boolean isEmpty() {
            return globalRadius == 0 && hRadius == null && vRadius == null && !hFlag && !vFlag;
        }
    }

    private OsnRawInput extractOsnInput(String[] args) {
        int globalRadius = 0;
        Integer hRadius = null;
        Integer vRadius = null;
        boolean hFlag = false;
        boolean vFlag = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim().toLowerCase();

            if (arg.equals("-h") || arg.equals("-hand")) {
                hFlag = true;
                if (i + 1 < args.length && isInteger(args[i + 1])) {
                    hRadius = Integer.parseInt(args[++i]);
                }
            } else if (arg.equals("-v")) {
                vFlag = true;
                if (i + 1 < args.length && isInteger(args[i + 1])) {
                    vRadius = Integer.parseInt(args[++i]);
                }
            } else if (isInteger(arg)) {
                globalRadius = Integer.parseInt(arg);
            }
        }

        return new OsnRawInput(globalRadius, hRadius, vRadius, hFlag, vFlag);
    }

    private OsnSettings resolveOsnRadii(OsnRawInput input) {
        int rx, ry, rz;

        if (input.hRadius() != null) {
            rx = rz = input.hRadius();
        } else if (input.hFlag()) {
            rx = rz = input.globalRadius();
        } else {
            rx = rz = input.vFlag() ? 0 : input.globalRadius();
        }

        if (input.vRadius() != null) {
            ry = input.vRadius();
        } else if (input.vFlag()) {
            ry = input.globalRadius();
        } else {
            ry = input.hFlag() ? 0 : input.globalRadius();
        }

        return new OsnSettings(Math.max(rx, 0), Math.max(ry, 0), Math.max(rz, 0));
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}