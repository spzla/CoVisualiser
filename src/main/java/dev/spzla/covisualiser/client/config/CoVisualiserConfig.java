package dev.spzla.covisualiser.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.spzla.covisualiser.client.CoVisualiserClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoVisualiserConfig {
    public static final CoVisualiserConfig INSTANCE = new CoVisualiserConfig();

    public final Path configFile = FabricLoader.getInstance().getConfigDir().resolve(CoVisualiserClient.MOD_ID + ".json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean closeUiOnTeleport = true;

    public void save() {
        try {
            Files.deleteIfExists(configFile);

            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("closeUiOnTeleport", closeUiOnTeleport);

            Files.writeString(configFile, gson.toJson(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            if (Files.notExists(configFile)) {
                save();
                return;
            }

            JsonObject json = gson.fromJson(Files.readString(configFile), JsonObject.class);
            if (json.has("enabled"))
                enabled = json.getAsJsonPrimitive("enabled").getAsBoolean();
            if (json.has("closeUiOnTeleport"))
                closeUiOnTeleport = json.getAsJsonPrimitive("closeUiOnTeleport").getAsBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Screen makeScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("covisualiser.general.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("covisualiser.config.general.title"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("covisualiser.config.option.enabled"))
                                .description(OptionDescription.of(Text.translatable("covisualiser.config.option.enabled.description")))
                                .binding(
                                        true,
                                        () -> enabled,
                                        value -> enabled = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("covisualiser.config.option.closeuionteleport"))
                                .description(OptionDescription.of(Text.translatable("covisualiser.config.option.closeuionteleport.description")))
                                .binding(
                                        true,
                                        () -> closeUiOnTeleport,
                                        value -> closeUiOnTeleport = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }
}
