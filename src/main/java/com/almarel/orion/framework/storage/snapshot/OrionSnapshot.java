package com.almarel.orion.framework.storage.snapshot;

import com.almarel.orion.brush.data.BrushState;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Простой DTO (Data Transfer Object) для хранения состояния игрока перед сохранением.
 */
@NoArgsConstructor
public class OrionSnapshot {

    public Map<String, BrushState> brushesData;



    public List<String> commandHistory;

    public Boolean dynamicSpeedEnabled;
    public Double dynamicSpeedMinLevel;
    public Double dynamicSpeedMaxLevel;
}