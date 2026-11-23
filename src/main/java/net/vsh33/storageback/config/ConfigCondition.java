package net.vsh33.storageback.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;
import net.vsh33.storageback.StorageBack;

/**
 * Resource condition {@code storageback:config} used in {@code fabric:load_conditions} of recipes and their recipe-book
 * advancements, e.g. {@code {"condition": "storageback:config", "option": "balancing_changes"}}.
 * Evaluated server-side every time data packs are loaded, so it works the same on dedicated servers.
 */
public record ConfigCondition(String option) implements ResourceCondition {
	public static final String BALANCING_CHANGES = "balancing_changes";
	public static final String BACK_RECIPES = "back_recipes";

	public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("option").forGetter(ConfigCondition::option)
	).apply(instance, ConfigCondition::new));
	public static final ResourceConditionType<ConfigCondition> TYPE = ResourceConditionType.create(StorageBack.id("config"), CODEC);

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(HolderLookup.Provider registryLookup) {
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
}
