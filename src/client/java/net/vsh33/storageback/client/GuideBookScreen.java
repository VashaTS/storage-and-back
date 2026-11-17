package net.vsh33.storageback.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.vsh33.storageback.StorageBack;
import net.vsh33.storageback.client.GuideBookContents.Category;
import net.vsh33.storageback.client.GuideBookContents.Entry;
import net.vsh33.storageback.client.GuideBookContents.Link;
import net.vsh33.storageback.client.GuideBookContents.Location;
import net.vsh33.storageback.client.GuideBookContents.RecipeGrid;
import net.vsh33.storageback.client.GuideBookContents.Section;
import net.vsh33.storageback.config.StorageBackConfig;

/**
 * Custom two-page guidebook: parchment background, clickable category and entry
 * links, and crafting grids drawn with real item icons.
 */
public class GuideBookScreen extends Screen {
	public static final Identifier TEXTURE = StorageBack.id("textures/gui/guidebook.png");
	public static final int BOOK_W = 256;
	public static final int BOOK_H = 192;
	public static final int TEX = 256;
	public static final int PAGE_W = 110;
	public static final int LEFT = 14;
	public static final int RIGHT = 136;
	public static final int TOP = 14;
	public static final int PAGE_BOTTOM = 164;
	/** Entry body text under the icon/title; leaves room for a see-also link. */
	public static final int MAX_BODY_LINES = 11;
	public static final int MAX_INTRO_LINES = 6;
	public static final int MAX_CONFIG_LINES = 14;

	private static final int SLOT = 18;
	private static final int SLOT_U = 0;
	private static final int SLOT_V = 192;
	private static final int SLOT_HOVER_U = 18;
	private static final int ARROW_U = 40;
	private static final int ARROW_V = 194;
	private static final int ARROW_W = 22;
	private static final int ARROW_H = 15;
	private static final int HOME_U = 64;
	private static final int BACK_U = 80;
	private static final int ICON = 16;
	private static final int DISABLED_U = 96;
	private static final int RESULT_U = 114;
	private static final int WIDGET_V = 192;

	private static final int INK = 0xFF3A2A18;
	private static final int INK_SOFT = 0xFF5A4430;
	private static final int LINK = 0xFF2C4A7C;
	private static final int LINK_HOVER = 0xFF1A3060;
	private static final int GOLD = 0xFF7A5A10;
	private static final int DISABLED = 0xFF8B2A22;
	private static final int ENABLED = 0xFF2E6B3A;
	private static final int HOVER_FILL = 0x332A1C10;

	private final GuideBookContents contents;
	private final List<Location> history = new ArrayList<>();
	private final List<Hotspot> hotspots = new ArrayList<>();
	private Location location = Location.index();
	private int leftPos;
	private int topPos;
	private PageButton prevButton;
	private PageButton nextButton;

	private record Hotspot(int x, int y, int w, int h, Runnable action, ItemStack tooltipItem, Component tooltipText) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	public GuideBookScreen() {
		super(Component.translatable("item.storageback.guide_book"));
		this.contents = GuideBookContents.create();
	}

	@Override
	protected void init() {
		this.leftPos = (this.width - BOOK_W) / 2;
		this.topPos = (this.height - BOOK_H) / 2;
		this.prevButton = this.addRenderableWidget(new PageButton(this.leftPos + 18, this.topPos + 172, false, button -> this.turnPage(-1), true));
		this.nextButton = this.addRenderableWidget(new PageButton(this.leftPos + BOOK_W - 41, this.topPos + 172, true, button -> this.turnPage(1), true));
		this.updateButtons();
	}

	public void openIndex() {
		this.goTo(Location.index(), true);
	}

	public void openStorageList() {
		this.goTo(Location.storageList(), true);
	}

	public void openBackList() {
		this.goTo(Location.backList(), true);
	}

	public void openConfig() {
		this.goTo(Location.config(), true);
	}

	public void openEntry(String id) {
		this.goTo(Location.entry(id), true);
	}

	public Location location() {
		return this.location;
	}

	private void goTo(Location target, boolean recordHistory) {
		if (recordHistory && !GuideBookContents.same(this.location, target)) {
			this.history.add(this.location);
			if (this.history.size() > 64) {
				this.history.removeFirst();
			}
		}
		this.location = target;
		this.updateButtons();
	}

	private void goBack() {
		if (this.history.isEmpty()) {
			return;
		}
		this.location = this.history.removeLast();
		this.updateButtons();
		this.pageSound();
	}

	private void turnPage(int delta) {
		int index = this.contents.indexOf(this.location) + delta;
		List<Location> order = this.contents.readingOrder();
		if (index < 0 || index >= order.size()) {
			return;
		}
		this.goTo(order.get(index), true);
	}

