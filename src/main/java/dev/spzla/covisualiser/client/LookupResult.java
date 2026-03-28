package dev.spzla.covisualiser.client;

public record LookupResult(int x, int y, int z, String blockId, String worldId, String playerName, String action) {
    public double distanceTo(LookupResult other) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
