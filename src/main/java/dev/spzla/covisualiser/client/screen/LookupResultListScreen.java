package dev.spzla.covisualiser.client.screen;

import dev.spzla.covisualiser.client.CoVisualiserClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

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

    private List<Text> resetTooltipText;
    private List<Text> previousTooltipText;
    private List<Text> nextTooltipText;

    private ButtonWidget resetButton;
    private ButtonWidget previousPageButton;
    private ButtonWidget nextPageButton;

    private int pages;

    public LookupResultListScreen() {
        super(Text.literal("Lookup Results List"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        this.calculateSizes();

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonMargin = 4;

        this.resetButton = ButtonWidget.builder(Text.literal("RESET"), button -> {
            cv.resetState();
            cv.results.clear();
            this.clearAndInit();
        })
                .position(this.x + buttonMargin, this.y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .size(buttonWidth, buttonHeight)
                .build();

        this.pages = Math.max(1, (int)Math.ceil(cv.results.size() / (float)cardsPerPage));
        int lastPage = pages - 1;

        this.resetTooltipText = List.of(
                Text.translatable("covisualiser.tooltip.reset")
        );

        this.previousTooltipText = List.of(
                Text.translatable("covisualiser.tooltip.skipnpages", shiftSkipPageMult),
                Text.translatable("covisualiser.tooltip.skiptofirst")
        );

        this.nextTooltipText = List.of(
                Text.translatable("covisualiser.tooltip.skipnpages", shiftSkipPageMult),
                Text.translatable("covisualiser.tooltip.skiptolast")
        );

        this.previousPageButton = ButtonWidget.builder(Text.literal("PREVIOUS"), button -> {
            int pagesToSkip = -1;
            if (client.isShiftPressed()) pagesToSkip *= client.isCtrlPressed() ? cv.currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.clearAndInit();
        })
                .position(this.x + this.backgroundWidth - 2 * (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        this.nextPageButton = ButtonWidget.builder(Text.literal("NEXT"), button -> {
            int pagesToSkip = 1;
            if (client.isShiftPressed()) pagesToSkip *= client.isCtrlPressed() ? lastPage - cv.currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.clearAndInit();
        })
                .position(this.x + this.backgroundWidth - (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        if (cv.currentPage == 0) {
            previousPageButton.active = false;
        }

        if (cv.currentPage == lastPage) {
            nextPageButton.active = false;
        }

        this.refreshCards();
        this.addDrawableChild(this.resetButton);
        this.addDrawableChild(this.previousPageButton);
        this.addDrawableChild(this.nextPageButton);
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
        if (client.isShiftPressed()) {
            int amount = client.isCtrlPressed() ? this.shiftSkipPageMult : 1;
            if (verticalAmount > 0 || horizontalAmount > 0) {
                movePage(-amount);
                this.clearAndInit();
                return true;
            } else if (verticalAmount < 0 || horizontalAmount < 0) {
                movePage(amount);
                this.clearAndInit();
                return true;
            }
        }

        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderDarkening(context);

        context.fill(RenderPipelines.GUI, x, y, x + backgroundWidth, y + backgroundHeight, 0xFFDEDEDE);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        if (cv.results.isEmpty()) {
            String noResultsText = "Oops! There is nothing to see here :(";
            context.drawText(
                    textRenderer,
                    noResultsText,
                    x + backgroundWidth / 2 - textRenderer.getWidth(noResultsText) / 2,
                    y + backgroundHeight / 2 - textRenderer.fontHeight / 2,
                    0xFF000000,
                    false
            );
        } else {
            MutableText mt = Text.empty()
                    .append(Text.literal("Showing results for: ").formatted(Formatting.BOLD))
                    .append(Text.literal(cv.commandUsed.replaceFirst("co (lookup|l) ", "")));
            context.drawText(textRenderer, mt, x + 4, y + 4, 0xFF000000, false);
        }

        String pageText = String.format("Page %d of %d", cv.currentPage + 1, pages);
        int pageTextWidth = textRenderer.getWidth(pageText);
        int textX = (this.width - pageTextWidth) / 2;
        int textY = this.y + this.backgroundHeight - textRenderer.fontHeight / 2 - 10 - 4;

        context.drawText(textRenderer, pageText, textX, textY, 0xFF000000, false);

        if (this.resetButton.isHovered()) {
            context.drawTooltip(this.textRenderer, this.resetTooltipText, mouseX, mouseY);
        }

        if (this.previousPageButton.isHovered()) {
            context.drawTooltip(this.textRenderer, this.previousTooltipText, mouseX, mouseY);
        }

        if (this.nextPageButton.isHovered()) {
            context.drawTooltip(this.textRenderer, this.nextTooltipText, mouseX, mouseY);
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
            this.addDrawableChild(card);
        }
    }
}
