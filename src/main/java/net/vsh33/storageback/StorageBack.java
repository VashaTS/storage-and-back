package net.vsh33.storageback;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.Identifier;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.item.ModItems;
import net.vsh33.storageback.config.ConfigCondition;
import net.vsh33.storageback.config.StorageBackConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage &amp; Back: storage blocks for items that have none, and "back" recipes that turn vanilla blocks into the items
 * they are crafted from.
 */
public class StorageBack implements ModInitializer {
	public static final String MOD_ID = "storageback";
	public static final Logger LOGGER = LoggerFactory.getLogger("Storage & Back");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		StorageBackConfig.load();
		ResourceConditions.register(ConfigCondition.TYPE);
		ModBlocks.initialize();
		ModItems.initialize();
	}
}
