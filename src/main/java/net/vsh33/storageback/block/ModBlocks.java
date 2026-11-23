package net.vsh33.storageback.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.vsh33.storageback.StorageBack;

/** All storage blocks and their block items. Shown in a "Storage & Back" creative tab and in vanilla Building Blocks. */
public final class ModBlocks {
	public static final List<Block> BLOCKS = new ArrayList<>();

	public static final Block FLINT_BLOCK = register("flint_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.TUFF),
			p -> p);
	public static final Block SUGAR_CANE_BUNDLE = register("sugar_cane_bundle", RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instrument(NoteBlockInstrument.BANJO)
					.strength(0.5F).sound(SoundType.GRASS).ignitedByLava(),
			p -> p);
	public static final Block CHARCOAL_BLOCK = register("charcoal_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASEDRUM)
					.requiresCorrectToolForDrops().strength(5.0F, 6.0F),
			p -> p);
	public static final Block GUNPOWDER_BLOCK = register("gunpowder_block", GunpowderBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.SNARE)
					.strength(0.5F).sound(SoundType.SAND),
			p -> p);
	public static final Block BLAZE_ROD_BUNDLE = register("blaze_rod_bundle", RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BELL)
					.requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.BONE_BLOCK).lightLevel(state -> 5),
			p -> p);
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
					.isViewBlocking((state, getter, pos) -> false),
			p -> p);
	public static final Block ROTTEN_FLESH_BLOCK = register("rotten_flesh_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.DIDGERIDOO)
					.strength(0.5F).sound(SoundType.MUD),
			p -> p);
	public static final Block CARROT_CRATE = crate("carrot_crate", MapColor.COLOR_ORANGE);
	public static final Block POTATO_CRATE = crate("potato_crate", MapColor.SAND);
	public static final Block BEETROOT_CRATE = crate("beetroot_crate", MapColor.COLOR_RED);
	public static final Block APPLE_CRATE = crate("apple_crate", MapColor.COLOR_RED);

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, StorageBack.id("storageback"));

	private static Block crate(String name, MapColor color) {
		return register(name, Block::new,
				BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
						.strength(1.5F).sound(SoundType.WOOD).ignitedByLava(),
				p -> p);
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
			UnaryOperator<Item.Properties> itemProperties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, StorageBack.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, StorageBack.id(name));
		Item.Properties props = itemProperties.apply(new Item.Properties());
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, props));
		BLOCKS.add(block);
		return block;
	}

	public static void initialize() {
		// Fuel: Charcoal Block like a Block of Coal (80 items), Blaze Rod Bundle = 9 blaze rods (108 items).
		FuelRegistry.INSTANCE.add(CHARCOAL_BLOCK, 200 * 80);
		FuelRegistry.INSTANCE.add(BLAZE_ROD_BUNDLE, 200 * 108);
		// Composting chances: like a hay bale (85%) for plant bundles and crates, 65% for rotten flesh.
		CompostingChanceRegistry compost = CompostingChanceRegistry.INSTANCE;
		compost.add(SUGAR_CANE_BUNDLE, 0.85F);
		compost.add(ROTTEN_FLESH_BLOCK, 0.65F);
		for (Block crate : new Block[]{CARROT_CRATE, POTATO_CRATE, BEETROOT_CRATE, APPLE_CRATE}) {
			compost.add(crate, 0.85F);
		}

		// Same values as vanilla: hay bale, wool, coal block, planks.
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		flammable.add(SUGAR_CANE_BUNDLE, 60, 20);
		flammable.add(CHARCOAL_BLOCK, 5, 5);
		flammable.add(LEATHER_BLOCK, 30, 60);
		flammable.add(FEATHER_BLOCK, 30, 60);
		for (Block crate : new Block[]{CARROT_CRATE, POTATO_CRATE, BEETROOT_CRATE, APPLE_CRATE}) {
			flammable.add(crate, 5, 20);
		}

		CreativeModeTab tab = FabricItemGroup.builder()
				.icon(() -> new ItemStack(FLINT_BLOCK))
				.title(Component.translatable("itemGroup.storageback.storageback"))
				.displayItems((params, output) -> BLOCKS.forEach(output::accept))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.forEach(output::accept));
	}

	private ModBlocks() {
	}
}
