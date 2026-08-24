package dev.spzla.covisualiser.client.lookup;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LookupSession {
    private final LookupResultParser parser;
    private final LookupCommandSender commandSender;
    private final int pageSize = 100;

    private final List<LookupResult> results = new ArrayList<>();

    private State state = State.IDLE;
    private Completion completion;
    private LookupMessage.Action pendingAction;

    private int currentPage;
    private int remainingResults;
    private int resultsOnCurrentPage;

    public LookupSession(
            LookupResultParser parser,
            LookupCommandSender commandSender
    ) {
        this.parser = parser;
        this.commandSender = commandSender;
    }

    public void start() {
        if (state != State.IDLE && state != State.COMPLETE) {
            throw new IllegalStateException("Lookup is already active");
        }

        results.clear();
        pendingAction = null;
        currentPage = 0;
        remainingResults = 0;
        resultsOnCurrentPage = 0;
        completion = null;

        state = State.WAITING_FOR_COUNT;
    }

    public boolean handle(Component component, boolean inOverlay) {
        if (inOverlay || state == State.IDLE || state == State.COMPLETE) return true;

        Optional<LookupMessage> parsed = parser.parse(component);

        if (parsed.isEmpty()) {
            return true;
        }

        LookupMessage message = parsed.get();

        if (!canConsume(message)) {
            return true;
        }

        consume(message);
        return false;
    }

    private boolean canConsume(LookupMessage message) {
        return switch (message) {
            case LookupMessage.Fail _,
                 LookupMessage.Searching _,
                 LookupMessage.PageHeader _,
                 LookupMessage.PageInfo _ ->
                    true;

            case LookupMessage.Count _ ->
                    state == State.WAITING_FOR_COUNT;

            case LookupMessage.Action _ ->
                    state == State.WAITING_FOR_ACTION;

            case LookupMessage.Location _ ->
                    state == State.WAITING_FOR_LOCATION;
        };
    }

    private void consume(LookupMessage message) throws IllegalStateException {
        switch (message) {
            case LookupMessage.Count count ->
                    handleCount(count);

            case LookupMessage.Action action ->
                    handleAction(action);

            case LookupMessage.Location action ->
                    handleLocation(action);

            case LookupMessage.DatabaseBusy _ -> complete(Completion.DATABASE_BUSY);

            case LookupMessage.NoParams _ -> complete(Completion.NO_PARAMS);

            case LookupMessage.NoTime _ -> complete(Completion.NO_TIME);

            case LookupMessage.UserNotFound _ -> complete(Completion.USER_NOT_FOUND);

            case LookupMessage.NoResults _ -> complete(Completion.NO_RESULTS);

            case LookupMessage.Searching _,
                 LookupMessage.PageHeader _,
                 LookupMessage.PageInfo _ -> {}

            default -> throw new IllegalStateException("Unexpected value: " + message);
        }
    }

    private void handleCount(LookupMessage.Count count) {
        remainingResults = count.count();

        if (remainingResults == 0) {
            completion = Completion.NO_RESULTS;
            state = State.COMPLETE;
            return;
        }

        state = State.WAITING_FOR_ACTION;
        requestNextPage();
    }

    private void handleAction(LookupMessage.Action action) {
        pendingAction = action;
        state = State.WAITING_FOR_LOCATION;
    }

    private void handleLocation(LookupMessage.Location location) {
        if (pendingAction == null) {
            throw new IllegalStateException("pendingAction is null");
        }

        results.add(createResult(pendingAction, location));
        pendingAction = null;

        remainingResults--;
        resultsOnCurrentPage++;

        if (remainingResults == 0) {
            completion = Completion.SUCCESS;
            state = State.COMPLETE;
            return;
        }

        state = State.WAITING_FOR_ACTION;

        if (resultsOnCurrentPage == pageSize) {
            resultsOnCurrentPage = 0;
            requestNextPage();
        }
    }

    private LookupResult createResult(LookupMessage.Action action, LookupMessage.Location location) {
        return switch (action) {
            case LookupMessage.BlockAction block ->
                new LookupResult.Block(
                        block.timestamp(),
                        block.change(),
                        block.sourceName(),
                        block.blockId(),
                        location.x(),
                        location.y(),
                        location.z(),
                        location.worldId(),
                        location.action()
                );
            case LookupMessage.ItemAction item ->
                new LookupResult.Item(
                        item.timestamp(),
                        item.change(),
                        item.sourceName(),
                        item.amount(),
                        item.itemId(),
                        location.x(),
                        location.y(),
                        location.z(),
                        location.worldId(),
                        location.action()
                );
            case LookupMessage.ContainerAction container ->
                new LookupResult.Container(
                        container.timestamp(),
                        container.change(),
                        container.sourceName(),
                        container.amount(),
                        container.itemId(),
                        location.x(),
                        location.y(),
                        location.z(),
                        location.worldId(),
                        location.action()
                );
        };
    }

    private void complete(Completion completion) {
        this.completion = completion;
        state = State.COMPLETE;
    }

    private void requestNextPage() {
        commandSender.requestPage(++currentPage);
    }

    public boolean isComplete() {
        return state == State.COMPLETE;
    }

    public List<LookupResult> results() {
        return List.copyOf(results);
    }

    public Completion completion() {
        return completion;
    }

    private enum State {
        IDLE,
        WAITING_FOR_COUNT,
        WAITING_FOR_ACTION,
        WAITING_FOR_LOCATION,
        COMPLETE,
    }

    public enum Completion {
        SUCCESS,
        NO_RESULTS,
        NO_PARAMS,
        NO_TIME,
        USER_NOT_FOUND,
        DATABASE_BUSY,
        TIMEOUT
    }
}
