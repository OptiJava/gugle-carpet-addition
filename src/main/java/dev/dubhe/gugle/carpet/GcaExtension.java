package dev.dubhe.gugle.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.gugle.carpet.api.Function;
import dev.dubhe.gugle.carpet.api.tools.text.ComponentTranslate;
import dev.dubhe.gugle.carpet.tools.FakePlayerEnderChestContainer;
import dev.dubhe.gugle.carpet.tools.FakePlayerInventoryContainer;
import dev.dubhe.gugle.carpet.tools.FakePlayerResident;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class GcaExtension implements CarpetExtension, ModInitializer {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static String MOD_ID = "gca";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static @NotNull ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static final HashMap<Player, Map.Entry<FakePlayerInventoryContainer, FakePlayerEnderChestContainer>> fakePlayerInventoryContainerMap = new HashMap<>();

    public static final List<Map.Entry<Long, Function>> planFunction = new ArrayList<>();

    static {
        CarpetServer.manageExtension(new GcaExtension());
    }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        GcaExtension.fakePlayerInventoryContainerMap.put(player, Map.entry(
            new FakePlayerInventoryContainer(player), new FakePlayerEnderChestContainer(player)
        ));

        if (GcaSetting.fakePlayerResident) {
            JsonObject fakePlayerList = new JsonObject();
            fakePlayerInventoryContainerMap.forEach((vplayer, fakePlayerInventoryContainer) -> {
                if (!(vplayer instanceof EntityPlayerMPFake)) return;
                String username = vplayer.getName().getString();
                fakePlayerList.add(username, FakePlayerResident.save(vplayer));
            });
            File file = CarpetServer.minecraft_server.getWorldPath(LevelResource.ROOT).resolve("fake_player.gca.json").toFile();
            if (!file.isFile()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try (BufferedWriter bfw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bfw.write(new Gson().toJson(fakePlayerList));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPlayerLoggedOut(ServerPlayer player) {
        GcaExtension.fakePlayerInventoryContainerMap.remove(player);
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(GcaSetting.class);
    }

    public static void onServerStop(MinecraftServer server) {
        if (GcaSetting.fakePlayerResident) {
            JsonObject fakePlayerList = new JsonObject();
            fakePlayerInventoryContainerMap.forEach((player, fakePlayerInventoryContainer) -> {
                if (!(player instanceof EntityPlayerMPFake)) return;
                String username = player.getName().getString();
                fakePlayerList.add(username, FakePlayerResident.save(player));
            });
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.gca.json").toFile();
            if (!file.isFile()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    GcaExtension.LOGGER.error(e.getMessage(), e);
                }
            }
            try (BufferedWriter bfw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                bfw.write(GSON.toJson(fakePlayerList));
            } catch (IOException e) {
                GcaExtension.LOGGER.error(e.getMessage(), e);
            }
        }
        fakePlayerInventoryContainerMap.clear();
    }

    public static void onServerStart(MinecraftServer server) {
        if (GcaSetting.fakePlayerResident) {
            JsonObject fakePlayerList = new JsonObject();
            File file = server.getWorldPath(LevelResource.ROOT).resolve("fake_player.gca.json").toFile();
            if (!file.isFile()) {
                return;
            }
            try (BufferedReader bfr = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                fakePlayerList = GSON.fromJson(bfr, JsonObject.class);
            } catch (IOException e) {
                GcaExtension.LOGGER.error(e.getMessage(), e);
            }
            for (Map.Entry<String, JsonElement> entry : fakePlayerList.entrySet()) {
                FakePlayerResident.load(entry, server);
            }
            file.delete();
        }
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return ComponentTranslate.getTranslations(lang);
    }

    @Override
    public void onInitialize() {
    }
}
