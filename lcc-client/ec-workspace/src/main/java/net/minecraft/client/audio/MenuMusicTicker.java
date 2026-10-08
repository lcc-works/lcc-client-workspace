package net.minecraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Looping background music for the menu screens (main menu, multiplayer and
 * singleplayer).
 *
 * <p>Vanilla's {@link MusicTicker} only runs while a world is loaded, so the
 * front end was silent. This plays one fixed track through the sound handler and
 * keeps exactly one instance alive across screen changes.</p>
 *
 * <p>The track is {@code assets/minecraft/sounds/music/menu/menu.ogg}, wired up
 * by the {@code music.menu.custom} entry in {@code sounds.json}. Swap the file
 * to change the music; no code change needed.</p>
 */
@OnlyIn(Dist.CLIENT)
public class MenuMusicTicker {

    private static final ResourceLocation MENU_MUSIC = new ResourceLocation("music.menu.custom");

    private static ISound playing;
    private static boolean wasPlaying;

    /**
     * The screens that should have music. Checked by class name suffix so this
     * stays valid for the TeaVM, LWJGL and JS targets alike.
     */
    public static boolean isMenuScreen(Object screen) {
        if (screen == null) {
            return false;
        }

        String name = screen.getClass().getName();
        return name.endsWith("MainMenuScreen")
                || name.endsWith("MultiplayerScreen")
                || name.endsWith("WorldSelectionScreen")
                || name.endsWith("GuiSocialInfoScreen")
                || name.endsWith("GuiSocialLoginScreen");
    }

    /**
     * Called every client tick. Starts the track when a menu screen is showing
     * and stops it when one is not.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        // Never fight the in-world music.
        if (mc.player != null || mc.world != null) {
            stop();
            return;
        }

        boolean shouldPlay = isMenuScreen(mc.currentScreen);

        if (shouldPlay && !wasPlaying) {
            play(mc);
        } else if (!shouldPlay && wasPlaying) {
            stop();
        }

        wasPlaying = shouldPlay;
    }

    private static void play(Minecraft mc) {
        if (mc.getSoundHandler() == null || playing != null) {
            return;
        }

        playing = new SimpleSound(
                MENU_MUSIC,
                SoundCategory.MUSIC,
                0.55F,
                1.0F,
                false,
                0,
                ISound.AttenuationType.NONE,
                0.0F,
                0.0F,
                0.0F,
                true
        );

        mc.getSoundHandler().play(playing);
    }

    public static void stop() {
        SoundHandler handler = Minecraft.getInstance().getSoundHandler();
        if (playing != null && handler != null) {
            handler.stop(playing);
        }
        playing = null;
        wasPlaying = false;
    }
}