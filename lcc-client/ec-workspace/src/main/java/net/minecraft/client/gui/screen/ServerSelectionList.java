package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ExtendedList<ServerSelectionList.Entry> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation field_214359_c = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation field_214360_d = new ResourceLocation("textures/gui/server_selection.png");
    private final MultiplayerScreen owner;
    private final List<ServerSelectionList.NormalEntry> serverListInternet = Lists.newArrayList();
    private final ServerSelectionList.Entry lanScanEntry = new ServerSelectionList.LanScanEntry();

    public ServerSelectionList(MultiplayerScreen ownerIn, Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        this.owner = ownerIn;
        // MultiplayerScreen draws the menu background itself; the inherited dirt
        // backdrop would cover the entire viewport and hide it.
        this.setDrawBackdrop(false);
    }

    private void func_195094_h() {
        this.clearEntries();
        this.serverListInternet.forEach(this::addEntry);
        this.addEntry(this.lanScanEntry);
    }

    public void setSelected(ServerSelectionList.Entry p_setSelected_1_) {
        super.setSelected(p_setSelected_1_);
        if (this.getSelected() instanceof ServerSelectionList.NormalEntry) {
            NarratorChatListener.INSTANCE.func_216864_a((new TranslationTextComponent("narrator.select", ((ServerSelectionList.NormalEntry) this.getSelected()).server.serverName)).getString());
        }

    }

    protected void moveSelection(int p_moveSelection_1_) {
        int i = this.children().indexOf(this.getSelected());
        int j = MathHelper.clamp(i + p_moveSelection_1_, 0, this.getItemCount() - 1);
        ServerSelectionList.Entry serverselectionlist$entry = this.children().get(j);
        super.setSelected(serverselectionlist$entry);
        if (serverselectionlist$entry instanceof ServerSelectionList.LanScanEntry) {
            if (p_moveSelection_1_ <= 0 || j != this.getItemCount() - 1) {
                if (p_moveSelection_1_ >= 0 || j != 0) {
                    this.moveSelection(p_moveSelection_1_);
                }
            }
        } else {
            this.ensureVisible(serverselectionlist$entry);
            this.owner.func_214295_b();
        }
    }

    public void updateOnlineServers(ServerList p_148195_1_) {
        this.serverListInternet.clear();

        for (int i = 0; i < p_148195_1_.countServers(); ++i) {
            this.serverListInternet.add(new ServerSelectionList.NormalEntry(this.owner, p_148195_1_.getServerData(i)));
        }

        this.func_195094_h();
    }

    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 30;
    }

    public int getRowWidth() {
        return super.getRowWidth() + 85;
    }

    protected boolean isFocused() {
        return this.owner.getFocused() == this;
    }

    /**
     * Whether the row at the given index should show the selection highlight.
     *
     * <p>A themed entry fills the whole selection box, so it hides the highlight
     * the list drew and has to draw the border itself instead. Mirrors the
     * condition {@code AbstractList.renderList} uses.</p>
     */
    private boolean isRowSelected(int index) {
        return this.renderSelection && index >= 0 && index < this.getItemCount() && this.isSelectedItem(index);
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ExtendedList.AbstractListEntry<ServerSelectionList.Entry> {
    }

    @OnlyIn(Dist.CLIENT)
    public static class LanScanEntry extends ServerSelectionList.Entry {
        private final Minecraft mc = Minecraft.getInstance();

        public void render(int p_render_1_, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int p_render_6_, int p_render_7_, boolean p_render_8_, float p_render_9_) {
            int i = p_render_2_ + p_render_5_ / 2 - 9 / 2;
            this.mc.fontRenderer.drawString(I18n.format("lanServer.scanning"), (float) (this.mc.currentScreen.width / 2 - this.mc.fontRenderer.getStringWidth(I18n.format("lanServer.scanning")) / 2), (float) i, 16777215);
            String s;
            switch ((int) (Util.milliTime() / 300L % 4L)) {
                case 0:
                default:
                    s = "O o o";
                    break;
                case 1:
                case 3:
                    s = "o O o";
                    break;
                case 2:
                    s = "o o O";
            }

            this.mc.fontRenderer.drawString(s, (float) (this.mc.currentScreen.width / 2 - this.mc.fontRenderer.getStringWidth(s) / 2), (float) (i + 9), 8421504);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class NormalEntry extends ServerSelectionList.Entry {
        private final MultiplayerScreen owner;
        private final Minecraft mc;
        private final ServerData server;
        private final ResourceLocation serverIcon;
        private String lastIconB64;
        private DynamicTexture icon;
        private long lastClickTime;
        /** Lazily parsed from the MOTD marker; null when the server has no theme. */
        private ServerMotdTheme theme;
        /** Decoded theme logo, distinct from the vanilla favicon. */
        private DynamicTexture themeIcon;

        protected NormalEntry(MultiplayerScreen p_i50669_2_, ServerData p_i50669_3_) {
            this.owner = p_i50669_2_;
            this.server = p_i50669_3_;
            this.mc = Minecraft.getInstance();
            this.serverIcon = new ResourceLocation("servers/" + Integer.toHexString(p_i50669_3_.serverIP.hashCode()) + "/icon");
            this.icon = (DynamicTexture) this.mc.getTextureManager().getTexture(this.serverIcon);
        }

        public void render(int p_render_1_, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int p_render_6_, int p_render_7_, boolean p_render_8_, float p_render_9_) {
            if (!this.server.pinged) {
                this.server.pinged = true;
                this.server.pingToServer = -2L;
                this.server.serverMOTD = "";
                this.server.populationInfo = "";
                try {
                    this.owner.getOldServerPinger().ping(this.server);
                } catch (Exception var3) {
                    this.server.pingToServer = -1L;
                    this.server.serverMOTD = TextFormatting.DARK_RED + I18n.format("multiplayer.status.cannot_connect");
                }

            }

            boolean flag = this.server.version > SharedConstants.getVersion().getProtocolVersion();
            boolean flag1 = this.server.version < SharedConstants.getVersion().getProtocolVersion();
            boolean flag2 = flag || flag1;

            // A server may request its own theme through a hidden marker on the
            // MOTD. When present we draw a styled card instead of the flat row.
            ServerMotdTheme theme = this.theme;
            if (theme == null && this.server.pinged) {
                this.theme = theme = ServerMotdTheme.parse(this.server.serverMOTD);
            }

            // The theme logo is optional, so fall back to the vanilla favicon (the
            // server icon, which EaglerXServer serves as the 64x64 "icon" field).
            // That way the logo shows whether it came from the theme marker or
            // from server_icon.png, and the text never overlaps it.
            boolean hasThemeLogo = theme != null && theme.showLogo && theme.getLogoData() != null;
            boolean drawsLogo = hasThemeLogo || this.server.iconTextureObject != null;
            int textInset = theme != null
                    ? (hasThemeLogo ? THEME_LOGO_SIZE + 10 : (drawsLogo ? 32 + 3 : 8))
                    : 32 + 3;
            int textX = p_render_3_ + textInset;
            int nameColor = theme != null ? 0xFFFFFFFF : 16777215;

            if (theme != null) {
                // The card paints over the row the list highlighted, so it needs
                // to know whether it is selected in order to redraw that border.
                renderThemeCard(this.mc, theme, this, p_render_3_, p_render_2_, p_render_4_, p_render_5_, textX, nameColor, flag2,
                        isRowSelected(p_render_1_), ServerSelectionList.this.isFocused());
            }

            this.mc.fontRenderer.drawString(this.server.serverName, (float) textX, (float) (p_render_2_ + 1), nameColor);

            String motdForDisplay = this.server.serverMOTD;
            if (theme != null) {
                // Strip the marker so the raw JSON never reaches the screen.
                motdForDisplay = stripThemeMarker(motdForDisplay);
            }

            boolean showMotdLine = theme == null || theme.showMotd;
            // The themed card has no vanilla 32px icon, so the wrap width has to
            // start from the text inset instead, and leave room for the ping
            // bars drawn at the right edge.
            int motdWrapWidth = theme != null
                    ? p_render_4_ - textInset - 17
                    : p_render_4_ - 32 - 2;
            List<String> list = showMotdLine
                    ? this.mc.fontRenderer.listFormattedStringToWidth(motdForDisplay, motdWrapWidth)
                    : java.util.Collections.<String>emptyList();

            for (int i = 0; i < Math.min(list.size(), 2); ++i) {
                this.mc.fontRenderer.drawString(list.get(i), (float) textX, (float) (p_render_2_ + 12 + 9 * i), 8421504);
            }

            String s2 = flag2 ? TextFormatting.DARK_RED + this.server.gameVersion : this.server.populationInfo;
            int j = this.mc.fontRenderer.getStringWidth(s2);
            this.mc.fontRenderer.drawString(s2, (float) (p_render_3_ + p_render_4_ - j - 15 - 2), (float) (p_render_2_ + 1), 8421504);
            int k = 0;
            String s = null;
            int l;
            String s1;
            if (flag2) {
                l = 5;
                s1 = I18n.format(flag ? "multiplayer.status.client_out_of_date" : "multiplayer.status.server_out_of_date");
                s = this.server.playerList;
            } else if (this.server.pinged && this.server.pingToServer != -2L) {
                if (this.server.pingToServer < 0L) {
                    l = 5;
                } else if (this.server.pingToServer < 150L) {
                    l = 0;
                } else if (this.server.pingToServer < 300L) {
                    l = 1;
                } else if (this.server.pingToServer < 600L) {
                    l = 2;
                } else if (this.server.pingToServer < 1000L) {
                    l = 3;
                } else {
                    l = 4;
                }

                if (this.server.pingToServer < 0L) {
                    s1 = I18n.format("multiplayer.status.no_connection");
                } else {
                    s1 = this.server.pingToServer + "ms";
                    s = this.server.playerList;
                }
            } else {
                k = 1;
                l = (int) (Util.milliTime() / 100L + (long) (p_render_1_ * 2) & 7L);
                if (l > 4) {
                    l = 8 - l;
                }

                s1 = I18n.format("multiplayer.status.pinging");
            }

            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            AbstractGui.blit(p_render_3_ + p_render_4_ - 15, p_render_2_, (float) (k * 10), (float) (176 + l * 8), 10, 8, 256, 256);

            // The theme card already painted the themed logo, so only draw the vanilla
            // favicon when it did not. Unthemed entries keep the vanilla
            // behaviour, including the "no icon" placeholder.
            if (!hasThemeLogo) {
                if (this.server.iconTextureObject != null) {
                    this.drawTextureAt(p_render_3_, p_render_2_, this.server.iconResourceLocation);
                } else if (theme == null) {
                    this.drawTextureAt(p_render_3_, p_render_2_, ServerSelectionList.field_214359_c);
                }
            }

            int i1 = p_render_6_ - p_render_3_;
            int j1 = p_render_7_ - p_render_2_;
            if (i1 >= p_render_4_ - 15 && i1 <= p_render_4_ - 5 && j1 >= 0 && j1 <= 8) {
                this.owner.setHoveringText(s1);
            } else if (i1 >= p_render_4_ - j - 15 - 2 && i1 <= p_render_4_ - 15 - 2 && j1 >= 0 && j1 <= 8) {
                this.owner.setHoveringText(s);
            }

            if (this.mc.gameSettings.touchscreen || p_render_8_) {
                this.mc.getTextureManager().bindTexture(ServerSelectionList.field_214360_d);
                AbstractGui.fill(p_render_3_, p_render_2_, p_render_3_ + 32, p_render_2_ + 32, -1601138544);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int k1 = p_render_6_ - p_render_3_;
                int l1 = p_render_7_ - p_render_2_;
                if (this.canJoin()) {
                    if (k1 < 32 && k1 > 16) {
                        AbstractGui.blit(p_render_3_, p_render_2_, 0.0F, 32.0F, 32, 32, 256, 256);
                    } else {
                        AbstractGui.blit(p_render_3_, p_render_2_, 0.0F, 0.0F, 32, 32, 256, 256);
                    }
                }

                if (p_render_1_ > 0) {
                    if (k1 < 16 && l1 < 16) {
                        AbstractGui.blit(p_render_3_, p_render_2_, 96.0F, 32.0F, 32, 32, 256, 256);
                    } else {
                        AbstractGui.blit(p_render_3_, p_render_2_, 96.0F, 0.0F, 32, 32, 256, 256);
                    }
                }

                if (p_render_1_ < this.owner.getServerList().countServers() - 1) {
                    if (k1 < 16 && l1 > 16) {
                        AbstractGui.blit(p_render_3_, p_render_2_, 64.0F, 32.0F, 32, 32, 256, 256);
                    } else {
                        AbstractGui.blit(p_render_3_, p_render_2_, 64.0F, 0.0F, 32, 32, 256, 256);
                    }
                }
            }
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();

        }

        protected void drawTextureAt(int p_178012_1_, int p_178012_2_, ResourceLocation p_178012_3_) {
            this.mc.getTextureManager().bindTexture(p_178012_3_);
            GlStateManager.enableBlend();
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(p_178012_1_, p_178012_2_, 0.0F, 0.0F, 32, 32, 32, 32);
            GlStateManager.disableBlend();
        }

        private boolean canJoin() {
            return true;
        }

        /**
         * Removes the hidden theme marker so raw MOTD JSON is never shown to the
         * player. Falls back to the original string if it is not our JSON.
         *
         * <p>Handles MOTDs the query joined with {@code \n}: only the lines that
         * actually carry a marker are rewritten, the rest pass through.</p>
         */
        private static String stripThemeMarker(String motd) {
            if (motd == null) {
                return "";
            }

            // Fast path: not our JSON at all, so nothing to strip.
            if (motd.indexOf("eagler_theme") < 0) {
                return motd;
            }

            // Whole string FIRST. The generator pretty-prints the component, so
            // it spans many lines but is still a single JSON object. Trying each
            // line on its own matched nothing, left `changed` false, and dumped
            // the raw `{"text": ...}` on screen.
            String whole = stripOne(motd);
            if (whole != null) {
                return whole;
            }

            // Otherwise the query joined several MOTD lines. Rewrite only the
            // lines carrying a marker and leave the rest untouched.
            String nl = motd.indexOf("\r\n") >= 0 ? "\r\n" : "\n";
            String[] lines = motd.split("\r\n|\n", -1);

            boolean changed = false;
            for (int i = 0; i < lines.length; i++) {
                String stripped = stripOne(lines[i]);
                if (stripped != null) {
                    lines[i] = stripped;
                    changed = true;
                }
            }

            return changed ? String.join(nl, lines) : motd;
        }

        /** The 16 legacy colours, as packed 0xRRGGBB. Mirrors TextFormatting's table. */
        private static final int[] LEGACY_COLORS = {
                0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
                0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
                0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };

        /**
         * The 16 legacy colours by the names {@code TextFormatting} resolves,
         * matching {@link #LEGACY_COLORS} index for index.
         *
         * <p>The deserializer looks colours up by <em>name</em>, so emitting the
         * section code ({@code "6"}) instead of the name ({@code "gold"}) makes it
         * throw outright.</p>
         */
        private static final String[] LEGACY_NAMES = {
                "black", "dark_blue", "dark_green", "dark_aqua",
                "dark_red", "dark_purple", "gold", "gray",
                "dark_gray", "blue", "green", "aqua",
                "red", "light_purple", "yellow", "white"
        };

        /**
         * Maps a 0xRRGGBB value onto the nearest legacy colour.
         *
         * <p>1.14 has no hex colours - {@code TextFormatting} only knows the 16
         * codes - so a component carrying {@code "color": "#a0a0ad"} makes the
         * stock deserializer throw outright. Rewriting it to the nearest legacy
         * name first is what lets the real parser handle everything else.</p>
         */
        private static String nearestLegacyColorName(int rgb) {
            int best = 0;
            int bestDist = Integer.MAX_VALUE;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            for (int i = 0; i < LEGACY_COLORS.length; i++) {
                int c = LEGACY_COLORS[i];
                int dr = r - ((c >> 16) & 0xFF);
                int dg = g - ((c >> 8) & 0xFF);
                int db = b - (c & 0xFF);
                int dist = dr * dr + dg * dg + db * db;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = i;
                }
            }

            return LEGACY_NAMES[best];
        }

        /**
         * Rewrites every {@code "#rrggbb"} colour in the tree to the equivalent
         * legacy name, so the stock chat-component parser accepts it. Unknown
         * keys such as {@code eagler_theme} are left untouched - GSON ignores
         * them, which is the whole point.
         */
        private static void normalizeHexColors(com.google.gson.JsonElement el) {
            if (el == null || el.isJsonNull()) {
                return;
            }

            if (el.isJsonArray()) {
                for (com.google.gson.JsonElement child : el.getAsJsonArray()) {
                    normalizeHexColors(child);
                }
                return;
            }

            if (!el.isJsonObject()) {
                return;
            }

            com.google.gson.JsonObject obj = el.getAsJsonObject();

            com.google.gson.JsonElement colour = obj.get("color");
            if (colour != null && colour.isJsonPrimitive() && colour.getAsJsonPrimitive().isString()) {
                String s = colour.getAsString();
                if (s.length() == 7 && s.charAt(0) == '#') {
                    try {
                        int rgb = (int) Long.parseLong(s.substring(1), 16);
                        obj.addProperty("color", nearestLegacyColorName(rgb));
                    } catch (NumberFormatException ignored) {
                        // Leave an unparseable value for the parser to reject.
                    }
                }
            }

            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : obj.entrySet()) {
                normalizeHexColors(e.getValue());
            }
        }

        /**
         * Last-resort text extraction: walks the standard {@code text} and
         * {@code extra} fields and ignores everything else.
         *
         * <p>This exists so that a MOTD we cannot format still renders as its
         * words. Returning the raw JSON here would be the one outcome the whole
         * exercise is trying to avoid, so every failure path ends up here.</p>
         */
        private static String plainTextFallback(com.google.gson.JsonElement el) {
            StringBuilder sb = new StringBuilder();

            if (el == null || el.isJsonNull()) {
                return "";
            }

            if (el.isJsonArray()) {
                for (com.google.gson.JsonElement child : el.getAsJsonArray()) {
                    sb.append(plainTextFallback(child));
                }
                return sb.toString();
            }

            if (!el.isJsonObject()) {
                return "";
            }

            com.google.gson.JsonObject obj = el.getAsJsonObject();

            com.google.gson.JsonElement text = obj.get("text");
            if (text != null && text.isJsonPrimitive() && text.getAsJsonPrimitive().isString()) {
                sb.append(text.getAsString());
            }

            com.google.gson.JsonElement extra = obj.get("extra");
            if (extra != null && extra.isJsonArray()) {
                sb.append(plainTextFallback(extra));
            }

            return sb.toString();
        }

        /**
         * Renders a MOTD that carries a theme marker as a normal formatted string.
         *
         * <p>{@code eagler_theme} is treated purely as extension metadata: it
         * stays in the JSON and goes through the ordinary chat-component
         * deserializer, which ignores keys it does not know. Everything a client
         * is meant to show - {@code text}, {@code extra}, colours, bold - comes
         * back through the standard path, so an unknown field can never turn the
         * whole MOTD into a dumped JSON object.</p>
         *
         * @return the formatted MOTD text, or null if this line has no marker
         */
        private static String stripOne(String line) {
            String trimmed = line.trim();
            if (trimmed.length() < 2 || trimmed.charAt(0) != '{') {
                return null;
            }

            com.google.gson.JsonObject obj;
            try {
                com.google.gson.JsonElement root = new com.google.gson.JsonParser().parse(trimmed);
                if (!root.isJsonObject()) {
                    return null;
                }
                obj = root.getAsJsonObject();
            } catch (Throwable t) {
                return null;
            }

            if (!obj.has("eagler_theme")) {
                return null;
            }

            try {
                // 1.14 has no hex colours, so map them onto legacy names first.
                normalizeHexColors(obj);

                // 2. Parse as an ordinary chat component. GSON drops the unknown
                //    eagler_theme key by itself, so the standard fields are read
                //    exactly as they would be on any server.
                ITextComponent component = ITextComponent.Serializer.fromJson(obj);
                if (component != null) {
                    // 3. getFormattedText() walks siblings and emits the § codes
                    //    for each style, preserving per-component colour and bold.
                    String formatted = component.getFormattedText();
                    if (formatted != null) {
                        return formatted;
                    }
                }
            } catch (Throwable t) {
                // Malformed marker or component: fall through to plain text
                // rather than showing the raw JSON.
                LOGGER.debug("Themed MOTD component would not parse, using plain text");
            }

            return plainTextFallback(obj);
        }

        /**
         * Drawn size of a themed entry's logo. The list rows are only 36px tall,
         * so this has to stay at 32 (or less) or the logo overflows the row and
         * collides with the neighbouring entry.
         */
        private static final int THEME_LOGO_SIZE = 32;

        /**
         * Gradient is rendered into a small texture and stretched over the card.
         * That keeps an arbitrary angle cheap: without it, a rotated gradient
         * would need one fill per row per pixel column.
         */
        private static final int GRADIENT_TEX = 64;

        private static DynamicTexture gradientTexture;
        private static int gradientKey;
        private static final ResourceLocation GRADIENT_LOCATION =
                new ResourceLocation("eagler", "gui/server_theme_gradient.png");

        /**
         * Builds (or reuses) the gradient texture for a theme.
         */
        private static DynamicTexture getGradientTexture(ServerMotdTheme theme) {
            int key = theme.getGradientKey();
            if (gradientTexture != null && gradientKey == key) {
                return gradientTexture;
            }

            double rad = Math.toRadians(theme.angle);
            double ndx = Math.cos(rad);
            double ndy = Math.sin(rad);

            // Normalise against the projection range of the whole square, not just
            // by dividing by |dx|+|dy|. The corners are where the extremes of
            // (x*dx + y*dy) sit, so this makes t reach exactly 0 and 1 at opposite
            // corners - the same thing CSS linear-gradient does. Dividing by
            // |dx|+|dy| instead left t topping out around 0.85, so the gradient
            // never actually reached bg.
            double p00 = 0.0 * ndx + 0.0 * ndy;
            double p10 = 1.0 * ndx + 0.0 * ndy;
            double p01 = 0.0 * ndx + 1.0 * ndy;
            double p11 = 1.0 * ndx + 1.0 * ndy;
            double min = Math.min(Math.min(p00, p10), Math.min(p01, p11));
            double max = Math.max(Math.max(p00, p10), Math.max(p01, p11));
            double span = max - min;
            if (span <= 0.0) {
                span = 1.0;
            }

            NativeImage img = new NativeImage(GRADIENT_TEX, GRADIENT_TEX, false);
            for (int y = 0; y < GRADIENT_TEX; y++) {
                for (int x = 0; x < GRADIENT_TEX; x++) {
                    double px = (x + 0.5) / GRADIENT_TEX;
                    double py = (y + 0.5) / GRADIENT_TEX;

                    // Distance along the gradient axis, 0 at one corner.
                    double t = (px * ndx + py * ndy - min) / span;
                    t = Math.max(0.0, Math.min(1.0, t));

                    img.setPixelRGBA(x, y, lerpColor(theme.accentDark | 0xFF000000, theme.bg | 0xFF000000, (float) t));
                }
            }

            try {
                if (gradientTexture == null) {
                    gradientTexture = new DynamicTexture(img);
                } else {
                    gradientTexture.setTextureData(img);
                    gradientTexture.updateDynamicTexture();
                }
                Minecraft.getInstance().getTextureManager().loadTexture(GRADIENT_LOCATION, gradientTexture);
                gradientKey = key;
                return gradientTexture;
            } catch (Throwable t) {
                LOGGER.error("Failed to build server theme gradient", t);
                return null;
            }
        }

        /**
         * Paints the themed card behind a server entry: background, gradient and
         * the server's logo if it supplied one. The card spans the row's full
         * selection box, so a selected row also gets its border redrawn on top.
         */
        private static void renderThemeCard(Minecraft mc, ServerMotdTheme theme,
                                           NormalEntry entry, int x, int y, int width,
                                           int height, int textX, int nameColor,
                                           boolean versionMismatch, boolean selected,
                                           boolean focused) {
            // The card covers the row's selection box exactly. The list frames
            // that box from (x - 2, y - 1) to (x + width - 2, y + height + 2) -
            // see AbstractList.renderList - so an inset card left a 2px gap on
            // every side. Rows sit 36px apart and the card is 35px tall, which
            // keeps a 1px gap between stacked cards so neighbouring entries do
            // not bleed into one solid block.
            int cardX = x - 2;
            int cardY = y - 1;
            int cardW = width;
            int cardH = height + 3;

            int bg = 0xFF000000 | theme.bg;

            AbstractGui.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);

            if (theme.gradient) {
                DynamicTexture grad = getGradientTexture(theme);
                if (grad != null) {
                    mc.getTextureManager().bindTexture(GRADIENT_LOCATION);
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    // Stretch the whole 64x64 texture across the card. The source
                    // rect has to stay at GRADIENT_TEX: passing the card size
                    // here made the UVs run past 1.0 (cardW/64 is about 3), and
                    // GL_REPEAT then tiled the gradient three times across the
                    // entry instead of showing one smooth ramp.
                    AbstractGui.blit(cardX, cardY, cardW, cardH,
                            0.0F, 0.0F, GRADIENT_TEX, GRADIENT_TEX,
                            GRADIENT_TEX, GRADIENT_TEX);
                }
            }

            // No accent bar: it rendered as a stray coloured block in front of the logo
            // slot, which is usually empty because most servers send no logo.

            // Subtle top highlight for depth.
            AbstractGui.fill(cardX, cardY, cardX + cardW, cardY + 1, 0x18FFFFFF);

            // Version mismatch still reads clearly on the themed card.
            if (versionMismatch) {
                AbstractGui.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0xFFB02020);
            }

            if (theme.showLogo && theme.getLogoData() != null) {
                DynamicTexture logo = entry.getThemeIcon(theme);
                if (logo != null) {
                    int size = THEME_LOGO_SIZE;
                    // Sit the logo just inside the card's left edge rather than at
                    // a fixed offset from the row. At x+12 a 32px logo ended at
                    // x+44 while the text starts at x+42 (THEME_LOGO_SIZE + 10),
                    // so the two actually overlapped. Measured from the card it
                    // ends at x+34 and leaves a clean 8px gap before the text.
                    int lx = cardX + 4;
                    int ly = cardY + (cardH - size) / 2;
                    mc.getTextureManager().bindTexture(entry.themeIconLocation);
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    // Source rect must be the full 64x64 texture, not the drawn size.
                    // Passing `size` (40) here capped the UVs at 40/64, so only the top
                    // left 62% of the logo was drawn, stretched.
                    AbstractGui.blit(lx, ly, size, size, 0.0F, 0.0F, 64, 64, 64, 64);
                }
            }

            // AbstractList draws the selection frame before the entry renders, so
            // the card has just covered it. Redraw it here, matching vanilla: a 1px
            // white frame, dimmed to half while the list does not have focus.
            if (selected) {
                int border = focused ? 0xFFFFFFFF : 0x80FFFFFF;
                AbstractGui.fill(cardX, cardY, cardX + cardW, cardY + 1, border);
                AbstractGui.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, border);
                AbstractGui.fill(cardX, cardY, cardX + 1, cardY + cardH - 1, border);
                AbstractGui.fill(cardX + cardW - 1, cardY, cardX + cardW, cardY + cardH - 1, border);
            }
        }

        private static int lerpColor(int a, int b, float t) {
            int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg2 = (b >> 8) & 0xFF, bb = b & 0xFF;
            int r = (int) (ar + (br - ar) * t);
            int g = (int) (ag + (bg2 - ag) * t);
            int bl = (int) (ab + (bb - ab) * t);
            int al = (int) (aa + (ba - aa) * t);
            return (al << 24) | (r << 16) | (g << 8) | bl;
        }

        private ResourceLocation themeIconLocation;

        private DynamicTexture getThemeIcon(ServerMotdTheme theme) {
            String data = theme.getLogoData();
            if (data == null) {
                return null;
            }

            if (data.equals(this.lastIconB64) && this.themeIcon != null) {
                return this.themeIcon;
            }

            if (this.themeIconLocation == null) {
                this.themeIconLocation = new ResourceLocation("servers/"
                        + Integer.toHexString(this.server.serverIP.hashCode()) + "/themeicon");
            }

            try {
                // Decode the base64 ourselves. NativeImage.func_216511_b does NOT
                // take base64 in this fork - it wraps the string in a VFile2 and
                // reads it as a resource path, so handing it a base64 blob threw,
                // got swallowed by the catch below, and the logo never appeared.
                byte[] png = net.lax1dude.eaglercraft.Base64.decodeBase64(data);
                NativeImage img = NativeImage.read(new java.io.ByteArrayInputStream(png));
                if (this.themeIcon == null) {
                    this.themeIcon = new DynamicTexture(img);
                } else {
                    this.themeIcon.setTextureData(img);
                    this.themeIcon.updateDynamicTexture();
                }
                this.mc.getTextureManager().loadTexture(this.themeIconLocation, this.themeIcon);
                this.lastIconB64 = data;
                return this.themeIcon;
            } catch (Throwable t) {
                LOGGER.error("Invalid theme logo for server {}", this.server.serverIP, t);
                this.lastIconB64 = data;
                return null;
            }
        }

        private void prepareServerIcon() {
            String s = this.server.getBase64EncodedIconData();
            if (s == null) {
                this.mc.getTextureManager().deleteTexture(this.serverIcon);
                if (this.icon != null && this.icon.getTextureData() != null) {
                    this.icon.getTextureData().close();
                }

                this.icon = null;
            } else {
                try {
                    NativeImage nativeimage = NativeImage.func_216511_b(s);
                    Validate.validState(nativeimage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(nativeimage.getHeight() == 64, "Must be 64 pixels high");
                    if (this.icon == null) {
                        this.icon = new DynamicTexture(nativeimage);
                    } else {
                        this.icon.setTextureData(nativeimage);
                        this.icon.updateDynamicTexture();
                    }

                    this.mc.getTextureManager().loadTexture(this.serverIcon, this.icon);
                } catch (Throwable throwable) {
                    ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", this.server.serverName, this.server.serverIP, throwable);
                    this.server.setBase64EncodedIconData((String) null);
                }
            }

        }

        public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
            double d0 = p_mouseClicked_1_ - (double) ServerSelectionList.this.getRowLeft();
            double d1 = p_mouseClicked_3_ - (double) ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
            if (d0 <= 32.0D) {
                if (d0 < 32.0D && d0 > 16.0D && this.canJoin()) {
                    this.owner.func_214287_a(this);
                    this.owner.connectToSelected();
                    return true;
                }

                int i = this.owner.serverListSelector.children().indexOf(this);
                if (d0 < 16.0D && d1 < 16.0D && i > 0) {
                    int k = Screen.hasShiftDown() ? 0 : i - 1;
                    this.owner.getServerList().swapServers(i, k);
                    if (this.owner.serverListSelector.getSelected() == this) {
                        this.owner.func_214287_a(this);
                    }

                    this.owner.serverListSelector.updateOnlineServers(this.owner.getServerList());
                    return true;
                }

                if (d0 < 16.0D && d1 > 16.0D && i < this.owner.getServerList().countServers() - 1) {
                    ServerList serverlist = this.owner.getServerList();
                    int j = Screen.hasShiftDown() ? serverlist.countServers() - 1 : i + 1;
                    serverlist.swapServers(i, j);
                    if (this.owner.serverListSelector.getSelected() == this) {
                        this.owner.func_214287_a(this);
                    }

                    this.owner.serverListSelector.updateOnlineServers(serverlist);
                    return true;
                }
            }

            this.owner.func_214287_a(this);
            if (Util.milliTime() - this.lastClickTime < 250L) {
                this.owner.connectToSelected();
            }

            this.lastClickTime = Util.milliTime();
            return false;
        }

        public ServerData getServerData() {
            return this.server;
        }
    }
}
