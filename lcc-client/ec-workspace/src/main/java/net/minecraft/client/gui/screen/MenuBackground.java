package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The static menu background, shared by every full-screen menu so they all show
 * the same artwork instead of the tiled dirt texture.
 *
 * <p>Replace
 * {@code assets/minecraft/textures/gui/title/background/custom.png} to change
 * it. Any size works; a high-resolution source such as 2560x1440 looks best
 * because it is stretched to the whole viewport.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class MenuBackground {

    /** Same file the title screen has always used. */
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/title/background/custom.png");

    /**
     * Blit source size, and also the texture-size divisor handed to blit.
     *
     * <p>{@code AbstractGui.blit} computes its UVs as
     * {@code (u + srcW) / texW}, so passing the same value for both cancels out
     * and yields UVs of exactly 0..1 - i.e. the whole texture, whatever its real
     * pixel dimensions. Only relative values matter here, not the actual size of
     * the image.</p>
     */
    private static final int FULL_TEXTURE = 512;

    private MenuBackground() {
    }

    /**
     * Draws the background stretched to fill the viewport.
     *
     * @param alpha 0..1, used by the title screen's fade-in
     */
    public static void draw(int width, int height, float alpha) {
        Minecraft mc = Minecraft.getInstance();

        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        float a = Math.max(0.0F, Math.min(1.0F, alpha));
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, a);

        mc.getTextureManager().bindTexture(TEXTURE);
        // Sample the whole texture and stretch it. The source size here must
        // equal the divisor, or maxU runs past 1.0 and GL_REPEAT tiles the image
        // instead of scaling it - which is what happened when the viewport width
        // was passed as the source size on wide windows (960/512 = 1.9).
        AbstractGui.blit(0, 0, width, height, 0.0F, 0.0F,
                FULL_TEXTURE, FULL_TEXTURE, FULL_TEXTURE, FULL_TEXTURE);

        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws the background plus a flat dark wash over it.
     *
     * <p>Menus with text lists need this: the artwork is bright at the top
     * (sky), and unselected server rows are transparent, so white labels over
     * raw artwork are hard to read.</p>
     *
     * @param scrim 0xRRGGBBAA, or 0 for none
     */
    public static void drawWithScrim(int width, int height, int scrim) {
        draw(width, height, 1.0F);
        if (scrim != 0) {
            AbstractGui.fill(0, 0, width, height, scrim);
        }
    }
}