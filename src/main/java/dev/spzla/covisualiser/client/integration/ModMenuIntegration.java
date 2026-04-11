package dev.spzla.covisualiser.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.spzla.covisualiser.client.CoVisualiserClient;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CoVisualiserClient.getConfig().makeScreen(parent);
    }
}
