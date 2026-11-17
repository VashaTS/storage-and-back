package net.vsh33.storageback.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.vsh33.storageback.item.GuideBookPages;
import net.vsh33.storageback.item.GuideBookScreens;

/** Client setup: the guide book opens the vanilla read-only book screen. */
public class StorageBackClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GuideBookScreens.setOpener(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.gui.setScreen(new BookViewScreen(new BookViewScreen.BookAccess(GuideBookPages.pages())));
		});
	}
}
