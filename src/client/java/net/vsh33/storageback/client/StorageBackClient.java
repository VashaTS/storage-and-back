package net.vsh33.storageback.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.vsh33.storageback.item.GuideBookScreens;

/** Client setup: the guide book opens the custom illustrated guidebook screen. */
public class StorageBackClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GuideBookScreens.setOpener(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gui.setScreen(new GuideBookScreen());
		});
	}
}
