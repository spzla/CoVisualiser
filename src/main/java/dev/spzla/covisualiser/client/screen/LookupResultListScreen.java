package dev.spzla.covisualiser.client.screen;

import dev.spzla.covisualiser.client.CoVisualiserClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LookupResultListScreen extends Screen {
    protected int x;
    protected int y;

    protected int margin = 40;

    protected int maxWidth = 588;
    protected int maxHeight = 272;

    protected int backgroundWidth = 588;
    protected int backgroundHeight = 272;

    protected int cardWidth;
    protected int cardHeight;
    protected int cardMarginTop = 16;
    protected int cardMarginHorizontal = 4;
    protected int cardMarginBottom = 28;
    protected int cardGap = 4;

    private final int cardsPerPage = 4;

    private final int shiftSkipPageMult = 10;

    protected List<CardWidget> cards = new ArrayList<>();

    private List<Component> resetTooltipText;
    private List<Component> previousTooltipText;
    private List<Component> nextTooltipText;

    private Button resetButton;
    private Button previousPageButton;
    private Button nextPageButton;

    private int pages;

    public LookupResultListScreen() {
        super(Component.literal("Lookup Results List"));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        this.calculateSizes();

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonMargin = 4;

        this.resetButton = Button.builder(Component.literal("RESET"), button -> {
            cv.resetState();
            cv.results.clear();
            this.rebuildWidgets();
        })
                .pos(this.x + buttonMargin, this.y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .size(buttonWidth, buttonHeight)
                .build();

        this.pages = Math.max(1, (int)Math.ceil(cv.results.size() / (float)cardsPerPage));
        int lastPage = pages - 1;

        this.resetTooltipText = List.of(
                Component.translatable("covisualiser.tooltip.reset")
        );

        this.previousTooltipText = List.of(
                Component.translatable("covisualiser.tooltip.skipnpages", shiftSkipPageMult),
                Component.translatable("covisualiser.tooltip.skiptofirst")
        );

        this.nextTooltipText = List.of(
                Component.translatable("covisualiser.tooltip.skipnpages", shiftSkipPageMult),
                Component.translatable("covisualiser.tooltip.skiptolast")
        );

        this.previousPageButton = Button.builder(Component.literal("PREVIOUS"), button -> {
            int pagesToSkip = -1;
            if (minecraft.hasShiftDown()) pagesToSkip *= minecraft.hasControlDown() ? cv.currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.rebuildWidgets();
        })
                .pos(this.x + this.backgroundWidth - 2 * (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        this.nextPageButton = Button.builder(Component.literal("NEXT"), button -> {
            int pagesToSkip = 1;
            if (minecraft.hasShiftDown()) pagesToSkip *= minecraft.hasControlDown() ? lastPage - cv.currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.rebuildWidgets();
        })
                .pos(this.x + this.backgroundWidth - (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        if (cv.currentPage == 0) {
            previousPageButton.active = false;
        }

        if (cv.currentPage == lastPage) {
            nextPageButton.active = false;
        }

        this.refreshCards();
        this.addRenderableWidget(this.resetButton);
        this.addRenderableWidget(this.previousPageButton);
        this.addRenderableWidget(this.nextPageButton);
    }

    private void calculateSizes() {
        this.backgroundWidth = Math.clamp(this.width - this.margin, 0, maxWidth);
        this.backgroundHeight = Math.clamp(this.height - this.margin, 0, maxHeight);

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        this.cardWidth = (this.backgroundWidth - 2 * this.cardMarginHorizontal - this.cardGap) / 2;
        this.cardHeight = (this.backgroundHeight - this.cardMarginTop - this.cardMarginBottom - this.cardGap) / 2;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (minecraft.hasShiftDown()) {
            int amount = minecraft.hasControlDown() ? this.shiftSkipPageMult : 1;
            if (verticalAmount > 0 || horizontalAmount > 0) {
                movePage(-amount);
                this.rebuildWidgets();
                return true;
            } else if (verticalAmount < 0 || horizontalAmount < 0) {
                movePage(amount);
                this.rebuildWidgets();
                return true;
            }
        }

        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        this.renderMenuBackground(context);

        context.fill(RenderPipelines.GUI, x, y, x + backgroundWidth, y + backgroundHeight, 0xFFDEDEDE);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        if (cv.results.isEmpty()) {
            String noResultsText = "Oops! There is nothing to see here :(";
            context.drawString(
                    font,
                    noResultsText,
                    x + backgroundWidth / 2 - font.width(noResultsText) / 2,
                    y + backgroundHeight / 2 - font.lineHeight / 2,
                    0xFF000000,
                    false
            );
        } else {
            MutableComponent mt = Component.empty()
                    .append(Component.literal("Showing results for: ").withStyle(ChatFormatting.BOLD))
                    .append(Component.literal(cv.commandUsed.replaceFirst("co (lookup|l) ", "")));
            context.drawString(font, mt, x + 4, y + 4, 0xFF000000, false);
        }

        String pageText = String.format("Page %d of %d", cv.currentPage + 1, pages);
        int pageTextWidth = font.width(pageText);
        int textX = (this.width - pageTextWidth) / 2;
        int textY = this.y + this.backgroundHeight - font.lineHeight / 2 - 10 - 4;

        context.drawString(font, pageText, textX, textY, 0xFF000000, false);

        if (this.resetButton.isHovered()) {
            context.setComponentTooltipForNextFrame(this.font, this.resetTooltipText, mouseX, mouseY);
        }

        if (this.previousPageButton.isHovered()) {
            context.setComponentTooltipForNextFrame(this.font, this.previousTooltipText, mouseX, mouseY);
        }

        if (this.nextPageButton.isHovered()) {
            context.setComponentTooltipForNextFrame(this.font, this.nextTooltipText, mouseX, mouseY);
        }
    }

    private void movePage(int amount) {
        CoVisualiserClient.getInstance().currentPage = Math.clamp(CoVisualiserClient.getInstance().currentPage + amount, 0, this.pages - 1);
    }

    protected void refreshCards() {
        CoVisualiserClient cv = CoVisualiserClient.getInstance();
        this.cards.clear();

        int startIndex = cv.currentPage * cardsPerPage;

        for (int i = 0; i < cardsPerPage; i++) {
            int resultIndex = startIndex + i;

            if (resultIndex >= cv.results.size()) {
                break;
            }

            int x = cardMarginHorizontal;
            int y = cardMarginTop;
            int cardMargin = cardGap;

            if (i % 2 == 1) {
                x += cardWidth + cardMargin;
            }

            y += (i >>> 1) * (cardHeight + cardMargin);

            CardWidget card = CardWidget.builder()
                    .position(this.x + x, this.y + y)
                    .size(cardWidth, cardHeight)
                    .index(resultIndex)
                    .result(cv.results.get(resultIndex))
                    .build();

            this.cards.add(card);
            this.addRenderableWidget(card);
        }
    }
}
