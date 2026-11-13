package net.vsh33.storageback.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.vsh33.storageback.StorageBack;

/** All storage blocks and their block items. Shown in a "Storage & Back" creative tab and in vanilla Building Blocks. */
public final class ModBlocks {
	public static final List<Block> BLOCKS = new ArrayList<>();

	/** 26.3: fuel values are data-driven ({@code data/storageback/context_int_provider/cooking/*.json}). */
	public static final ResourceKey<ContextIntProvider> COOKING_TIME_BLAZE_ROD_BUNDLE =
			ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, StorageBack.id("cooking/time_blaze_rod_bundle"));

	public static final Block FLINT_BLOCK = register("flint_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.TUFF),
			p -> p);
	public static final Block SUGAR_CANE_BUNDLE = register("sugar_cane_bundle", RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instrument(NoteBlockInstrument.BANJO)
					.strength(0.5F).sound(SoundType.GRASS).ignitedByLava(),
			p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH));
	public static final Block CHARCOAL_BLOCK = register("charcoal_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().strength(5.0F, 6.0F),
			p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_COAL_BLOCK));
	public static final Block GUNPOWDER_BLOCK = register("gunpowder_block", p -> new ColoredFallingBlock(new ColorRGBA(0xFF5A5A5A), p),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.SNARE)
					.strength(0.5F).sound(SoundType.SAND),
			p -> p);
	public static final Block BLAZE_ROD_BUNDLE = register("blaze_rod_bundle", RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BELL)
					.requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.BONE_BLOCK).lightLevel(state -> 5),
			p -> p.cookingFuel(COOKING_TIME_BLAZE_ROD_BUNDLE));
	public static final Block LEATHER_BLOCK = register("leather_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.GUITAR)
					.strength(0.8F).sound(SoundType.WOOL).ignitedByLava(),
			p -> p);
	public static final Block FEATHER_BLOCK = register("feather_block", SoftLandingBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.FLUTE)
					.strength(0.5F).sound(SoundType.WOOL).ignitedByLava(),
			p -> p);
	public static final Block ENDER_PEARL_BLOCK = register("ender_pearl_block", EnderPearlBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.CHIME)
					.strength(1.5F, 6.0F).sound(SoundType.AMETHYST)
					.noOcclusion().forceSolidOff()
					.isValidSpawn((state, getter, pos, type) -> false)
					.isRedstoneConductor((state, getter, pos) -> false)
					.isSuffocating((state, getter, pos) -> false)
					.isViewBlocking((state, getter, pos, box) -> false),
			p -> p);
	public static final Block ROTTEN_FLESH_BLOCK = register("rotten_flesh_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.DIDGERIDOO)
					.strength(0.5F).sound(SoundType.MUD),
			p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
	public static final Block CARROT_CRATE = crate("carrot_crate", MapColor.COLOR_ORANGE);
	public static final Block POTATO_CRATE = crate("potato_crate", MapColor.SAND);
	public static final Block BEETROOT_CRATE = crate("beetroot_crate", MapColor.COLOR_RED);
	public static final Block APPLE_CRATE = crate("apple_crate", MapColor.COLOR_RED);

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, StorageBack.id("storageback"));

	private static Block crate(String name, MapColor color) {
		return register(name, Block::new,
				BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
						.strength(1.5F).sound(SoundType.WOOD).ignitedByLava(),
				p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH));
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
			UnaryOperator<Item.Properties> itemProperties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, StorageBack.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, StorageBack.id(name));
		Item.Properties props = itemProperties.apply(new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, props));
		BLOCKS.add(block);
		return block;
	}

	public static void initialize() {
		// Same values as vanilla: hay bale, wool, coal block, planks.
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		flammable.add(SUGAR_CANE_BUNDLE, 60, 20);
		flammable.add(CHARCOAL_BLOCK, 5, 5);
		flammable.add(LEATHER_BLOCK, 30, 60);
		flammable.add(FEATHER_BLOCK, 30, 60);
		for (Block crate : new Block[]{CARROT_CRATE, POTATO_CRATE, BEETROOT_CRATE, APPLE_CRATE}) {
			flammable.add(crate, 5, 20);
		}

		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(FLINT_BLOCK))
				.title(Component.translatable("itemGroup.storageback.storageback"))
				.displayItems((params, output) -> BLOCKS.forEach(output::accept))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.forEach(output::accept));
	}

	private ModBlocks() {
	}
}
