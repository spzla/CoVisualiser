package dev.spzla.covisualiser.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.spzla.covisualiser.client.CoVisualiserClient;
import dev.spzla.covisualiser.client.lookup.LookupResult;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class BlockMarkerRenderPipeline {
    private static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(CoVisualiserClient.MOD_ID, "pipeline/lines_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build()
    );

    private BlockMarkerRenderState blockMarkerState;
    private List<BlockMarkerRenderState> blockMarkerStates = List.of();

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer stagedBuffer = new StagedVertexBuffer(() -> "Block Marker Buffer", RenderType.SMALL_BUFFER_SIZE);

    public void extractBlockMarker(LevelExtractionContext context) {
        List<LookupResult> results = CoVisualiserClient.getInstance().results;

        this.blockMarkerStates = results.stream()
                .map(this::toMarker)
                .toList();
//        this.blockMarkerState = new BlockMarkerRenderState(0, 100, 0, "world", .8f, 0f, 0f, 0.4f);
    }

    public void renderAndDrawBlockMarker(LevelRenderContext context) {
        RenderPipeline renderPipeline = BlockMarkerRenderPipeline.LINES_THROUGH_WALLS;
        VertexFormat formatBinding = renderPipeline.getVertexFormatBinding(0);

        assert formatBinding != null;

        PrimitiveTopology primitive = renderPipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = stagedBuffer.appendDraw(formatBinding, primitive);

        this.renderBlockMarkers(context, draw);

        stagedBuffer.upload();

        StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);

        if (info != null) {
            draw(Minecraft.getInstance(), info, renderPipeline);
        }

        stagedBuffer.endFrame();
    }

    private void renderBlockMarkers(LevelRenderContext context, StagedVertexBuffer.Draw draw) {
        if (this.blockMarkerStates.isEmpty()) {
            return;
        }

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        final var builder = stagedBuffer.getVertexBuilder(draw);

        for (BlockMarkerRenderState marker : this.blockMarkerStates) {
            this.renderLineBox(
                    matrices.last(),
                    builder,
                    marker.x(),
                    marker.y(),
                    marker.z(),
                    marker.x() + 1,
                    marker.y() + 1,
                    marker.z() + 1,
                    marker.r(),
                    marker.g(),
                    marker.b(),
                    marker.a(),
                    2.0f
            );
        }

        matrices.popPose();
    }

    private void renderLineBox(PoseStack.Pose pose, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha, float width) {
        line(pose, buffer,
                minX, minY, minZ,
                maxX, minY, minZ,
                1, 0, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                1, 0, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                1, 0, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                1, 0, 0,
                red, green, blue, alpha,
                width);


        line(pose, buffer,
                minX, minY, minZ,
                minX, maxY, minZ,
                0, 1, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                0, 1, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                0, 1, 0,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                0, 1, 0,
                red, green, blue, alpha,
                width);


        line(pose, buffer,
                minX, minY, minZ,
                minX, minY, maxZ,
                0, 0, 1,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                0, 0, 1,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                0, 0, 1,
                red, green, blue, alpha,
                width);

        line(pose, buffer,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                0, 0, 1,
                red, green, blue, alpha,
                width);
    }

    private void line(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float nx, float ny, float nz,
            float red, float green, float blue, float alpha,
            float width
    ) {
        buffer.addVertex(pose, x1, y1, z1)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(width);

        buffer.addVertex(pose, x2, y2, z2)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(width);
    }

    private void renderFilledBox(Matrix4fc positionMatrix, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        // Front Face
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Back face
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);

        // Left face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Right face
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Top face
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Bottom face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);

    }

    private static void draw(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();

        assert colorTexture != null;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> CoVisualiserClient.MOD_ID + " example render pipeline rendering", colorTexture, Optional.empty(), mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());

            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public static void close() {
        stagedBuffer.close();
    }

    private BlockMarkerRenderState toMarker(LookupResult result) {
        float r;
        float g;
        float b;
        float a = 0.8f;

        // TODO: config
        switch (result) {
            case LookupResult.Block block -> {
                switch (block.change()) {
                    case ADD -> {
                        r = 0.2f;
                        g = 1.0f;
                        b = 0.2f;
                    }

                    case REMOVE -> {
                        r = 1.0f;
                        g = 0.2f;
                        b = 0.2f;
                    }

                    default -> {
                        r = 1.0f;
                        g = 1.0f;
                        b = 1.0f;
                    }
                }
            }

            case LookupResult.Item item -> {
                switch(item.change()) {
                    case ADD -> {
                        r = 0.1f;
                        g = 0.5f;
                        b = 1.0f;
                    }

                    case REMOVE -> {
                        r = 1.0f;
                        g = 0.5f;
                        b = 0.1f;
                    }

                    default -> {
                        r = 1.0f;
                        g = 1.0f;
                        b = 1.0f;
                    }
                }
            }

            case LookupResult.Container container -> {
                switch(container.change()) {
                    case ADD -> {
                        r = 0.6f;
                        g = 0.0f;
                        b = 0.9f;
                    }

                    case REMOVE -> {
                        r = 0.9f;
                        g = 0.0f;
                        b = 0.9f;
                    }

                    default -> {
                        r = 1.0f;
                        g = 1.0f;
                        b = 1.0f;
                    }
                }
            }

            default -> {
                r = 1.0f;
                g = 1.0f;
                b = 1.0f;
            }
        }

        return new BlockMarkerRenderState(
                result.x(),
                result.y(),
                result.z(),
                result.worldId(),
                r,
                g,
                b,
                a
        );
    }
}
