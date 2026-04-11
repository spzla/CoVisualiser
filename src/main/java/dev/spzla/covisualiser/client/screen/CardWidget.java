package dev.spzla.covisualiser.client.screen;

import dev.spzla.covisualiser.client.CoVisualiserClient;
import dev.spzla.covisualiser.client.Constants;
import dev.spzla.covisualiser.client.LookupResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CardWidget implements Drawable, Element, Selectable {
    protected int width;
    protected int height;
    private int x;
    private int y;
    private int index;
    private LookupResult lookupResult;

    private int innerLeft;
    private int innerTop;
    private int innerRight;
    private int innerBottom;

    private int right;
    private int bottom;

    private int innerPadding;
    private int outerPadding;

    private int borderWidth = 2;

    private ZonedDateTime zdt;

    private final ButtonWidget teleportButton;

    public CardWidget(int x, int y, int width, int height, int index, LookupResult lookupResult) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.index = index;
        this.lookupResult = lookupResult;

        this.right = this.x + this.width;
        this.bottom = this.y + this.height;

        this.innerLeft = this.x + this.borderWidth;
        this.innerTop = this.y + this.borderWidth;
        this.innerRight = this.right - this.borderWidth;
        this.innerBottom = this.bottom - this.borderWidth;

        this.outerPadding = 4;
        this.innerPadding = 4;

        zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(lookupResult.timestamp()), ZoneId.systemDefault());

        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonX = this.x + width - buttonWidth - borderWidth - 4;
        int buttonY = this.y + height - buttonHeight - borderWidth - 4;

        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        this.teleportButton = ButtonWidget.builder(Text.literal("TELEPORT"), button -> {
            cv.readIds.add(this.index);
            CoVisualiserClient.getInstance().sendCommand(
                    String.format("co tp %s %.2f %d %.2f",
                            this.lookupResult.worldId(),
                            this.lookupResult.x() + 0.5,
                            this.lookupResult.y(),
                            this.lookupResult.z() + 0.5
                    ));

            if (CoVisualiserClient.getConfig().closeUiOnTeleport) {
                MinecraftClient.getInstance().setScreen(null);
            }
        })
                .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        CoVisualiserClient cv = CoVisualiserClient.getInstance();

        int textColor = cv.readIds.contains(this.index) ? Constants.READ_COLOR : Constants.UNREAD_COLOR;

        context.fill(this.x, this.y, this.right, this.y + this.height, 0xFFAEAEAE);
        context.fill(this.innerLeft, this.innerTop, this.x + this.width - this.borderWidth, this.y + this.height - this.borderWidth, 0xFFDEDEDE);

//        int imageBoxSize = 100;
//        context.fill(innerX + 4, innerY + 4, innerX + 4 + imageBoxSize, innerY + 4 + imageBoxSize, 0xFF000000);

//        context.drawText(client.textRenderer, this.lookupResult.blockId(), innerLeft + 8, innerTop + 8, 0xFFFFFFFF, true);

        int line = 1;
        int lineHeight = client.textRenderer.fontHeight + this.innerPadding;
        //context.drawText(client.textRenderer, String.format("#%d", this.index + 1), innerX + imageBoxSize + 2 * 4, innerY + 2 * 4, 0xFF9E9E9E, false);
        context.drawText(client.textRenderer, String.format("#%d", this.index + 1), this.innerLeft + this.outerPadding, this.innerTop + this.outerPadding, textColor, false);
        String date = zdt.format(CoVisualiserClient.getInstance().dateFormatter);
        context.drawText(
                client.textRenderer, date, this.innerRight - client.textRenderer.getWidth(date) - this.innerPadding,
                innerTop + this.outerPadding, textColor, false);
        context.drawText(
                client.textRenderer,Text.literal(this.lookupResult.playerName()).formatted(Formatting.BOLD),
                this.innerLeft + this.outerPadding, innerTop + lineHeight * line++, textColor, false);
        context.drawText(
                client.textRenderer,
                String.format("XYZ: %d %d %d", this.lookupResult.x(), this.lookupResult.y(), this.lookupResult.z()),
                this.innerLeft + this.outerPadding, innerTop + lineHeight * line++, textColor, false);
        context.drawText(client.textRenderer, String.format("dim: %s", this.lookupResult.worldId()),
                this.innerLeft + this.outerPadding, innerTop + lineHeight * line++, textColor, false);
        context.drawText(client.textRenderer, String.format("Block: %s", this.lookupResult.blockId()),
                this.innerLeft + this.outerPadding, innerTop + lineHeight * line++, textColor, false);

        this.teleportButton.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width &&
               mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.teleportButton.mouseClicked(click, doubled)) return true;

        return false;
    }

    @Override
    public void setFocused(boolean focused) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }

    public static class Builder {
        private int x;
        private int y;
        private int width = 288;
        private int height = 112;
        private int index;
        private LookupResult lookupResult;

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder result(LookupResult lookupResult) {
            this.lookupResult = lookupResult;
            return this;
        }

        public CardWidget build() {
            return new CardWidget(this.x, this.y, this.width, this.height, this.index, this.lookupResult);
        }
    }
}
