package dev.spzla.covisualiser.client.render;

public record BlockMarkerRenderState(
    int x,
    int y,
    int z,
    String worldId,
    float r,
    float g,
    float b,
    float a
) { }
