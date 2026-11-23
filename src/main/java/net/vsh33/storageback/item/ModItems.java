package net.vsh33.storageback.item;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.vsh33.storageback.StorageBack;
import net.vsh33.storageback.block.ModBlocks;

/** Non-block items added by Storage &amp; Back. */
public final class ModItems {
	public static final Item GUIDE_BOOK = register("guide_book", GuideBookItem::new, props -> props.stacksTo(16));

	private static Item register(String name, Function<Item.Properties, Item> factory, UnaryOperator<Item.Properties> extra) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, StorageBack.id(name));
		Item.Properties props = extra.apply(new Item.Properties());
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props));
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ModBlocks.TAB_KEY).register(output -> output.accept(GUIDE_BOOK));
	}

	private ModItems() {
	}
}
