package net.vsh33.storageback.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vsh33.storageback.config.StorageBackConfig;

/** Cloth Config screen for {@link StorageBackConfig}. Only loaded when Cloth Config is present. */
public final class StorageBackConfigScreen {
	public static Screen create(Screen parent) {
		StorageBackConfig config = StorageBackConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.storageback.title"))
				.setSavingRunnable(StorageBackConfig::save);
		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory recipes = builder.getOrCreateCategory(Component.translatable("config.storageback.category.recipes"));

		recipes.addEntry(entries.startBooleanToggle(Component.translatable("config.storageback.balancing_changes"), config.balancingChanges)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.storageback.balancing_changes.tooltip"),
						Component.translatable("config.storageback.reload_note"))
				.setSaveConsumer(value -> config.balancingChanges = value)
				.build());
		recipes.addEntry(entries.startBooleanToggle(Component.translatable("config.storageback.back_recipes"), config.backRecipes)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.storageback.back_recipes.tooltip"),
						Component.translatable("config.storageback.reload_note"))
				.setSaveConsumer(value -> config.backRecipes = value)
				.build());
		return builder.build();
	}

	private StorageBackConfigScreen() {
	}
}
