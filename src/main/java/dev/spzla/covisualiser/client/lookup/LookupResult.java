package dev.spzla.covisualiser.client.lookup;

public sealed interface LookupResult
    permits LookupResult.Block,
            LookupResult.Item,
            LookupResult.Container {
    long timestamp();
    ChangeType change();
    String sourceName();
    int x();
    int y();
    int z();
    String worldId();
    ActionType action();

    record Block(
            long timestamp,
            ChangeType change,
            String sourceName,
            String blockId,
            int x,
            int y,
            int z,
            String worldId,
            ActionType action
    ) implements LookupResult {}

    record Item(
            long timestamp,
            ChangeType change,
            String sourceName,
            int amount,
            String itemId,
            int x,
            int y,
            int z,
            String worldId,
            ActionType action
    ) implements LookupResult {}

    record Container(
            long timestamp,
            ChangeType change,
            String sourceName,
            int amount,
            String itemId,
            int x,
            int y,
            int z,
            String worldId,
            ActionType action
    ) implements LookupResult {}
}
