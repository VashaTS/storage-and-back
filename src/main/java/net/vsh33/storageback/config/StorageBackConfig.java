package net.vsh33.storageback.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.vsh33.storageback.StorageBack;

/**
 * Settings stored in {@code config/storageback.json}. Recipes are data, so the values are read by {@link ConfigCondition}
 * whenever the server loads its data packs: on world load / server start and on {@code /reload}.
 * On a dedicated server the server's own file is the one that counts.
 */
public final class StorageBackConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path file;
	private static StorageBackConfig instance = new StorageBackConfig();

	/** Recipes that change vanilla balance: Amethyst Block -> 4 Amethyst Shards and Magma Block -> 4 Magma Cream. */
	public boolean balancingChanges = true;
	/** All recipes that turn a vanilla block back into its items (Quartz Block -> 4 Nether Quartz, ...). */
	public boolean backRecipes = true;

	public static StorageBackConfig get() {
		return instance;
	}

	public static Path file() {
		if (file == null) {
			file = FabricLoader.getInstance().getConfigDir().resolve("storageback.json");
		}
		return file;
	}

	public static void load() {
		StorageBackConfig loaded = null;
		Path path = file();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				loaded = GSON.fromJson(reader, StorageBackConfig.class);
			} catch (Exception e) {
				StorageBack.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}
		instance = loaded != null ? loaded : new StorageBackConfig();
		save();
	}

	public static void save() {
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			StorageBack.LOGGER.error("Could not write {}", path, e);
		}
	}
}
