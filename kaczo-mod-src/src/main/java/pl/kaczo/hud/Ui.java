package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Wspólne kolory i elementy w stylu „liquid glass”: zaokrąglone, półprzezroczyste, z jasną krawędzią. */
public class Ui {
    public static final int DIM = 0x99050810, PANEL = 0xD0121826, CARD = 0x55FFFFFF, CARD_OFF = 0x26FFFFFF,
            BTN = 0x38FFFFFF, BTN_HOVER = 0x62FFFFFF, ORANGE = 0xFFFFC21A, GREEN = 0xE03DBE6A, GREEN_HOVER = 0xF050D47C,
            WHITE = 0xFFFFFFFF, GRAY = 0xFFA9B2C6, RED = 0xFFFF5C63, YELLOW = 0xFFFFE14A;

    public static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami (promień r, kolor ARGB). */
    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    /** „Szkło”: półprzezroczysta tafla z jasną krawędzią i refleksem u góry. */
    public static void glass(DrawContext ctx, int x, int y, int w, int h, int r, int fill) {
        rrect(ctx, x, y, w, h, r, fill);
        int c = 0x40FFFFFF;                                           // jasna krawędź
        ctx.fill(x + r, y, x + w - r, y + 1, c);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, 0x20FFFFFF);
        ctx.fill(x, y + r, x + 1, y + h - r, c);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, 0x20FFFFFF);
        if (h > 8 && w > 2 * r + 4) ctx.fill(x + r, y + 1, x + w - r, y + 2, 0x30FFFFFF); // refleks
    }

    /** Tło elementów HUD zależne od globalnego stylu (Glass / Flat). */
    public static void box(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (color == 0) return;
        Module g = Modules.GENERAL;
        if (g.num("style") == 1) { ctx.fill(x, y, x + w, y + h, color); return; }
        int r = g.num("radius");
        rrect(ctx, x, y + 1, w, h, r, 0x26000000);                       // miękki cień pod taflą
        rrect(ctx, x, y, w, h, r, color);
        if (h > 6) rrect(ctx, x + 1, y + 1, w - 2, Math.max(2, h / 2 - 1), Math.max(0, r - 1), 0x12FFFFFF); // połysk górnej połowy
        if (r > 0 && w > 2 * r) ctx.fill(x + r, y, x + w - r, y + 1, 0x3CFFFFFF);                       // jasna górna krawędź
        if (r > 0 && w > 2 * r) ctx.fill(x + r, y + h - 1, x + w - r, y + h, 0x28000000);               // ciemna dolna krawędź
        if (g.bool("glassborder")) {
            // delikatna jasna obwódka rysowana jako cienkie ramki wzdłuż prostych krawędzi
            int c = 0x30FFFFFF;
            ctx.fill(x + r, y, x + w - r, y + 1, c);
            ctx.fill(x + r, y + h - 1, x + w - r, y + h, c);
            ctx.fill(x, y + r, x + 1, y + h - r, c);
            ctx.fill(x + w - 1, y + r, x + w, y + h - r, c);
        }
    }

    public static void button(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, int bg, int fg) {
        boolean solid = (bg >>> 24) >= 0xF0;
        if (solid) { rrect(ctx, x, y, w, h, 4, bg); }
        else glass(ctx, x, y, w, h, 4, bg);
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, fg, false);
    }

    public static void centered(DrawContext ctx, MinecraftClient mc, String s, int cx, int y, int color) {
        ctx.drawText(mc.textRenderer, s, cx - mc.textRenderer.getWidth(s) / 2, y, color, false);
    }
}
