package net.vsh33.storageback.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.config.StorageBackConfig;
import net.vsh33.storageback.item.ModItems;

/**
 * Categories, entries and embedded recipe layouts for the guide book.
 * <p>
 * Minecraft 26.x does not sync the full {@code RecipeManager} to the client (only
 * recipe-book displays and property sets), so the grids are embedded here. They
 * match the recipe JSON in the data pack exactly.
 */
public final class GuideBookContents {
	public enum Section {
		INDEX,
		STORAGE_LIST,
		BACK_LIST,
		CONFIG,
		ENTRY
	}

	public record Location(Section section, String entryId) {
		public static Location index() {
			return new Location(Section.INDEX, null);
		}

		public static Location storageList() {
			return new Location(Section.STORAGE_LIST, null);
		}

		public static Location backList() {
			return new Location(Section.BACK_LIST, null);
		}

		public static Location config() {
			return new Location(Section.CONFIG, null);
		}

		public static Location entry(String id) {
			return new Location(Section.ENTRY, id);
		}
	}

	public record RecipeGrid(boolean shaped, List<ItemStack> slots, ItemStack result, boolean balancing, boolean backRecipe) {
		public boolean disabled() {
			StorageBackConfig config = StorageBackConfig.get();
			if (backRecipe && !config.backRecipes) {
				return true;
			}
			return balancing && !config.balancingChanges;
		}
	}

	public record Link(Component label, Location target) {
	}

	public record Entry(String id, ItemStack icon, Component title, Component body, List<RecipeGrid> recipes, List<Link> links) {
	}

	public record Category(String id, Location target, ItemStack icon, Component title) {
	}

	private final List<Category> categories;
	private final List<Entry> storageEntries;
	private final List<Entry> backEntries;
	private final List<Location> readingOrder;

	private GuideBookContents(List<Category> categories, List<Entry> storageEntries, List<Entry> backEntries, List<Location> readingOrder) {
		this.categories = categories;
		this.storageEntries = storageEntries;
		this.backEntries = backEntries;
		this.readingOrder = readingOrder;
	}

	public static GuideBookContents create() {
		List<Entry> storage = List.of(
				storage("flint_block", new ItemStack(ModBlocks.FLINT_BLOCK), Items.FLINT,
						List.of(new Link(Component.translatable("book.storageback.link.guide_recipe"), Location.index()))),
				storage("sugar_cane_bundle", new ItemStack(ModBlocks.SUGAR_CANE_BUNDLE), Items.SUGAR_CANE, List.of()),
				storage("charcoal_block", new ItemStack(ModBlocks.CHARCOAL_BLOCK), Items.CHARCOAL, List.of()),
				storage("gunpowder_block", new ItemStack(ModBlocks.GUNPOWDER_BLOCK), Items.GUNPOWDER, List.of()),
				storage("blaze_rod_bundle", new ItemStack(ModBlocks.BLAZE_ROD_BUNDLE), Items.BLAZE_ROD, List.of()),
				storage("leather_block", new ItemStack(ModBlocks.LEATHER_BLOCK), Items.LEATHER, List.of()),
				storage("feather_block", new ItemStack(ModBlocks.FEATHER_BLOCK), Items.FEATHER, List.of()),
				storage("ender_pearl_block", new ItemStack(ModBlocks.ENDER_PEARL_BLOCK), Items.ENDER_PEARL, List.of()),
				storage("rotten_flesh_block", new ItemStack(ModBlocks.ROTTEN_FLESH_BLOCK), Items.ROTTEN_FLESH, List.of()),
				storage("carrot_crate", new ItemStack(ModBlocks.CARROT_CRATE), Items.CARROT, List.of()),
				storage("potato_crate", new ItemStack(ModBlocks.POTATO_CRATE), Items.POTATO, List.of()),
				storage("beetroot_crate", new ItemStack(ModBlocks.BEETROOT_CRATE), Items.BEETROOT, List.of()),
				storage("apple_crate", new ItemStack(ModBlocks.APPLE_CRATE), Items.APPLE, List.of()));

		List<Entry> back = List.of(
				back("quartz", Items.QUARTZ_BLOCK, Items.QUARTZ, 4, false, "quartz"),
				back("amethyst", Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, 4, true, "amethyst"),
				back("clay", Items.CLAY, Items.CLAY_BALL, 4, false, "clay"),
				back("glowstone", Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4, false, "glowstone"),
				back("snow", Items.SNOW_BLOCK, Items.SNOWBALL, 4, false, "snow"),
				back("honeycomb", Items.HONEYCOMB_BLOCK, Items.HONEYCOMB, 4, false, "honeycomb"),
				back("bricks", Items.BRICKS, Items.BRICK, 4, false, "bricks"),
				back("nether_bricks", Items.NETHER_BRICKS, Items.NETHER_BRICK, 4, false, "nether_bricks"),
				back("dripstone", Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, 4, false, "dripstone"),
				back("prismarine", Items.PRISMARINE, Items.PRISMARINE_SHARD, 4, false, "prismarine"),
				back("prismarine_bricks", Items.PRISMARINE_BRICKS, Items.PRISMARINE_SHARD, 9, false, "prismarine_bricks"),
				back("dark_prismarine", Items.DARK_PRISMARINE, Items.PRISMARINE_SHARD, 8, false, "dark_prismarine"),
				back("magma", Items.MAGMA_BLOCK, Items.MAGMA_CREAM, 4, true, "magma"),
				back("sandstone", Items.SANDSTONE, Items.SAND, 4, false, "sandstone"),
				back("red_sandstone", Items.RED_SANDSTONE, Items.RED_SAND, 4, false, "red_sandstone"));

		List<Category> categories = List.of(
				new Category("storage", Location.storageList(), new ItemStack(ModBlocks.FLINT_BLOCK),
						Component.translatable("book.storageback.category.storage")),
				new Category("back", Location.backList(), new ItemStack(Items.QUARTZ_BLOCK),
						Component.translatable("book.storageback.category.back")),
				new Category("config", Location.config(), new ItemStack(Items.REDSTONE),
						Component.translatable("book.storageback.category.config")));

		List<Location> order = new ArrayList<>();
		order.add(Location.index());
		order.add(Location.storageList());
		for (Entry entry : storage) {
			order.add(Location.entry(entry.id()));
		}
		order.add(Location.backList());
		for (Entry entry : back) {
			order.add(Location.entry(entry.id()));
		}
		order.add(Location.config());

		return new GuideBookContents(categories, storage, back, List.copyOf(order));
	}