	private void updateButtons() {
		int index = this.contents.indexOf(this.location);
		if (this.prevButton != null) {
			this.prevButton.visible = index > 0;
			this.nextButton.visible = index < this.contents.readingOrder().size() - 1;
		}
	}

	private void pageSound() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
	}

	private void clickSound() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, BOOK_W, BOOK_H, TEX, TEX);
		this.hotspots.clear();
		switch (this.location.section()) {
			case INDEX -> this.drawIndex(graphics, mouseX, mouseY);
			case STORAGE_LIST -> this.drawList(graphics, mouseX, mouseY, Component.translatable("book.storageback.category.storage"), this.contents.storageEntries(), null);
			case BACK_LIST -> this.drawList(graphics, mouseX, mouseY, Component.translatable("book.storageback.category.back"), this.contents.backEntries(), Component.translatable("book.storageback.page.back.intro"));
			case CONFIG -> this.drawConfig(graphics, mouseX, mouseY);
			case ENTRY -> this.drawEntry(graphics, mouseX, mouseY);
		}
		this.drawNavIcons(graphics, mouseX, mouseY);
		this.applyHoverTooltip(graphics, mouseX, mouseY);
	}

	private void drawIndex(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		int y = ly + this.drawTitle(graphics, lx, ly, PAGE_W, Component.translatable("book.storageback.title_heading"));
		y = graphics.textWithWordWrap(this.font, Component.translatable("book.storageback.intro"), lx, y, PAGE_W, INK, false);
		y += 8;
		this.drawRecipe(graphics, lx, y, this.contents.guideBookRecipe(), Component.translatable("book.storageback.heading.crafting"), mouseX, mouseY);

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		ry += this.drawTitle(graphics, rx, ry, PAGE_W, Component.translatable("book.storageback.heading.index"));
		for (Category category : this.contents.categories()) {
			ry += this.drawLinkRow(graphics, rx, ry, PAGE_W, category.icon(), category.title(), category.target(), mouseX, mouseY, true);
		}
	}

	private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Component title, List<Entry> entries, Component intro) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		ly += this.drawTitle(graphics, lx, ly, PAGE_W, title);
		if (intro != null) {
			ly = graphics.textWithWordWrap(this.font, intro, lx, ly, PAGE_W, INK, false);
			ly += 4;
		}
		int bottom = this.topPos + PAGE_BOTTOM;
		int rightStart = intro != null ? this.topPos + TOP : ly;
		int i = 0;
		while (i < entries.size()) {
			Entry entry = entries.get(i);
			int height = this.linkRowHeight(false);
			if (ly + height > bottom) {
				break;
			}
			ly += this.drawLinkRow(graphics, lx, ly, PAGE_W, entry.icon(), this.listTitle(entry), Location.entry(entry.id()), mouseX, mouseY, false);
			i++;
		}
		int rx = this.leftPos + RIGHT;
		int ry = rightStart;
		while (i < entries.size()) {
			Entry entry = entries.get(i);
			int height = this.linkRowHeight(false);
			if (ry + height > bottom) {
				break;
			}
			ry += this.drawLinkRow(graphics, rx, ry, PAGE_W, entry.icon(), this.listTitle(entry), Location.entry(entry.id()), mouseX, mouseY, false);
			i++;
		}
	}

	private int linkRowHeight(boolean spacious) {
		return spacious ? 22 : 16;
	}

	private Component listTitle(Entry entry) {
		MutableComponent title = entry.title().copy();
		boolean anyBalancing = false;
		boolean anyDisabled = false;
		for (RecipeGrid grid : entry.recipes()) {
			if (grid.balancing()) {
				anyBalancing = true;
			}
			if (grid.disabled()) {
				anyDisabled = true;
			}
		}
		if (anyBalancing) {
			title.append(Component.literal(" *").withStyle(ChatFormatting.GOLD));
		}
		if (anyDisabled) {
			title = title.withStyle(style -> style.withColor(DISABLED));
		}
		return title;
	}

	private void drawConfig(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		ly += this.drawTitle(graphics, lx, ly, PAGE_W, Component.translatable("book.storageback.category.config"));
		ly = graphics.textWithWordWrap(this.font, Component.translatable("book.storageback.page.config.balancing"), lx, ly, PAGE_W, INK, false);
		ly += 4;
		this.drawStatus(graphics, lx, ly, StorageBackConfig.get().balancingChanges);

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		ry += this.drawTitle(graphics, rx, ry, PAGE_W, Component.translatable("book.storageback.category.back"));
		ry = graphics.textWithWordWrap(this.font, Component.translatable("book.storageback.page.config.back"), rx, ry, PAGE_W, INK, false);
		ry += 4;
		ry = this.drawStatus(graphics, rx, ry, StorageBackConfig.get().backRecipes);
		ry += 8;
		graphics.textWithWordWrap(this.font, Component.translatable("book.storageback.page.config.how"), rx, ry, PAGE_W, INK, false);
	}

	private int drawStatus(GuiGraphicsExtractor graphics, int x, int y, boolean on) {
		Component text = Component.translatable(on ? "book.storageback.status.on" : "book.storageback.status.off");
		graphics.text(this.font, text, x, y, on ? ENABLED : DISABLED, false);
		return y + this.font.lineHeight;
	}

	private void drawEntry(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Entry entry = this.contents.entry(this.location.entryId());
		if (entry == null) {
			return;
		}
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		this.drawLargeIcon(graphics, lx, ly, entry.icon(), mouseX, mouseY);
		int titleX = lx + 36;
		int titleW = PAGE_W - 36;
		List<FormattedCharSequence> titleLines = this.font.split(entry.title().copy().withStyle(ChatFormatting.BOLD), titleW);
		int titleY = ly + Math.max(0, (32 - titleLines.size() * 9) / 2);
		for (FormattedCharSequence line : titleLines) {
			graphics.text(this.font, line, titleX, titleY, INK, false);
			titleY += 9;
		}
		int y = ly + 36;
		y = graphics.textWithWordWrap(this.font, entry.body(), lx, y, PAGE_W, INK, false);
		if (!entry.links().isEmpty()) {
			y += 8;
			graphics.text(this.font, Component.translatable("book.storageback.see_also"), lx, y, INK_SOFT, false);
			y += 11;
			for (Link link : entry.links()) {
				y += this.drawTextLink(graphics, lx, y, PAGE_W, link.label(), link.target(), mouseX, mouseY);
			}
		}

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		for (int i = 0; i < entry.recipes().size(); i++) {
			RecipeGrid grid = entry.recipes().get(i);
			Component header;
			if (grid.shaped()) {
				header = Component.translatable("book.storageback.heading.crafting");
			} else if (grid.backRecipe() || entry.recipes().stream().anyMatch(RecipeGrid::shaped)) {
				header = Component.translatable("book.storageback.heading.uncrafting");
			} else {
				header = Component.translatable("book.storageback.heading.shapeless");
			}
			int used = this.drawRecipe(graphics, rx, ry, grid, header, mouseX, mouseY);
			ry += used + 8;
		}
	}

	private void drawLargeIcon(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, int mouseX, int mouseY) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, RESULT_U, WIDGET_V, SLOT, SLOT, TEX, TEX);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 14, y + 14, RESULT_U, WIDGET_V, SLOT, SLOT, TEX, TEX);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(2.0F, 2.0F);
		graphics.item(stack, 1, 1);
		graphics.pose().popMatrix();
		this.hotspots.add(new Hotspot(x, y, 32, 32, null, stack, null));
	}

	private int drawLinkRow(GuiGraphicsExtractor graphics, int x, int y, int width, ItemStack icon, Component title, Location target, int mouseX, int mouseY, boolean spacious) {
		int height = this.linkRowHeight(spacious);
		boolean hover = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		if (hover) {
			graphics.fill(x - 1, y - 1, x + width, y + height, HOVER_FILL);
		}
		graphics.item(icon, x, y);
		graphics.itemDecorations(this.font, icon, x, y);
		int textX = x + 18;
		int textW = width - 18;
		boolean star = title.getString().endsWith(" *");
		Component toFit = title;
		int starW = 0;
		if (star && !spacious) {
			starW = this.font.width(" *");
			toFit = Component.literal(title.getString().substring(0, title.getString().length() - 2)).withStyle(title.getStyle());
		}
		Component fitted = spacious ? title : this.fitOneLine(toFit, textW - starW);
		Component label = hover
				? fitted.copy().withStyle(Style.EMPTY.withUnderlined(true).withColor(LINK_HOVER))
				: fitted.copy().withStyle(Style.EMPTY.withColor(LINK));
		if (spacious) {
			List<FormattedCharSequence> lines = this.font.split(label, textW);
			int textY = y + Math.max(0, (16 - Math.min(2, lines.size()) * 9) / 2);
			int drawn = 0;
			for (FormattedCharSequence line : lines) {
				graphics.text(this.font, line, textX, textY, hover ? LINK_HOVER : LINK, false);
				textY += 9;
				drawn++;
				if (drawn == 2) {
					break;
				}
			}
		} else {
			int textY = y + Math.max(0, (16 - 9) / 2);
			graphics.text(this.font, label, textX, textY, hover ? LINK_HOVER : LINK, false);
			if (star) {
				graphics.text(this.font, " *", textX + this.font.width(label), textY, GOLD, false);
			}
		}
		this.hotspots.add(new Hotspot(x, y, width, height, () -> {
			this.clickSound();
			this.goTo(target, true);
		}, icon, title));
		return height;
	}

	private Component fitOneLine(Component text, int width) {
		if (this.font.width(text) <= width) {
			return text;
		}
		String ellipsis = "...";
		int budget = Math.max(0, width - this.font.width(ellipsis));
		return Component.literal(this.font.plainSubstrByWidth(text.getString(), budget) + ellipsis).withStyle(text.getStyle());
	}

	private int drawTextLink(GuiGraphicsExtractor graphics, int x, int y, int width, Component label, Location target, int mouseX, int mouseY) {
		boolean hover = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + 10;
		Component text = hover
				? label.copy().withStyle(Style.EMPTY.withUnderlined(true).withColor(LINK_HOVER))
				: label.copy().withStyle(Style.EMPTY.withColor(LINK));
		graphics.text(this.font, text, x, y, hover ? LINK_HOVER : LINK, false);
		this.hotspots.add(new Hotspot(x, y, width, 10, () -> {
			this.clickSound();
			this.goTo(target, true);
		}, ItemStack.EMPTY, label));
		return 11;
	}

	private int drawTitle(GuiGraphicsExtractor graphics, int x, int y, int width, Component title) {
		Component bold = title.copy().withStyle(ChatFormatting.BOLD);
		List<FormattedCharSequence> lines = this.font.split(bold, width);
		int yy = y;
		int lineW = 24;
		for (FormattedCharSequence line : lines) {
			graphics.text(this.font, line, x, yy, INK, false);
			lineW = Math.max(lineW, this.font.width(line));
			yy += 9;
		}
		lineW = Math.min(width, lineW);
		graphics.fill(x, yy + 1, x + lineW, yy + 2, 0xFFB08A4A);
		return yy + 4 - y;
	}

	private int drawRecipe(GuiGraphicsExtractor graphics, int x, int y, RecipeGrid grid, Component header, int mouseX, int mouseY) {
		graphics.text(this.font, header.copy().withStyle(ChatFormatting.BOLD), x, y, INK, false);
		int gridY = y + 12;
		int gridX = x;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int i = row * 3 + col;
				ItemStack stack = grid.slots().get(i);
				this.drawSlot(graphics, gridX + col * SLOT, gridY + row * SLOT, stack, false, grid.disabled(), mouseX, mouseY);
			}
		}
		int arrowX = gridX + 3 * SLOT + 4;
		int arrowY = gridY + SLOT + (SLOT - ARROW_H) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, arrowX, arrowY, ARROW_U, ARROW_V, ARROW_W, ARROW_H, TEX, TEX);
		int resultX = arrowX + ARROW_W + 4;
		int resultY = gridY + SLOT;
		this.drawSlot(graphics, resultX, resultY, grid.result(), true, grid.disabled(), mouseX, mouseY);
		int bottom = gridY + 3 * SLOT;
		if (grid.disabled()) {
			graphics.text(this.font, Component.translatable("book.storageback.disabled"), x, bottom + 2, DISABLED, false);
			bottom += 11;
		} else if (grid.balancing()) {
			graphics.text(this.font, Component.literal("* ").append(Component.translatable("book.storageback.balancing")), x, bottom + 2, GOLD, false);
			bottom += 11;
		}
		return bottom - y;
	}

	private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, boolean result, boolean disabled, int mouseX, int mouseY) {
		boolean hover = !stack.isEmpty() && mouseX >= x && mouseY >= y && mouseX < x + SLOT && mouseY < y + SLOT;
		int u = result ? RESULT_U : (hover ? SLOT_HOVER_U : SLOT_U);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, WIDGET_V, SLOT, SLOT, TEX, TEX);
		if (!stack.isEmpty()) {
			graphics.item(stack, x + 1, y + 1);
			graphics.itemDecorations(this.font, stack, x + 1, y + 1);
			this.hotspots.add(new Hotspot(x, y, SLOT, SLOT, null, stack, null));
		}
		if (disabled) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, DISABLED_U, WIDGET_V, SLOT, SLOT, TEX, TEX);
		}
	}

	private void drawNavIcons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int backX = this.leftPos + 102;
		int homeX = this.leftPos + 138;
		int iconY = this.topPos + 174;
		boolean backHover = mouseX >= backX && mouseY >= iconY && mouseX < backX + ICON && mouseY < iconY + ICON;
		boolean homeHover = mouseX >= homeX && mouseY >= iconY && mouseX < homeX + ICON && mouseY < iconY + ICON;
		if (backHover && !this.history.isEmpty()) {
			graphics.fill(backX - 1, iconY - 1, backX + ICON + 1, iconY + ICON + 1, HOVER_FILL);
		}
		if (homeHover) {
			graphics.fill(homeX - 1, iconY - 1, homeX + ICON + 1, iconY + ICON + 1, HOVER_FILL);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, backX, iconY, BACK_U, WIDGET_V, ICON, ICON, TEX, TEX);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, homeX, iconY, HOME_U, WIDGET_V, ICON, ICON, TEX, TEX);
		if (this.history.isEmpty()) {
			graphics.fill(backX, iconY, backX + ICON, iconY + ICON, 0x66805030);
		}
		this.hotspots.add(new Hotspot(backX, iconY, ICON, ICON, () -> {
			if (!this.history.isEmpty()) {
				this.goBack();
			}
		}, ItemStack.EMPTY, Component.translatable("book.storageback.nav.back")));
		this.hotspots.add(new Hotspot(homeX, iconY, ICON, ICON, () -> {
			this.clickSound();
			this.goTo(Location.index(), true);
		}, ItemStack.EMPTY, Component.translatable("book.storageback.nav.index")));
	}

	private void applyHoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Last matching hotspot wins so item icons painted over a row still show the item tooltip.
		Hotspot hit = null;
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.contains(mouseX, mouseY)) {
				hit = hotspot;
			}
		}
		if (hit == null) {
			return;
		}
		if (hit.tooltipItem != null && !hit.tooltipItem.isEmpty()) {
			graphics.setTooltipForNextFrame(this.font, hit.tooltipItem, mouseX, mouseY);
		} else if (hit.tooltipText != null) {
			graphics.setTooltipForNextFrame(this.font, hit.tooltipText, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (event.button() == 0) {
			for (int i = this.hotspots.size() - 1; i >= 0; i--) {
				Hotspot hotspot = this.hotspots.get(i);
				if (hotspot.action != null && hotspot.contains(event.x(), event.y())) {
					hotspot.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (super.keyPressed(event)) {
			return true;
		}
		if (event.isLeft()) {
			this.turnPage(-1);
			return true;
		}
		if (event.isRight()) {
			this.turnPage(1);
			return true;
		}
		return false;
	}

	/**
	 * Fails the screenshot / content test if any translated body is too long for its page.
	 */
	public static void assertContentsFit(Font font, String lang) {
		GuideBookContents contents = GuideBookContents.create();
		StringBuilder errors = new StringBuilder();
		check(font, lang, errors, "intro", Component.translatable("book.storageback.intro"), MAX_INTRO_LINES);
		check(font, lang, errors, "back.intro", Component.translatable("book.storageback.page.back.intro"), 3);
		check(font, lang, errors, "config.balancing", Component.translatable("book.storageback.page.config.balancing"), MAX_CONFIG_LINES);
		check(font, lang, errors, "config.back", Component.translatable("book.storageback.page.config.back"), MAX_CONFIG_LINES);
		check(font, lang, errors, "config.how", Component.translatable("book.storageback.page.config.how"), 5);
		for (Entry entry : contents.storageEntries()) {
			int max = entry.links().isEmpty() ? MAX_BODY_LINES : MAX_BODY_LINES - 3;
			if (entry.recipes().stream().anyMatch(RecipeGrid::balancing)) {
				max -= 1;
			}
			check(font, lang, errors, entry.id(), entry.body(), max);
		}
		for (Entry entry : contents.backEntries()) {
			int max = entry.recipes().stream().anyMatch(RecipeGrid::balancing) ? MAX_BODY_LINES - 1 : MAX_BODY_LINES;
			check(font, lang, errors, "back." + entry.id(), entry.body(), max);
		}
		if (!errors.isEmpty()) {
			throw new AssertionError("Guide book text overflows in " + lang + ":\n" + errors);
		}
	}

	private static void check(Font font, String lang, StringBuilder errors, String name, Component text, int maxLines) {
		int lines = font.split(text, PAGE_W).size();
		System.out.println("[guide-book] " + lang + " " + name + " lines=" + lines + "/" + maxLines);
		if (lines > maxLines) {
			errors.append(" ").append(name).append(" has ").append(lines).append(" lines (max ").append(maxLines).append("): ")
					.append(text.getString().replace("\n", " / ")).append("\n");
		}
	}
}
