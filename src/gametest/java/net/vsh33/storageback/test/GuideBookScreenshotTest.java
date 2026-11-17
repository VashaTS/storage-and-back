package net.vsh33.storageback.test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.storageback.block.ModBlocks;
import net.vsh33.storageback.item.GuideBookPages;
import net.vsh33.storageback.item.ModItems;

/**
 * Screenshots of the guide book in the inventory, in the 2×2 crafting grid, and open on several pages in EN and PL.
 * Written to /workspace/snb-book/. Also fails if any page overflows the vanilla book text box.
 */
public class GuideBookScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of("/workspace/snb-book");
	private static final int BOOK_TEXT_WIDTH = 114;
	private static final int BOOK_MAX_LINES = 128 / 9;

	private static void shot(ClientGameTestContext context, String name) {
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

			assertPagesFit(context, "en_us");

			server.runCommand("item replace entity @p hotbar.0 with storageback:guide_book");
			server.runCommand("item replace entity @p hotbar.1 with minecraft:book");
			server.runCommand("item replace entity @p hotbar.2 with storageback:flint_block");
			context.waitTicks(40);

			clearCrafting(server);
			openInventory(context);
			shot(context, "inventory-en");

			// 2×2 crafting grid: Book + Flint Block → guide book
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
			openBook(context);
			shotPages(context, "en", new int[]{0, 1, 5, 8, 14, 15, 19, 20});

			setLanguage(context, "pl_pl");
			assertPagesFit(context, "pl_pl");

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
			openBook(context);
			shotPages(context, "pl", new int[]{0, 1, 5, 8, 14, 15, 19, 20});
		}
	}

	private static void openBook(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(GuideBookPages.pages()))));
		context.waitForScreen(BookViewScreen.class);
		context.waitTicks(5);
	}

	private static void shotPages(ClientGameTestContext context, String lang, int[] pages) {
		for (int page : pages) {
			final int target = page;
			context.runOnClient(mc -> {
				if (mc.gui.screen() instanceof BookViewScreen screen) {
					screen.setPage(target);
				}
			});
			context.waitTicks(5);
			shot(context, "book-" + lang + "-p" + page);
		}
	}

	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
		});
		CompletableFuture<Void> reload = context.computeOnClient(Minecraft::reloadResourcePacks);
		context.waitFor(mc -> reload.isDone(), 30 * 20);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		// The reload splash (Mojang logo) must finish before we open any screen.
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

	private static void assertPagesFit(ClientGameTestContext context, String lang) {
		context.runOnClient(mc -> {
			StringBuilder overflows = new StringBuilder();
			int i = 0;
			for (Component page : GuideBookPages.pages()) {
				int lines = mc.font.split(page, BOOK_TEXT_WIDTH).size();
				System.out.println("[guide-book] " + lang + " page " + i + " lines=" + lines + "/" + BOOK_MAX_LINES);
				if (lines > BOOK_MAX_LINES) {
					overflows.append(" page ").append(i).append(" has ").append(lines).append(" lines: ")
							.append(page.getString().replace("\n", " / ")).append("\n");
				}
				i++;
			}
			if (!overflows.isEmpty()) {
				throw new AssertionError("Guide book overflows in " + lang + ":\n" + overflows);
			}
		});
	}
}
