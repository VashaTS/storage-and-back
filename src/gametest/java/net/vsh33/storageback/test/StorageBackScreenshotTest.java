package net.vsh33.storageback.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.vsh33.storageback.client.StorageBackConfigScreen;

/** Builds small scenes with every block and saves screenshots (run/screenshots). xvfb-run -a ./gradlew runClientGameTest */
public class StorageBackScreenshotTest implements FabricClientGameTest {
	private static final String[] ALL = {"flint_block", "sugar_cane_bundle", "charcoal_block", "gunpowder_block", "blaze_rod_bundle",
			"leather_block", "feather_block", "ender_pearl_block", "rotten_flesh_block", "carrot_crate", "potato_crate", "beetroot_crate", "apple_crate"};

	private static void set(TestServerContext server, int x, int y, int z, String block) {
		server.runCommand("setblock " + x + " " + y + " " + z + " storageback:" + block);
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, String name, double x, double y, double z, double fx, double fy, double fz) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", x, y, z, fx, fy, fz));
		context.waitTicks(40);
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1600, 900));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -80 -64 200 64");
			for (int x = -80; x < 200; x += 40) {
				server.runCommand("fill " + x + " 200 -64 " + (x + 39) + " 200 64 minecraft:grass_block");
			}
			server.runCommand("gamemode spectator @a");
			context.getInput().pressKey(options -> options.keyToggleGui); // F1: hide the HUD

			// 1. storage wall: every block, three high
			for (int i = 0; i < ALL.length; i++) {
				for (int y = 201; y <= 203; y++) {
					set(server, i, y, 0, ALL[i]);
				}
			}
			// 2. crates stacked like a market stall
			String[] crates = {"apple_crate", "carrot_crate", "potato_crate", "beetroot_crate"};
			for (int i = 0; i < 4; i++) {
				set(server, 40 + i, 201, 0, crates[i]);
			}
			set(server, 41, 202, 0, "potato_crate");
			set(server, 42, 202, 0, "apple_crate");
			set(server, 40, 201, 1, "carrot_crate");
			set(server, 43, 201, 1, "beetroot_crate");
			// 3. bundles in all orientations
			for (int y = 201; y <= 203; y++) {
				set(server, 80, y, 0, "sugar_cane_bundle");
				set(server, 84, y, 0, "blaze_rod_bundle");
			}
			server.runCommand("setblock 81 201 0 storageback:sugar_cane_bundle[axis=x]");
			server.runCommand("setblock 82 201 0 storageback:sugar_cane_bundle[axis=z]");
			server.runCommand("setblock 83 201 0 storageback:blaze_rod_bundle[axis=z]");
			server.runCommand("setblock 82 202 0 storageback:blaze_rod_bundle[axis=x]");
			// 4. the "material" blocks
			String[] mats = {"flint_block", "charcoal_block", "gunpowder_block", "ender_pearl_block", "leather_block", "feather_block", "rotten_flesh_block"};
			for (int i = 0; i < mats.length; i++) {
				set(server, 120 + i, 201, 0, mats[i]);
			}
			set(server, 121, 202, 0, "flint_block");
			set(server, 123, 202, 0, "ender_pearl_block");
			set(server, 125, 202, 0, "feather_block");
			context.waitTicks(120);

			// "tp ... facing" aims from the feet, so targets are given ~1.6 blocks lower than the point to look at
			shot(context, server, "storageback_wall", 6.5, 201.6, 6.0, 6.5, 202.0, 0.5);
			shot(context, server, "storageback_wall_angle", -1.8, 202.4, 5.2, 6.0, 200.6, 0.5);
			shot(context, server, "storageback_crates", 44.0, 202.4, 3.4, 41.6, 200.0, 0.5);
			shot(context, server, "storageback_bundles", 85.6, 202.8, 3.6, 82.2, 200.4, 0.5);
			shot(context, server, "storageback_materials", 125.4, 202.4, 3.8, 123.0, 200.0, 0.5);
			if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
				// ./gradlew runClientGameTest -PwithConfigMods=true
				context.setScreen(() -> StorageBackConfigScreen.create(null));
				context.waitTicks(10);
				context.takeScreenshot("storageback_config");
				context.setScreen(() -> null);
			}
		}
	}
}
