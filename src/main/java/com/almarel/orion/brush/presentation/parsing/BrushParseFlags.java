package com.almarel.orion.brush.presentation.parsing;

import com.almarel.orion.brush.data.enums.BrushRotationMode;

import java.util.List;

public record BrushParseFlags(
        BrushRotationMode rotationMode,
        boolean zOnlyAir,
        Integer rollOffsetDeg,
        boolean ignoreAirCopy,
        List<String> rawIgnoreMaterials,
        boolean ignoreWhitelist,
        Integer smearDepth,
        Integer smearHeight,
        Integer smearHeightPercent,
        Integer smoothIterations,
        boolean localSmear,
        Double scale,
        Integer scaleSmoothIterations
) {}