	public List<Category> categories() {
		return categories;
	}

	public List<Entry> storageEntries() {
		return storageEntries;
	}

	public List<Entry> backEntries() {
		return backEntries;
	}

	public List<Location> readingOrder() {
		return readingOrder;
	}

	public RecipeGrid guideBookRecipe() {
		return shapeless(new ItemStack(ModItems.GUIDE_BOOK), false, false, new ItemStack(Items.BOOK), new ItemStack(ModBlocks.FLINT_BLOCK));
	}

	public Entry entry(String id) {
		for (Entry entry : storageEntries) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		for (Entry entry : backEntries) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	public int indexOf(Location location) {
		for (int i = 0; i < readingOrder.size(); i++) {
			if (same(readingOrder.get(i), location)) {
				return i;
			}
		}
		return 0;
	}

	public static boolean same(Location a, Location b) {
		if (a.section() != b.section()) {
			return false;
		}
		if (a.entryId() == null) {
			return b.entryId() == null;
		}
		return a.entryId().equals(b.entryId());
	}

	private static Entry storage(String id, ItemStack block, net.minecraft.world.item.Item ingredient, List<Link> links) {
		ItemStack nine = new ItemStack(ingredient, 1);
		RecipeGrid pack = shaped9(nine, new ItemStack(block.getItem()));
		RecipeGrid unpack = shapeless(new ItemStack(ingredient, 9), false, false, new ItemStack(block.getItem()));
		return new Entry(
				id,
				new ItemStack(block.getItem()),
				block.getHoverName(),
				Component.translatable("book.storageback.page." + id),
				List.of(pack, unpack),
				links);
	}

	private static Entry back(String id, net.minecraft.world.item.Item block, net.minecraft.world.item.Item result, int count, boolean balancing, String bodyId) {
		ItemStack icon = new ItemStack(block);
		RecipeGrid grid = shapeless(new ItemStack(result, count), balancing, true, new ItemStack(block));
		return new Entry(
				id,
				icon,
				icon.getHoverName(),
				Component.translatable("book.storageback.page.back." + bodyId),
				List.of(grid),
				List.of());
	}

	private static RecipeGrid shaped9(ItemStack ingredient, ItemStack result) {
		List<ItemStack> slots = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			slots.add(ingredient.copy());
		}
		return new RecipeGrid(true, List.copyOf(slots), result, false, false);
	}

	private static RecipeGrid shapeless(ItemStack result, boolean balancing, boolean backRecipe, ItemStack... ingredients) {
		List<ItemStack> slots = emptySlots();
		for (int i = 0; i < ingredients.length && i < 9; i++) {
			slots.set(i, ingredients[i]);
		}
		return new RecipeGrid(false, List.copyOf(slots), result, balancing, backRecipe);
	}

	private static List<ItemStack> emptySlots() {
		List<ItemStack> slots = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			slots.add(ItemStack.EMPTY);
		}
		return slots;
	}

}
