package dev.spzla.covisualiser.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.spzla.covisualiser.client.config.CoVisualiserConfig;
import dev.spzla.covisualiser.client.lookup.*;
import dev.spzla.covisualiser.client.render.BlockMarkerRenderPipeline;
import dev.spzla.covisualiser.client.screen.LookupResultListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoVisualiserClient implements ClientModInitializer {
    public static final String MOD_ID = "covisualiser";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static CoVisualiserClient INSTANCE;

    private final BlockMarkerRenderPipeline blockMarkerRenderPipeline = new BlockMarkerRenderPipeline();

    private final Pattern CV_PATTERN = Pattern.compile("#(covisualiser|covisualizer|covis|cv)");

    public List<LookupResult> results = new ArrayList<>();

    public String commandUsed = "";

    private final LookupResultParser lookupResultParser = new LookupResultParser();
    private LookupSession lookupSession;

    public int currentPage = 0;
    public Set<Integer> readIds = new HashSet<>();

    private static KeyMapping keyBinding;
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            CoVisualiserClient.MOD_ID,
                            "general"
                    )
            );
    
    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            INSTANCE = this;
        }

        getConfig().load();

        keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.covisualiser.openlist",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        LevelExtractionEvents.END_EXTRACTION.register(blockMarkerRenderPipeline::extractBlockMarker);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(blockMarkerRenderPipeline::renderAndDrawBlockMarker);

        ClientReceiveMessageEvents.ALLOW_GAME.register(this::handleMessage);
        ClientSendMessageEvents.MODIFY_COMMAND.register(this::modifyCommand);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        LOGGER.info("CoVisualiser Initialized");
    }

    private void onClientTick(Minecraft client) {
        while (keyBinding.consumeClick()) {
            LookupResultListScreen screen = new LookupResultListScreen();

            client.setScreenAndShow(screen);
        }

        // TODO: re-implement timeout
    }

    private String modifyCommand(String command) {
        if (!getConfig().enabled) {
            return command;
        }

        if (lookupSession != null) {
            return command;
        }

        if (!isLookupCommand(command)) {
            return command;
        }

        if (command.contains("#count")) {
            return command;
        }

        Matcher cv = CV_PATTERN.matcher(command);
        if (!cv.find()) {
            return command;
        }

        String lookupCommand = cv.replaceAll("").trim();

        startLookup(lookupCommand);

        return lookupCommand + " #count";
    }

    private boolean handleMessage(Component message, boolean overlay) {
        if (!getConfig().enabled || lookupSession == null) {
            return true;
        }

        boolean propagate = lookupSession.handle(message, overlay);

        if (lookupSession.isComplete()) {
            finishLookup();
        }

        return propagate;
    }

    private void startLookup(String command) {
        results.clear();
        resetState();

        commandUsed = command;
        LookupCommandSender commandSender = page ->
                CompletableFuture.runAsync(
                        () -> sendCommand(
                                "co l %d:100".formatted(page)
                        ),
                        CompletableFuture.delayedExecutor(
                                Constants.COMMAND_DELAY,
                                TimeUnit.MILLISECONDS
                        )
                );

        lookupSession = new LookupSession(
                lookupResultParser,
                commandSender
        );

        lookupSession.start();
    }

    private void finishLookup() {
        LookupSession session = lookupSession;

        if (session == null) {
            return;
        }

        lookupSession = null;

        List<LookupResult> parsedResults = session.results();
        LookupSession.Completion completion = session.completion();

        results.clear();

        switch (completion) {
            case SUCCESS -> {
                results.addAll(parsedResults);

                sendStyledMessage(
                    "Parsing finished - %d results."
                            .formatted(results.size()));
            }

            case NO_RESULTS ->
                    sendStyledMessage("No results found.");

            case NO_PARAMS ->
                    sendStyledMessage("Please specify a user or radius to lookup.");

            case NO_TIME ->
                    sendStyledMessage("Please specify the amount of time to lookup.");

            case USER_NOT_FOUND ->
                    sendStyledMessage("User not found.");

            case DATABASE_BUSY ->
                    sendStyledMessage("CoreProtect database busy. Please try again later.");

            case TIMEOUT ->
                    sendStyledMessage("Lookup timed out. Internal state reset.");
        }
    }

    private boolean isLookupCommand(String command) {
        String[] parts = command.trim().split("\\s+");

        return parts.length >= 2
                && parts[0].equals("co")
                && (parts[1].equals("l") || parts[1].equals("lookup"));
    }

    public void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            LOGGER.info("[{}] Sending command: {}", MOD_ID, command);
            if (client.getConnection() != null) {
                client.getConnection().sendCommand(command);
            }
        });
    }

    private void sendChatMessage(Component message) {
        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(message);
            }
        });
    }

    private void sendChatMessage(String message) {
        sendChatMessage(Component.literal(message));
    }

    private void sendStyledMessage(Component message) {
        MutableComponent text = Component.empty()
                .append(Component.literal("CoVisualiser").setStyle(Style.EMPTY.withColor(Constants.BRAND_COLOR)))
                .append(" - ")
                .append(message);

        sendChatMessage(text);
    }

    private void sendStyledMessage(String message) {
        sendStyledMessage(Component.literal(message));
    }

    public void resetState() {
        this.currentPage = 0;
        this.readIds.clear();
    }

    public static CoVisualiserClient getInstance() {
        return INSTANCE;
    }

    public static CoVisualiserConfig getConfig() {
        return CoVisualiserConfig.INSTANCE;
    }
}
