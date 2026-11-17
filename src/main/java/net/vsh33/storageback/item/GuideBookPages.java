package net.vsh33.storageback.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Pages shown in the Storage &amp; Back guide book. Text comes from the language files
 * so English and Polish (and any later translation) follow the client's language.
 */
public final class GuideBookPages {
	/** Block ids, in the same order as the storage blocks in the creative tab. */
	public static final List<String> BLOCKS = List.of(
			"flint_block",
			"sugar_cane_bundle",
			"charcoal_block",
			"gunpowder_block",
			"blaze_rod_bundle",
			"leather_block",
			"feather_block",
			"ender_pearl_block",
			"rotten_flesh_block",
			"carrot_crate",
			"potato_crate",
			"beetroot_crate",
			"apple_crate");

	public static final List<String> BACK_PAGES = List.of("back_intro", "back_1", "back_2", "back_3", "back_4", "back_5");

	public static List<Component> pages() {
		List<Component> pages = new ArrayList<>();
		pages.add(titled("book.storageback.title_heading", "book.storageback.page.title"));
		for (String id : BLOCKS) {
			pages.add(titled("block.storageback." + id, "book.storageback.page." + id));
		}
		for (String id : BACK_PAGES) {
			pages.add(titled("book.storageback.heading.back", "book.storageback.page." + id));
		}
		pages.add(titled("book.storageback.heading.config", "book.storageback.page.config"));
		return pages;
	}

	private static Component titled(String titleKey, String bodyKey) {
		MutableComponent page = Component.empty();
		page.append(Component.translatable(titleKey).withStyle(ChatFormatting.BOLD));
		page.append("\n\n");
		page.append(Component.translatable(bodyKey));
		return page;
	}

	private GuideBookPages() {
	}
}
