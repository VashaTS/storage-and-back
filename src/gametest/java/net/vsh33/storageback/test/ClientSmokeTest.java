package net.vsh33.storageback.test;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.vsh33.storageback.StorageBack;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.client.GuideBookScreen;
import net.vsh33.storageback.item.ModItems;

/**
 * Client smoke test for the 1.21.1 / 1.20.1 backports (the Fabric client game test API only exists on 1.21.4+):
 * creates a world, checks that every recipe loaded, renders every block, crafts the guide book and opens the guide book
 * in English and Polish. Screenshots go to -Dstorageback.smokeShots. Runs only with -Dstorageback.smoke=true.
 */
public class ClientSmokeTest implements ClientModInitializer {
	private static final AtomicInteger TICKS = new AtomicInteger();
	private static final Path OUT = Path.of(System.getProperty("storageback.smokeShots", "build/smoke-shots"));
	private static final List<String> RESULTS = new ArrayList<>();
	private static final String[] ALL = {"flint_block", "sugar_cane_bundle", "charcoal_block", "gunpowder_block", "blaze_rod_bundle",
			"leather_block", "feather_block", "ender_pearl_block", "rotten_flesh_block", "carrot_crate", "potato_crate", "beetroot_crate", "apple_crate"};

	@Override
	public void onInitializeClient() {
		if (!Boolean.getBoolean("storageback.smoke")) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(mc -> TICKS.incrementAndGet());
		Thread thread = new Thread(() -> {
			try {
				run();
				RESULTS.add("ALL OK");
			} catch (Throwable t) {
				StorageBack.LOGGER.error("[smoke] FAILED", t);
				RESULTS.add("FAILED: " + t);
			}
			try {
				Files.createDirectories(OUT);
				Files.write(OUT.resolve("smoke-result.txt"), RESULTS);
			} catch (IOException ignored) {
			}
			StorageBack.LOGGER.info("[smoke] result: {}", RESULTS);
			Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
		}, "storageback-smoke");
		thread.setDaemon(true);
		thread.start();
	}

	// ---- helpers (test thread)

	private static void client(Consumer<Minecraft> action) {
		Minecraft mc = Minecraft.getInstance();
		mc.submit(() -> action.accept(mc)).join();
	}

	private static <T> T computeClient(Function<Minecraft, T> fn) {
		Minecraft mc = Minecraft.getInstance();
		return mc.submit(() -> fn.apply(mc)).join();
	}

	private static void server(Consumer<MinecraftServer> action) {
		MinecraftServer s = computeClient(Minecraft::getSingleplayerServer);
		s.submit(() -> action.accept(s)).join();
	}

	private static <T> T computeServer(Function<MinecraftServer, T> fn) {
		MinecraftServer s = computeClient(Minecraft::getSingleplayerServer);
		return s.submit(() -> fn.apply(s)).join();
	}

