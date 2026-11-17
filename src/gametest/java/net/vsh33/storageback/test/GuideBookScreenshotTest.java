package net.vsh33.storageback.test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.client.GuideBookScreen;

/**
 * Screenshots of the illustrated guide book: index, category lists and several
 * entries in English and Polish, written to /workspace/snb-book2/. Also fails
 * if any page text overflows its column.
 */
public class GuideBookScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of("/workspace/snb-book2");

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name)
				.disableCounterPrefix()
				.withSize(1280, 720)
				.withDestinationDir(OUT));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule keepInventory true");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -8 -8 8 8");
			server.runCommand("fill -5 200 -5 5 200 5 minecraft:grass_block");
			server.runCommand("fill -5 201 -5 5 205 5 minecraft:air");
			server.runCommand("tp @p 0.5 201 0.5 facing 4.0 201 4.0");
			server.runCommand("gamemode survival @a");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(20);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);

			assertTextFits(context, "en_us");

			server.runCommand("item replace entity @p hotbar.0 with storageback:guide_book");
			server.runCommand("item replace entity @p hotbar.1 with minecraft:book");
			server.runCommand("item replace entity @p hotbar.2 with storageback:flint_block");
			context.waitTicks(40);

			clearCrafting(server);
			openInventory(context);
			shot(context, "inventory-en");

			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.inventoryMenu.getSlot(1).set(new ItemStack(Items.BOOK));
				player.inventoryMenu.getSlot(2).set(new ItemStack(ModBlocks.FLINT_BLOCK));
				player.inventoryMenu.broadcastChanges();
			});
			context.waitTicks(10);
			shot(context, "crafting-en");

			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			captureBook(context, "en");

			setLanguage(context, "pl_pl");
			assertTextFits(context, "pl_pl");

			clearCrafting(server);
			openInventory(context);
			shot(context, "inventory-pl");

			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.inventoryMenu.getSlot(1).set(new ItemStack(Items.BOOK));
				player.inventoryMenu.getSlot(2).set(new ItemStack(ModBlocks.FLINT_BLOCK));
				player.inventoryMenu.broadcastChanges();
			});
			context.waitTicks(10);
			shot(context, "crafting-pl");

			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			captureBook(context, "pl");
		}
	}

	private static void captureBook(ClientGameTestContext context, String lang) {
		openBook(context);
		shot(context, "index-" + lang);

		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof GuideBookScreen screen) {
				screen.openStorageList();
			}
		});
		context.waitTicks(5);
		shot(context, "storage-" + lang);

		openEntry(context, "flint_block");
		shot(context, "flint-" + lang);

		openEntry(context, "ender_pearl_block");
		shot(context, "ender-" + lang);

		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof GuideBookScreen screen) {
				screen.openBackList();
			}
		});
		context.waitTicks(5);
		shot(context, "back-list-" + lang);

		openEntry(context, "quartz");
		shot(context, "quartz-" + lang);

		openEntry(context, "dark_prismarine");
		shot(context, "dark-prismarine-" + lang);

		openEntry(context, "magma");
		shot(context, "magma-" + lang);

		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof GuideBookScreen screen) {
				screen.openConfig();
			}
		});
		context.waitTicks(5);
		shot(context, "config-" + lang);
	}

	private static void openBook(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gui.setScreen(new GuideBookScreen()));
		context.waitForScreen(GuideBookScreen.class);
		context.waitTicks(5);
	}

	private static void openEntry(ClientGameTestContext context, String id) {
		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof GuideBookScreen screen) {
				screen.openEntry(id);
			}
		});
		context.waitTicks(5);
	}

	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
		});
		CompletableFuture<Void> reload = context.computeOnClient(Minecraft::reloadResourcePacks);
		context.waitFor(mc -> reload.isDone(), 30 * 20);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitFor(mc -> mc.gui.overlay() == null, 30 * 20);
		context.waitTicks(20);
	}

	private static void clearCrafting(TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			for (int i = 1; i <= 4; i++) {
				player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
			}
			player.inventoryMenu.broadcastChanges();
		});
	}

	private static void openInventory(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(10);
	}

	private static void assertTextFits(ClientGameTestContext context, String lang) {
		context.runOnClient(mc -> GuideBookScreen.assertContentsFit(mc.font, lang));
	}
}
