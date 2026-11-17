package net.vsh33.storageback.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.vsh33.storageback.StorageBack;
import net.vsh33.storageback.block.EnderPearlBlock;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.item.ModItems;
import net.vsh33.storageback.config.StorageBackConfig;

/** Server game tests (./gradlew runGameTest, also run by ./gradlew build). */
public class StorageBackServerTest {
	/** block id, ingredient item */
	static final String[][] STORAGE = {
			{"flint_block", "flint"}, {"sugar_cane_bundle", "sugar_cane"}, {"charcoal_block", "charcoal"},
			{"gunpowder_block", "gunpowder"}, {"blaze_rod_bundle", "blaze_rod"}, {"leather_block", "leather"},
			{"feather_block", "feather"}, {"ender_pearl_block", "ender_pearl"}, {"rotten_flesh_block", "rotten_flesh"},
			{"carrot_crate", "carrot"}, {"potato_crate", "potato"}, {"beetroot_crate", "beetroot"}, {"apple_crate", "apple"},
	};
	/** vanilla block, result item, count */
	static final String[][] BACK = {
			{"quartz_block", "quartz", "4"}, {"amethyst_block", "amethyst_shard", "4"}, {"clay", "clay_ball", "4"},
			{"glowstone", "glowstone_dust", "4"}, {"snow_block", "snowball", "4"}, {"honeycomb_block", "honeycomb", "4"},
			{"bricks", "brick", "4"}, {"nether_bricks", "nether_brick", "4"}, {"dripstone_block", "pointed_dripstone", "4"},
			{"prismarine", "prismarine_shard", "4"}, {"prismarine_bricks", "prismarine_shard", "9"},
			{"dark_prismarine", "prismarine_shard", "8"}, {"magma_block", "magma_cream", "4"},
			{"sandstone", "sand", "4"}, {"red_sandstone", "red_sand", "4"},
	};
	static final List<String> BALANCING = List.of("amethyst_shard_from_amethyst_block", "magma_cream_from_magma_block");

	static Item item(String id) {
		Identifier key = id.contains(":") ? Identifier.parse(id) : Identifier.withDefaultNamespace(id);
		return BuiltInRegistries.ITEM.getValue(key);
	}