	private static void cmd(String command) {
		server(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), command));
	}

	private static void waitTicks(int n) {
		int target = TICKS.get() + n;
		while (TICKS.get() < target) {
			sleep(10);
		}
	}

	private static void waitFor(Predicate<Minecraft> cond, int maxTicks, String what) {
		int end = TICKS.get() + maxTicks;
		while (!computeClient(cond::test)) {
			if (TICKS.get() > end) throw new AssertionError("timed out waiting for " + what);
			sleep(50);
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void check(boolean ok, String what) {
		RESULTS.add((ok ? "PASS " : "FAIL ") + what);
		StorageBack.LOGGER.info("[smoke] {} {}", ok ? "PASS" : "FAIL", what);
		if (!ok) throw new AssertionError(what);
	}

	private static void shot(String name) {
		client(mc -> {
			mc.getToasts().clear();
			mc.gui.getChat().clearMessages(false);
		});
		waitTicks(3);
		client(mc -> {
			try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
				Files.createDirectories(OUT);
				image.writeToFile(OUT.resolve(name + ".png"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		RESULTS.add("shot " + name + ".png");
	}

	private static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().get(0);
	}

	private static void tp(double x, double y, double z, double fx, double fy, double fz) {
		cmd(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", x, y, z, fx, fy, fz));
		waitTicks(40);
	}

	private static void set(int x, int y, int z, String block) {
		cmd("setblock " + x + " " + y + " " + z + " storageback:" + block);
	}

	// ---- the test

	private static void run() {
		waitFor(mc -> mc.screen != null && mc.getOverlay() == null, 20 * 600, "title screen");
		waitTicks(20);
		client(mc -> {
			mc.options.pauseOnLostFocus = false;
			mc.options.onboardAccessibility = false;
			mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
			mc.getWindow().setWindowed(1280, 720);
			String name = "smoke" + System.currentTimeMillis();
			GameRules rules = new GameRules();
			rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
			rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
			rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
			mc.createWorldOpenFlows().createFreshLevel(name,
					new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
					new WorldOptions(20260101L, true, false),
					reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
		});
		waitFor(mc -> mc.level != null && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null, 20 * 600, "world");
		check(computeServer(s -> BuiltInRegistries.ITEM.containsKey(StorageBack.id("guide_book"))), "mod loaded: guide book registered");
		long recipes = computeServer(s -> s.getRecipeManager().getRecipes().stream().filter(r -> r.getId().getNamespace().equals(StorageBack.MOD_ID)).count());
		check(recipes == 43, "storageback recipes loaded: " + recipes + " / 43");
		long adv = computeServer(s -> s.getAdvancements().getAllAdvancements().stream().filter(a -> a.getId().getNamespace().equals(StorageBack.MOD_ID)).count());
		check(adv == 43, "storageback recipe advancements loaded: " + adv + " / 43");
		check(computeServer(s -> s.getRecipeManager().byKey(StorageBack.id("nether_wart_from_nether_wart_block")).isPresent()), "nether wart recipe loaded");

		cmd("time set 6000");
		cmd("weather clear");
		cmd("forceload add -80 -64 200 64");
		for (int x = -80; x < 200; x += 40) {
			cmd("fill " + x + " 200 -64 " + (x + 39) + " 200 64 minecraft:grass_block");
		}
		cmd("gamemode spectator @a");
		client(mc -> mc.options.hideGui = true);

		// 1. storage wall: every block, three high
		for (int i = 0; i < ALL.length; i++) {
			for (int y = 201; y <= 203; y++) {
				set(i, y, 0, ALL[i]);
			}
		}
		// 2. crates stacked like a market stall
		String[] crates = {"apple_crate", "carrot_crate", "potato_crate", "beetroot_crate"};
		for (int i = 0; i < 4; i++) {
			set(40 + i, 201, 0, crates[i]);
		}
		set(41, 202, 0, "potato_crate");
		set(42, 202, 0, "apple_crate");
		set(40, 201, 1, "carrot_crate");
		set(43, 201, 1, "beetroot_crate");
		// 3. bundles in all orientations
		for (int y = 201; y <= 203; y++) {
			set(80, y, 0, "sugar_cane_bundle");
			set(84, y, 0, "blaze_rod_bundle");
		}
		cmd("setblock 81 201 0 storageback:sugar_cane_bundle[axis=x]");
		cmd("setblock 82 201 0 storageback:sugar_cane_bundle[axis=z]");
		cmd("setblock 83 201 0 storageback:blaze_rod_bundle[axis=z]");
		cmd("setblock 82 202 0 storageback:blaze_rod_bundle[axis=x]");
		// 4. the "material" blocks
		String[] mats = {"flint_block", "charcoal_block", "gunpowder_block", "ender_pearl_block", "leather_block", "feather_block", "rotten_flesh_block"};
		for (int i = 0; i < mats.length; i++) {
			set(120 + i, 201, 0, mats[i]);
		}
		set(121, 202, 0, "flint_block");
		set(123, 202, 0, "ender_pearl_block");
		set(125, 202, 0, "feather_block");
		// 5. one ender pearl block in front of a white wall
		cmd("fill 158 201 10 162 205 10 minecraft:white_concrete");
		set(160, 201, 8, "ender_pearl_block");
		waitTicks(100);
		check(computeServer(s -> s.overworld().getBlockState(new net.minecraft.core.BlockPos(7, 201, 0)).is(ModBlocks.ENDER_PEARL_BLOCK)), "blocks placed in the world");

		tp(6.5, 201.6, 6.0, 6.5, 202.0, 0.5);
		shot("storageback_wall");
		tp(-1.8, 202.4, 5.2, 6.0, 200.6, 0.5);
		shot("storageback_wall_angle");
		tp(44.0, 202.4, 3.4, 41.6, 200.0, 0.5);
		shot("storageback_crates");
		tp(85.6, 202.8, 3.6, 82.2, 200.4, 0.5);
		shot("storageback_bundles");
		tp(125.4, 202.4, 3.8, 123.0, 200.0, 0.5);
		shot("storageback_materials");
		// eye level with the block (spectators clip into the ground), looking through the gaps at the white wall
		cmd("tp @p 161.6 199.95 6.4 27.6 2.0");
		waitTicks(40);
		shot("ender_pearl_block_close");

		// Inventory: the guide book crafted in the 2x2 grid, ender pearl block held in hand.
		client(mc -> mc.options.hideGui = false);
		cmd("gamemode survival @a");
		cmd("tp @p 30.5 201 20.5 facing 34 201.4 24");
		cmd("item replace entity @p hotbar.0 with storageback:ender_pearl_block");
		cmd("item replace entity @p hotbar.1 with storageback:guide_book");
		cmd("item replace entity @p hotbar.2 with minecraft:book");
		cmd("item replace entity @p hotbar.3 with storageback:flint_block");
		waitTicks(40);
		shot("ender_pearl_block_hand");
		for (String lang : new String[]{"en_us", "pl_pl"}) {
			if (!lang.equals("en_us")) {
				setLanguage(lang);
			}
			String suffix = lang.substring(0, 2);
			client(mc -> GuideBookScreen.assertContentsFit(mc.font, lang));
			check(true, "guide book text fits (" + lang + ")");
			craftGuideBook(suffix);
			captureBook(suffix);
		}
	}

	private static void craftGuideBook(String suffix) {
		server(s -> {
			ServerPlayer p = player(s);
			p.inventoryMenu.getSlot(1).set(new ItemStack(Items.BOOK));
			p.inventoryMenu.getSlot(2).set(new ItemStack(ModBlocks.FLINT_BLOCK));
			p.inventoryMenu.slotsChanged(p.inventoryMenu.getCraftSlots());
			p.inventoryMenu.broadcastChanges();
		});
		client(mc -> mc.setScreen(new InventoryScreen(mc.player)));
		waitTicks(15);
		check(computeClient(mc -> mc.player.inventoryMenu.getSlot(0).getItem().is(ModItems.GUIDE_BOOK)), "inventory crafting grid offers the guide book (" + suffix + ")");
		shot("crafting-" + suffix);
		server(s -> {
			ServerPlayer p = player(s);
			for (int i = 1; i <= 4; i++) p.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
			p.inventoryMenu.broadcastChanges();
		});
		client(mc -> mc.setScreen(null));
		waitTicks(5);
	}

	private static void captureBook(String lang) {
		client(mc -> mc.setScreen(new GuideBookScreen()));
		waitTicks(10);
		check(computeClient(mc -> mc.screen instanceof GuideBookScreen), "guide book screen open (" + lang + ")");
		shot("book-index-" + lang);
		book(GuideBookScreen::openStorageList);
		shot("book-storage-" + lang);
		for (String id : new String[]{"flint_block", "ender_pearl_block", "blaze_rod_bundle", "apple_crate"}) {
			book(s -> s.openEntry(id));
			shot("book-" + id + "-" + lang);
		}
		book(GuideBookScreen::openBackList);
		shot("book-back-list-" + lang);
		for (String id : new String[]{"quartz", "dark_prismarine", "prismarine_bricks", "magma", "nether_wart"}) {
			book(s -> s.openEntry(id));
			shot("book-" + id + "-" + lang);
		}
		book(GuideBookScreen::openConfig);
		shot("book-config-" + lang);
		client(mc -> mc.setScreen(null));
		waitTicks(5);
	}

	private static void book(Consumer<GuideBookScreen> action) {
		client(mc -> {
			if (mc.screen instanceof GuideBookScreen screen) action.accept(screen);
		});
		waitTicks(6);
	}

	private static void setLanguage(String code) {
		client(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
		});
		CompletableFuture<Void> reload = computeClient(Minecraft::reloadResourcePacks);
		while (!reload.isDone()) {
			sleep(50);
		}
		waitFor(mc -> mc.getOverlay() == null, 30 * 20, "resource reload");
		client(mc -> mc.setScreen(null));
		waitTicks(20);
	}
}
