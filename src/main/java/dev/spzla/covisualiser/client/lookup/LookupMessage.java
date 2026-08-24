package dev.spzla.covisualiser.client.lookup;

public sealed interface LookupMessage
    permits LookupMessage.Count,
            LookupMessage.Action,
            LookupMessage.Fail,
            LookupMessage.Location,
            LookupMessage.Searching,
            LookupMessage.PageHeader,
            LookupMessage.PageInfo
{

    record Count(int count) implements LookupMessage {}

    sealed interface Action extends LookupMessage
        permits BlockAction, ItemAction, ContainerAction {}

    sealed interface Fail extends LookupMessage
        permits DatabaseBusy, NoResults, NoTime, NoParams, UserNotFound {}

    record BlockAction(
            long timestamp,
            ChangeType change,
            String sourceName,
            String blockId
    ) implements Action {}

    record ItemAction(
            long timestamp,
            ChangeType change,
            String sourceName,
            int amount,
            String itemId
    ) implements Action {}

    record ContainerAction(
            long timestamp,
            ChangeType change,
            String sourceName,
            int amount,
            String itemId
    ) implements Action {}

    record Location(
            int x,
            int y,
            int z,
            String worldId,
            ActionType action
    ) implements LookupMessage {}

    record DatabaseBusy() implements Fail {}
    record Searching() implements LookupMessage {}
    record PageHeader() implements LookupMessage {}
    record PageInfo() implements LookupMessage {}
    record NoResults() implements Fail {}
    record NoParams() implements Fail {}
    record NoTime() implements Fail {}
    record UserNotFound(String name) implements Fail {}
}