	static boolean hasRecipe(MinecraftServer server, String path) {
		return server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, StorageBack.id(path))).isPresent();
	}

	/** Every recipe of this mod, with the grid that crafts it and the expected result. */
	record Case(String id, CraftingInput input, Item result, int count) {
	}

	static List<Case> cases() {
		List<Case> cases = new ArrayList<>();
		for (String[] s : STORAGE) {
			Item block = item(StorageBack.MOD_ID + ":" + s[0]);
			Item ing = item(s[1]);
			cases.add(new Case(s[0], CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(ing))), block, 1));
			cases.add(new Case(s[1] + "_from_" + s[0], CraftingInput.of(1, 1, List.of(new ItemStack(block))), ing, 9));
		}
		for (String[] b : BACK) {
			cases.add(new Case(b[1] + "_from_" + b[0], CraftingInput.of(1, 1, List.of(new ItemStack(item(b[0])))), item(b[1]), Integer.parseInt(b[2])));
		}
		cases.add(new Case("guide_book", CraftingInput.of(1, 2, List.of(new ItemStack(Items.BOOK), new ItemStack(ModBlocks.FLINT_BLOCK))), ModItems.GUIDE_BOOK, 1));
		return cases;
	}

	@GameTest
	public void recipesAndAdvancementsLoad(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		for (Case c : cases()) {
			helper.assertTrue(hasRecipe(server, c.id()), "missing recipe storageback:" + c.id());
			String folder = (c.id().contains("_from_") || c.id().equals("guide_book")) ? "misc" : "building_blocks";
			helper.assertTrue(server.getAdvancements().get(StorageBack.id("recipes/" + folder + "/" + c.id())) != null,
					"missing recipe advancement for " + c.id());
		}
		helper.assertTrue(cases().size() == 42, "expected 42 recipes, got " + cases().size());
		StorageBack.LOGGER.info("[test] all {} recipes and their advancements are loaded", cases().size());
		helper.succeed();
	}

	/** Every mod recipe is the ONLY crafting recipe (vanilla or mod) matching its grid, and gives the expected result. */
	@GameTest
	public void noRecipeConflicts(GameTestHelper helper) {
		var level = helper.getLevel();
		List<RecipeHolder<?>> crafting = level.getServer().getRecipeManager().getRecipes().stream()
				.filter(h -> h.value().getType() == RecipeType.CRAFTING).toList();
		for (Case c : cases()) {
			List<String> matches = new ArrayList<>();
			for (RecipeHolder<?> holder : crafting) {
				@SuppressWarnings("unchecked")
				Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) holder.value();
				if (recipe.matches(c.input(), level)) {
					matches.add(holder.id().identifier().toString());
				}
			}
			helper.assertTrue(matches.equals(List.of("storageback:" + c.id())), "grid for " + c.id() + " matches " + matches);
			@SuppressWarnings("unchecked")
			CraftingRecipe recipe = (CraftingRecipe) level.getServer().getRecipeManager()
					.byKey(ResourceKey.create(Registries.RECIPE, StorageBack.id(c.id()))).orElseThrow().value();
			ItemStack out = recipe.assemble(c.input());
			helper.assertTrue(out.is(c.result()) && out.getCount() == c.count(),
					c.id() + " should give " + c.count() + " " + c.result() + ", got " + out);
		}
		StorageBack.LOGGER.info("[test] {} recipes checked against {} crafting recipes: no conflicts", cases().size(), crafting.size());
		helper.succeed();
	}

	/** Turning "Balancing changes" off and reloading data removes exactly the amethyst and magma cream recipes. */
	@GameTest(maxTicks = 600)
	public void balancingToggleRemovesRecipes(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		StorageBackConfig config = StorageBackConfig.get();
		BALANCING.forEach(id -> helper.assertTrue(hasRecipe(server, id), "baseline: " + id + " should exist"));
		config.balancingChanges = false;
		CompletableFuture<Void> off = server.reloadResources(server.getPackRepository().getSelectedIds());
		final CompletableFuture<?>[] on = {null};
		helper.succeedWhen(() -> {
			if (on[0] == null) {
				helper.assertTrue(off.isDone(), "waiting for reload");
				for (String id : BALANCING) {
					helper.assertFalse(hasRecipe(server, id), id + " must be gone when balancing changes are off");
					helper.assertTrue(server.getAdvancements().get(StorageBack.id("recipes/misc/" + id)) == null, "advancement for " + id + " must be gone");
				}
				for (Case c : cases()) {
					if (!BALANCING.contains(c.id())) {
						helper.assertTrue(hasRecipe(server, c.id()), c.id() + " must stay");
					}
				}
				StorageBack.LOGGER.info("[test] balancing changes off: amethyst + magma cream recipes removed, other 40 kept");
				config.balancingChanges = true;
				on[0] = server.reloadResources(server.getPackRepository().getSelectedIds());
				helper.fail("reloading again");
			}
			helper.assertTrue(on[0].isDone(), "waiting for second reload");
			BALANCING.forEach(id -> helper.assertTrue(hasRecipe(server, id), id + " must be back after re-enabling"));
		});
	}

	@GameTest
	public void blockProperties(GameTestHelper helper) {
		helper.assertTrue(ModBlocks.BLOCKS.size() == 13, "13 blocks expected, got " + ModBlocks.BLOCKS.size());
		var flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block b : new Block[]{ModBlocks.SUGAR_CANE_BUNDLE, ModBlocks.CHARCOAL_BLOCK, ModBlocks.LEATHER_BLOCK, ModBlocks.FEATHER_BLOCK,
				ModBlocks.CARROT_CRATE, ModBlocks.POTATO_CRATE, ModBlocks.BEETROOT_CRATE, ModBlocks.APPLE_CRATE}) {
			helper.assertTrue(flammable.get(b) != null && flammable.get(b).getBurnOdds() > 0, b + " should be flammable");
		}
		helper.assertTrue(flammable.get(ModBlocks.FLINT_BLOCK) == null || flammable.get(ModBlocks.FLINT_BLOCK).getBurnOdds() == 0, "flint must not burn");
		for (Block b : new Block[]{ModBlocks.SUGAR_CANE_BUNDLE, ModBlocks.ROTTEN_FLESH_BLOCK, ModBlocks.CARROT_CRATE, ModBlocks.POTATO_CRATE,
				ModBlocks.BEETROOT_CRATE, ModBlocks.APPLE_CRATE}) {
			Float chance = CompostableRegistry.INSTANCE.get(b);
			helper.assertTrue(chance != null && chance > 0, b + " should be compostable");
		}
		var fuel = helper.getLevel().fuelValues();
		helper.assertTrue(fuel.burnDuration(new ItemStack(ModBlocks.CHARCOAL_BLOCK)) == 16000, "charcoal block burn time");
		helper.assertTrue(fuel.burnDuration(new ItemStack(ModBlocks.BLAZE_ROD_BUNDLE)) == 21600, "blaze rod bundle burn time");
		helper.assertTrue(ModBlocks.BLAZE_ROD_BUNDLE.defaultBlockState().getLightEmission() > 0, "blaze rod bundle should glow");
		var pearl = ModBlocks.ENDER_PEARL_BLOCK.defaultBlockState();
		helper.assertFalse(Block.isShapeFullBlock(EnderPearlBlock.SHAPE), "ender pearl block must not be a full cube");
		helper.assertTrue(pearl.propagatesSkylightDown(), "light should pass through the ender pearl block");
		helper.assertFalse(pearl.canOcclude(), "ender pearl block must not occlude neighbours");
		helper.assertFalse(pearl.isSolidRender(), "ender pearl block must not be a solid render cube");
		helper.succeed();
	}

	/** Charcoal Block and Blaze Rod Bundle work as furnace fuel. */
	@GameTest(maxTicks = 40)
	public void fuel(GameTestHelper helper) {
		Block[] fuels = {ModBlocks.CHARCOAL_BLOCK, ModBlocks.BLAZE_ROD_BUNDLE};
		for (int i = 0; i < fuels.length; i++) {
			BlockPos pos = new BlockPos(1 + i * 2, 1, 1);
			helper.setBlock(pos, Blocks.FURNACE);
			FurnaceBlockEntity furnace = helper.getBlockEntity(pos, FurnaceBlockEntity.class);
			furnace.setItem(0, new ItemStack(Items.RAW_IRON));
			furnace.setItem(1, new ItemStack(fuels[i]));
		}
		helper.runAfterDelay(10, () -> {
			for (int i = 0; i < fuels.length; i++) {
				BlockPos pos = new BlockPos(1 + i * 2, 1, 1);
				helper.assertTrue(helper.getBlockState(pos).getValue(AbstractFurnaceBlock.LIT), fuels[i] + " should light the furnace");
				FurnaceBlockEntity furnace = helper.getBlockEntity(pos, FurnaceBlockEntity.class);
				helper.assertTrue(furnace.getItem(1).isEmpty(), fuels[i] + " should be consumed as fuel");
			}
			helper.succeed();
		});
	}

	/** The Gunpowder Block falls like sand. */
	@GameTest(maxTicks = 60)
	public void gunpowderFalls(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
		helper.setBlock(new BlockPos(1, 3, 1), Blocks.AIR);
		helper.setBlock(new BlockPos(1, 5, 1), ModBlocks.GUNPOWDER_BLOCK);
		helper.succeedWhenBlockPresent(ModBlocks.GUNPOWDER_BLOCK, new BlockPos(1, 2, 1));
	}
}
