package dev.spzla.covisualiser.client;

public class LookupResultBuilder {
    private int x;
    private int y;
    private int z;
    private String blockId;
    private String worldId;
    private String playerName;
    private String action;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setAction(String action) {
        this.action = action;
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
        return new LookupResult(x, y, z, blockId, worldId, playerName, action);
    }
}
