package dev.spzla.covisualiser.client.screen;

import dev.spzla.covisualiser.client.CoVisualiserClient;
import dev.spzla.covisualiser.client.LookupResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.realms.util.TextRenderingUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class LookupResultListScreen extends Screen {
    protected int x;
    protected int y;

    protected int backgroundWidth = 588;
    protected int backgroundHeight = 272;

    private int currentPage = 0;
    private final int cardsPerPage = 4;

    private final int shiftSkipPageMult = 10;

    protected List<CardWidget> cards = new ArrayList<>();

    private TextWidget pagesTextWidget;

    private int pages;

    public LookupResultListScreen() {
        super(Text.literal("Test"));
    }

    @Override
    protected void init() {
        this.clearChildren();
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonMargin = 4;

        this.pages = Math.max(1, (int)Math.ceil(cv.results.size() / (float)cardsPerPage));
        int lastPage = pages - 1;
        ButtonWidget previousPageButton = ButtonWidget.builder(Text.literal("PREVIOUS"), button -> {
            int pagesToSkip = -1;
            if (client.isShiftPressed()) pagesToSkip *= client.isCtrlPressed() ? currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.clearAndInit();
        })
                .position(this.x + this.backgroundWidth - 2 * (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        ButtonWidget nextPageButton = ButtonWidget.builder(Text.literal("NEXT"), button -> {
            int pagesToSkip = 1;
            if (client.isShiftPressed()) pagesToSkip *= client.isCtrlPressed() ? lastPage - currentPage : shiftSkipPageMult;
            movePage(pagesToSkip);
            this.clearAndInit();
        })
                .position(this.x + this.backgroundWidth - (buttonWidth + buttonMargin), y + this.backgroundHeight - (buttonHeight + buttonMargin))
                .width(buttonWidth)
                .build();

        if (this.currentPage == 0) {
            previousPageButton.active = false;
        }

        if (this.currentPage == lastPage) {
            nextPageButton.active = false;
        };

        this.refreshCards();
        this.addDrawableChild(previousPageButton);
        this.addDrawableChild(nextPageButton);
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

//        context.drawText(textRenderer, String.format("%d results", cv.results.size()), x, y, 0xFF000000, false);

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

        String pageText = String.format("Page %d of %d", currentPage + 1, pages);
        int pageTextWidth = textRenderer.getWidth(pageText);
        int textX = (this.width - pageTextWidth) / 2;
        int textY = this.y + this.backgroundHeight - textRenderer.fontHeight / 2 - 10 - 4;

        context.drawText(textRenderer, pageText, textX, textY, 0xFF000000, false);
    }

    private void movePage(int amount) {
        this.currentPage = Math.clamp(this.currentPage + amount, 0, this.pages - 1);
    }

    protected void refreshCards() {
        CoVisualiserClient cv = CoVisualiserClient.getInstance();
        this.cards.clear();

        int startIndex = currentPage * cardsPerPage;

        for (int i = 0; i < cardsPerPage; i++) {
            int resultIndex = startIndex + i;

            if (resultIndex >= cv.results.size()) {
                break;
            }

            int cardWidth = 288;
            int cardHeight = 112;
            int x = 4;
            int y = 16;
            int cardMargin = 4;

            if (i % 2 == 1) {
                x += cardWidth + cardMargin;
            }

            y += (i >>> 1) * (cardHeight + cardMargin);

            CardWidget card = CardWidget.builder()
                    .position(this.x + x, this.y + y)
                    .index(resultIndex)
                    .result(cv.results.get(resultIndex))
                    .build();

            this.cards.add(card);
            this.addDrawableChild(card);
        }
    }
}
