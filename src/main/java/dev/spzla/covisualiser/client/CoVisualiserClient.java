package dev.spzla.covisualiser.client;

import dev.spzla.covisualiser.client.parser.LookupResultParser;
import dev.spzla.covisualiser.client.screen.LookupResultListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoVisualiserClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CoVisualiser");
    public static CoVisualiserClient INSTANCE;

    Pattern rowCountPattern = Pattern.compile("CoreProtect - (\\d+) rows? found.");
    Pattern timestampPattern = Pattern.compile("\\d+[,.]\\d+/[mhd] ago ([+-]) (\\w+) (placed|broke|dropped|picked up) (\\w+)\\.");
    Pattern detailsPattern = Pattern.compile("\\(x(-?\\d+)/y(-?\\d+)/z(-?\\d+)/(\\w+)\\)( \\(a:([a-z]+)\\))?");
    Pattern cvPattern = Pattern.compile("#(covisualiser|covisualizer|covis|cv)");

    public DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    public List<LookupResult> results = new ArrayList<>();
    private final LookupResultBuilder resultBuilder = new LookupResultBuilder();
    public String commandUsed = "";
    int counter = 0;
    int toCount = 0;

    private long lastActivityTime = 0;
    private static final int TIMEOUT_MS = 10 * 1000;

    private final LookupResultParser parser = new LookupResultParser();

    private ParserState parserState = ParserState.DEFAULT;

    private static KeyBinding keyBinding;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("covisualiser", "test"));
    
    @Override
    public void onInitializeClient() {
        if (INSTANCE == null) {
            INSTANCE = this;
        }

        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.covisualiser.openlist",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        ClientReceiveMessageEvents.ALLOW_GAME.register(this::parseMessage);
        ClientSendMessageEvents.COMMAND.register(this::detectLookupCommand);
        ClientSendMessageEvents.MODIFY_COMMAND.register(this::modifyCommand);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                LookupResultListScreen screen = new LookupResultListScreen();

                client.setScreen(screen);
            }

            if (parserState != ParserState.DEFAULT && lastActivityTime > 0) {
                if (System.currentTimeMillis() - lastActivityTime > TIMEOUT_MS) {
                    LOGGER.warn("Parser hung in state {}. Triggering failsafe reset.", parserState);
                    sendStyledMessage("Lookup timed out. Internal state reset");

                    results.clear();
                    resetState();
                }
            }
        });

        LOGGER.info("CoVisualiser Initialized");
    }

    public static CoVisualiserClient getInstance() {
        return INSTANCE;
    }

    public void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            LOGGER.info("[CoVisualiser] Sending command: {}", command);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(command);
            }
        });
    }

    private void sendChatMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(message, false);
            }
        });
    }

    private void sendChatMessage(String message) {
        sendChatMessage(Text.literal(message));
    }

    private void sendStyledMessage(Text message) {
        MutableText text = Text.empty()
                .append(Text.literal("CoVisualiser").setStyle(Style.EMPTY.withColor(Constants.BRAND_COLOR)))
                .append(" - ")
                .append(message);

        sendChatMessage(text);
    }

    private void sendStyledMessage(String message) {
        sendStyledMessage(Text.literal(message));
    }

    public void resetState() {
        this.parserState = ParserState.DEFAULT;
        this.counter = 0;
        this.toCount = 0;
        this.lastActivityTime = 0;
    }

    private String modifyCommand(String s) {
        if (parserState.equals(ParserState.DEFAULT) && (s.startsWith("co l") || s.startsWith("co lookup")) && cvPattern.matcher(s).find() && !s.contains("#count")) {
            String str = s.replaceAll(cvPattern.pattern(), "");
            results.clear();
            resetState();
            parserState = ParserState.PARSING_COUNT;
            commandUsed = str;

            lastActivityTime = System.currentTimeMillis();

            return str + " #count";
        }

        return s;
    }

    private void detectLookupCommand(String s) {
        LOGGER.info("[CoVisualiser] Command used: {}", s);
        if (toCount > 0 && (s.startsWith("co l ") || s.startsWith("co lookup"))) {
            LOGGER.info("co l/lookup command used!");
        }
    }

    private void sendNextPageCommand() {
        int nextPage = (counter / 100) + 1;
        sendCommand(String.format("co l %s:100", nextPage));
    }


    private String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private boolean parseMessage(Text message, boolean overlay) {
        if (overlay || parserState.equals(ParserState.DEFAULT)) return true;
        
        String rawText = message.getString();
        String strippedText = stripFormatting(rawText);


        if (strippedText.equals("CoreProtect - Database busy. Please try again later.")) {
            sendStyledMessage("CoreProtect database busy. Please try again later.");
            resetState();
            return false;
        }
        if (strippedText.matches("(◀ )?Page \\d+/\\d+( ▶)? \\([ 0-9|.]+\\)")) return false;
        if (strippedText.equals("CoreProtect - Lookup searching. Please wait...")) return false;

        if (parserState.equals(ParserState.PARSING_COUNT)) {
            Matcher countMatcher = rowCountPattern.matcher(strippedText);
            if (countMatcher.find()) {
                toCount = Integer.parseInt(countMatcher.group(1));
                if (toCount != 0) {
                    parserState = ParserState.SENDING_COMMANDS;
                    CompletableFuture.runAsync(() -> {
                        sendCommand(commandUsed.replace("#count", ""));
                    }, CompletableFuture.delayedExecutor(Constants.COMMAND_DELAY, TimeUnit.MILLISECONDS));
                } else {
                    resetState();
                    sendStyledMessage("No results found.");
                }
                return false;
            }

            return true;
        } else if (parserState.equals(ParserState.SENDING_COMMANDS)) {
            if (strippedText.equals("----- CoreProtect |  Lookup Results -----")) {
                lastActivityTime = System.currentTimeMillis();
                CompletableFuture.runAsync(() -> {
                    MinecraftClient.getInstance().execute(() -> {
                        parserState = ParserState.PARSING_RESULTS;
                        sendNextPageCommand();
                    });
                }, CompletableFuture.delayedExecutor(Constants.COMMAND_DELAY, TimeUnit.MILLISECONDS));

                return false;
            }

            if (timestampPattern.matcher(strippedText).find()) return false;
            if (detailsPattern.matcher(rawText).find()) return false;
        } else if (parserState.equals(ParserState.PARSING_RESULTS)) {
            if (strippedText.equals("----- CoreProtect |  Lookup Results -----")) return false;

            Matcher timestampMatcher = timestampPattern.matcher(strippedText);
            if (timestampMatcher.find()) {
                lastActivityTime = System.currentTimeMillis();

                resultBuilder.setAction(timestampMatcher.group(1));
                resultBuilder.setPlayerName(timestampMatcher.group(2));
                resultBuilder.setBlockId(timestampMatcher.group(4));

                HoverEvent hoverEvent = message.getSiblings().getFirst().getStyle().getHoverEvent();

                if (hoverEvent instanceof HoverEvent.ShowText(Text value)) {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(value.getString(), dateFormatter);
                    resultBuilder.setTimestamp(zonedDateTime.toEpochSecond());
                }

                return false;
            }

            Matcher detailsMatcher = detailsPattern.matcher(strippedText);
            if (detailsMatcher.find()) {
                lastActivityTime = System.currentTimeMillis();

                resultBuilder.setX(Integer.parseInt(detailsMatcher.group(1)));
                resultBuilder.setY(Integer.parseInt(detailsMatcher.group(2)));
                resultBuilder.setZ(Integer.parseInt(detailsMatcher.group(3)));
                resultBuilder.setWorldId(detailsMatcher.group(4));

                if (detailsMatcher.group(5) != null && !Objects.equals(detailsMatcher.group(6), "block")) {
                    resultBuilder.reset();
                    counter++;
                } else {
                    results.add(resultBuilder.build());
                    resultBuilder.reset();
                    counter++;
                }

                if (counter >= toCount) {
                    sendStyledMessage("Parsing finished - %d results.".formatted(results.size()));

                    CompletableFuture.runAsync(() -> {
                        MinecraftClient.getInstance().execute(this::resetState);
                    }, CompletableFuture.delayedExecutor(250, TimeUnit.MILLISECONDS));
                } else if (counter % 100 == 0) {
                    CompletableFuture.runAsync(this::sendNextPageCommand, CompletableFuture.delayedExecutor(Constants.COMMAND_DELAY, TimeUnit.MILLISECONDS));
                }

                return false;
            }
        }

        return true;
    }
}
