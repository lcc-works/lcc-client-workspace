package net.minecraft.client.gui.widget.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A borderless, left-aligned text button.
 *
 * <p>The widget occupies a full-width row rectangle so the whole strip is
 * clickable, and it paints a translucent bar behind the label while hovered.
 * That matters: with a bare label the clickable area is invisible, so the hitbox
 * has to be drawn for it to feel like it is where it looks like it is.</p>
 */
@OnlyIn(Dist.CLIENT)
public class FlatTextButton extends Button {

    /** Gap between the left edge of the row and the start of the label. */
    private static final int TEXT_INSET = 6;

    /** Translucent fill painted over the whole row while hovered. */
    private static final int HOVER_FILL = 0x28FFFFFF;

    /** Fill painted over an inactive row, so disabled still reads as a row. */
    private static final int DISABLED_FILL = 0x14000000;

    /** Height of one text line, used to centre the label in the row. */
    private static final int LINE_HEIGHT = 8;

    private final int normalColor;
    private final int hoverColor;
    private final boolean underlineOnHover;

    public FlatTextButton(int xIn, int yIn, int widthIn, int heightIn, String msg,
                          int normalColor, int hoverColor, boolean underlineOnHover,
                          IPressable onPress) {
        super(xIn, yIn, widthIn, heightIn, msg, onPress);
        this.normalColor = normalColor;
        this.hoverColor = hoverColor;
        this.underlineOnHover = underlineOnHover;
    }

    /** X position the label is drawn at, inset from the left of the row. */
    public int getTextX() {
        return this.x + TEXT_INSET;
    }

    /** Y position that vertically centres the label inside the row. */
    public int getTextY() {
        return this.y + (this.height - LINE_HEIGHT) / 2 + 1;
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = Minecraft.getInstance().fontRenderer;

        boolean hovered = this.isHovered();
        int alphaBits = MathHelper.ceil(MathHelper.clamp(this.alpha, 0.0F, 1.0F) * 255.0F) << 24;

        // Paint the whole row rather than just the glyphs, so the visible
        // highlight and the clickable area are the same rectangle.
        if (hovered || !this.active) {
            fill(this.x, this.y, this.x + this.width, this.y + this.height,
                    withAlpha(hovered ? HOVER_FILL : DISABLED_FILL, alphaBits));
        }

        int textX = getTextX();
        int textY = getTextY();

        int color = !this.active ? 0xFF909090 : (hovered ? this.hoverColor : this.normalColor);

        // Left accent bar marks the row the cursor is actually over.
        if (hovered) {
            fill(this.x, this.y, this.x + 2, this.y + this.height,
                    withAlpha(this.hoverColor & 0x00FFFFFF, alphaBits));
        }

        String text = this.getMessage();
        font.drawString(text, (float) textX, (float) textY, color | alphaBits);

        if (hovered && this.underlineOnHover) {
            int w = font.getStringWidth(text);
            fill(textX, textY + LINE_HEIGHT + 1, textX + w, textY + LINE_HEIGHT + 2,
                    withAlpha(this.hoverColor & 0x00FFFFFF, alphaBits));
        }
    }

    private static int withAlpha(int rgb, int alphaBits) {
        return (rgb & 0x00FFFFFF) | alphaBits;
    }
}