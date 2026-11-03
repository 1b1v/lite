package com.almarel.orion.framework.integration.fawe;

import com.fastasyncworldedit.core.queue.IChunkGet;
import com.sk89q.worldedit.world.World;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class ChunkAccessCache {
    private final World world;
    private final Long2ObjectOpenHashMap<IChunkGet> cacheMap;
    private long lastChunkKey = Long.MAX_VALUE;
    private IChunkGet lastChunk = null;

    public ChunkAccessCache(World world) {
        this.world = world;
        this.cacheMap = new Long2ObjectOpenHashMap<>();
    }

    public IChunkGet getChunk(int cx, int cz) {
        long cKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (cKey != lastChunkKey) {
            lastChunk = cacheMap.get(cKey);
            if (lastChunk == null) {
                lastChunk = world.get(cx, cz);
                cacheMap.put(cKey, lastChunk);
            }
            lastChunkKey = cKey;
        }
        return lastChunk;
    }
}