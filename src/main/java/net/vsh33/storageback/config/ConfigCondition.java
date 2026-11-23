package net.vsh33.storageback.config;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.vsh33.storageback.StorageBack;

/**
 * Resource condition {@code storageback:config} used in {@code fabric:load_conditions} of recipes and their recipe-book
 * advancements, e.g. {@code {"condition": "storageback:config", "option": "balancing_changes"}}.
 * Evaluated server-side every time data packs are loaded, so it works the same on dedicated servers.
 */
public final class ConfigCondition {
	public static final String BALANCING_CHANGES = "balancing_changes";
	public static final String BACK_RECIPES = "back_recipes";
	public static final ResourceLocation ID = StorageBack.id("config");

	public static boolean test(JsonObject json) {
		String option = GsonHelper.getAsString(json, "option");
		StorageBackConfig config = StorageBackConfig.get();
		return switch (option) {
			case BALANCING_CHANGES -> config.balancingChanges;
			case BACK_RECIPES -> config.backRecipes;
			default -> {
				StorageBack.LOGGER.warn("Unknown storageback:config option '{}'", option);
				yield true;
			}
		};
	}

	private ConfigCondition() {
	}
}
