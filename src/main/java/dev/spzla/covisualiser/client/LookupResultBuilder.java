package dev.spzla.covisualiser.client;

public class LookupResultBuilder {
    private int x;
    private int y;
    private int z;
    private long timestamp;
    private String blockId;
    private String worldId;
    private String playerName;
    private String action;

    public LookupResultBuilder setX(int x) {
        this.x = x;
        return this;
    }

    public LookupResultBuilder setY(int y) {
        this.y = y;
        return this;
    }

    public LookupResultBuilder setZ(int z) {
        this.z = z;
        return this;
    }

    public LookupResultBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LookupResultBuilder setBlockId(String blockId) {
        this.blockId = blockId;
        return this;
    }

    public LookupResultBuilder setWorldId(String worldId) {
        this.worldId = worldId;
        return this;
    }

    public LookupResultBuilder setPlayerName(String playerName) {
        this.playerName = playerName;
        return this;
    }

    public LookupResultBuilder setAction(String action) {
        this.action = action;
        return this;
    }

    public void reset() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.blockId = null;
        this.worldId = null;
        this.playerName = null;
        this.action = null;
    }

    public LookupResult build() {
        return new LookupResult(x, y, z, timestamp, blockId, worldId, playerName, action);
    }
}
