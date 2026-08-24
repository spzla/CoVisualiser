package dev.spzla.covisualiser.client.lookup;

import dev.spzla.covisualiser.client.DateFormats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupResultParser {
    private static final Pattern COUNT = Pattern.compile("CoreProtect - ([\\d,]+) rows? found\\.");
    private static final Pattern TIMESTAMP = Pattern.compile("\\d+[,.]\\d+/[mhd] ago");
    private static final Pattern BLOCK = Pattern.compile("([+-]) ([#\\w.]+) (placed|broke) (\\w+)\\.");
    private static final Pattern ITEM = Pattern.compile("([+-]) ([#\\w.]+) (picked up|dropped) x(\\d+) (\\w+)\\.");
    private static final Pattern CONTAINER = Pattern.compile("([+-]) ([#\\w.]+) (added|removed) x(\\d+) (\\w+)\\.");
    private static final Pattern LOCATION = Pattern.compile("\\(x(-?\\d+)/y(-?\\d+)/z(-?\\d+)/(\\w+)\\)(?: \\(a:([a-z]+)\\))?");

    private static final Pattern PAGE_INFO = Pattern.compile("(◀ )?Page \\d+/\\d+( ▶)? \\([ 0-9|.]+\\)");
    private static final Pattern USER_NOT_FOUND = Pattern.compile("CoreProtect - User \"([#\\w.]+)\" not found\\.");

    private static final String DATABASE_BUSY = "CoreProtect - Database busy. Please try again later.";
    private static final String PAGE_HEADER = "----- CoreProtect |  Lookup Results -----";
    private static final String SEARCHING = "CoreProtect - Lookup searching. Please wait...";
    private static final String NO_RESULTS = "CoreProtect - No results founds.";
    private static final String NO_PARAMS = "CoreProtect - Please specify a user or radius to lookup.";
    private static final String NO_TIME = "CoreProtect - Please specify the amount of time to lookup.";

    public Optional<LookupMessage> parse(Component message) {
        String rawText = message.getString();
        String strippedText = stripFormatting(rawText);

        if (PAGE_INFO.matcher(strippedText).matches()) {
            return Optional.of(new LookupMessage.PageInfo());
        }

        Matcher userNotFound = USER_NOT_FOUND.matcher(strippedText);
        if (userNotFound.matches()) {
            return Optional.of(new LookupMessage.UserNotFound(userNotFound.group(1)));
        }

        switch (strippedText) {
            case SEARCHING -> {
                return Optional.of(new LookupMessage.Searching());
            }
            case DATABASE_BUSY -> {
                return Optional.of(new LookupMessage.DatabaseBusy());
            }
            case PAGE_HEADER -> {
                return Optional.of(new LookupMessage.PageHeader());
            }
            case NO_PARAMS -> {
                return Optional.of(new LookupMessage.NoParams());
            }
            case NO_TIME -> {
                return Optional.of(new LookupMessage.NoTime());
            }
            case NO_RESULTS -> {
                return Optional.of(new LookupMessage.NoResults());
            }
        }

        Matcher countMatcher = COUNT.matcher(strippedText);
        if (countMatcher.matches()) {
            int count = Integer.parseInt(countMatcher.group(1).replace(",", ""));
            return Optional.of(new LookupMessage.Count(count));
        }

        Matcher timestampMatcher = TIMESTAMP.matcher(strippedText);
        if (timestampMatcher.find()) {
            HoverEvent hoverEvent = message.getSiblings().getFirst().getStyle().getHoverEvent();
            long timestamp = 0;

            if (hoverEvent instanceof HoverEvent.ShowText(Component value)) {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(
                        fixDate(value.getString()),
                        DateFormats.COREPROTECT_TIMESTAMP
                );
                timestamp = zonedDateTime.toEpochSecond();
            }

            Matcher block = BLOCK.matcher(strippedText);
            Matcher item = ITEM.matcher(strippedText);
            Matcher container = CONTAINER.matcher(strippedText);

            ChangeType change;
            String sourceName;

            if (block.find()) {
                change = matchChangeType(block.group(1));
                sourceName = block.group(2);
                String blockId = block.group(4);

                return Optional.of(new LookupMessage.BlockAction(
                        timestamp,
                        change,
                        sourceName,
                        blockId
                ));
            } else if (item.find()) {
                change = matchChangeType(item.group(1));
                sourceName = item.group(2);
                int count = Integer.parseInt(item.group(4));
                String itemId = item.group(5);

                return Optional.of(new LookupMessage.ItemAction(
                        timestamp,
                        change,
                        sourceName,
                        count,
                        itemId
                ));
            } else if (container.find()) {
                change = matchChangeType(container.group(1));
                sourceName = container.group(2);
                int count = Integer.parseInt(container.group(4));
                String itemId = container.group(5);

                return Optional.of(new LookupMessage.ContainerAction(
                        timestamp,
                        change,
                        sourceName,
                        count,
                        itemId
                ));
            }
        }

        Matcher location = LOCATION.matcher(strippedText);
        if (location.find()) {
            int x = Integer.parseInt(location.group(1));
            int y = Integer.parseInt(location.group(2));
            int z = Integer.parseInt(location.group(3));
            String worldId = location.group(4);
            ActionType action = matchActionType(location.group(5));

            return Optional.of(new LookupMessage.Location(
                    x,
                    y,
                    z,
                    worldId,
                    action
            ));
        }

        return Optional.empty();
    }

    private ChangeType matchChangeType(String s) throws IllegalStateException {
        return switch(s) {
            case "+" -> ChangeType.ADD;
            case "-" -> ChangeType.REMOVE;
            default -> throw new IllegalStateException("Unexpected value: " + s);
        };
    }

    private ActionType matchActionType(String s) throws IllegalStateException {
        return switch(s) {
            case "block" -> ActionType.BLOCK;
            case "item" -> ActionType.ITEM;
            case "container" -> ActionType.CONTAINER;
            case null -> ActionType.UNKNOWN;
            default -> throw new IllegalStateException("Unexpected value: " + s);
        };
    }

    private String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private String fixDate(String date) {
        if (date.contains("BST")) date = date.replace("BST", "Europe/London");
        return date;
    }
}
