package dev.spzla.covisualiser.client.lookup;

@FunctionalInterface
public interface LookupCommandSender {
    void requestPage(int page);
}
