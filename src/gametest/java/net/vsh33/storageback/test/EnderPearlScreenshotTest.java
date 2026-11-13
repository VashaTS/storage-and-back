package net.vsh33.storageback.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/**
 * Close-ups of the 9-pearl Ender Pearl Block in the world, in the hand, and in the inventory.
 * Screenshots are written to /workspace/snb-pearl-*.png.
 */
public class EnderPearlScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of("/workspace");

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
				.disableCounterPrefix()
				.withSize(1600, 900)
				.withDestinationDir(OUT));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add 80 0 140 40");
			server.runCommand("fill 90 200 0 130 200 40 minecraft:grass_block");
			// Bright backdrop so gaps between pearls show clearly. One isolated block.
			server.runCommand("fill 98 201 10 102 205 10 minecraft:white_concrete");
			server.runCommand("fill 97 201 7 97 205 10 minecraft:light_gray_concrete");
			server.runCommand("fill 103 201 7 103 205 10 minecraft:light_gray_concrete");
			server.runCommand("setblock 100 201 8 storageback:ender_pearl_block");
			server.runCommand("gamemode spectator @a");
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(80);

			// Eye-level 3/4 view: look through the gaps at the white wall.
			server.runCommand("tp @p 101.85 201.85 6.15 facing 100.45 201.55 8.2");
			context.waitTicks(40);
			shot(context, "snb-pearl-world");

			// Closer, slightly higher, still seeing between the pearls.
			server.runCommand("tp @p 101.35 202.15 6.55 facing 100.5 201.55 8.35");
			context.waitTicks(40);
			shot(context, "snb-pearl-world-close");

			// First-person hand: stand in the open, away from the placed block.
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("gamemode survival @a");
			server.runCommand("item replace entity @p hotbar.0 with storageback:ender_pearl_block");
			server.runCommand("item replace entity @p inventory.0 with storageback:ender_pearl_block 8");
			server.runCommand("tp @p 120.5 201.0 20.5 facing 124.0 201.4 24.0");
			context.waitTicks(160); // let recipe toasts expire
			shot(context, "snb-pearl-hand");

			context.getInput().pressKey(options -> options.keyInventory);
			context.waitTicks(20);
			shot(context, "snb-pearl-inventory");
		}
	}
}
