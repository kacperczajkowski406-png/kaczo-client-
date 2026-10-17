package pl.kaczo.hud;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Nowoczesny wygląd menu gry w samym kodzie (bez texture packa): „szklane” przyciski, logo na środku ekranu
 * tytułowego i animowane wejście. Wszystko do wyłączenia w ustawieniach moda.
 */
public final class Glass {
    private Glass() { }

    private static final Identifier LOGO = Identifier.of("kaczohud", "textures/gui/logo.png");
    /** Przyciski namalowane przez nas w ostatniej klatce – tylko im mixin wyłącza oryginalną grafikę. */
    private static final Map<Object, Long> SEEN = new WeakHashMap<>();
    private static long introStart = -1;
    private static boolean introPlayed = false;

    public static boolean buttonsOn() { return Modules.GENERAL.bool("glassbtn"); }
    public static boolean logoOn() { return Modules.GENERAL.bool("titlelogo"); }

    /** Używane przez mixin: czy oryginalny przycisk ma nie rysować swojej grafiki. */
    public static boolean hideVanilla(Object widget) {
        if (!buttonsOn()) return false;
        Long t = SEEN.get(widget);
        return t != null && System.currentTimeMillis() - t < 400;
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen && !introPlayed && Modules.GENERAL.bool("intro")) {
                introStart = System.currentTimeMillis();
                introPlayed = true;
            }
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                try { paint(client, s, ctx, mx, my); } catch (Throwable ignored) { /* wygląd nie może wywrócić gry */ }
            });
        });
    }

    private static float intro() {
        if (introStart < 0) return 1f;
        float p = (System.currentTimeMillis() - introStart) / 1300f;
        if (p >= 1f) { introStart = -1; return 1f; }
        return p;
    }

    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); float u = 1 - t; return 1 - u * u * u; }

    private static int fade(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static void paint(MinecraftClient mc, Screen screen, DrawContext ctx, int mx, int my) {
        float p = screen instanceof TitleScreen ? intro() : 1f;
        if (screen instanceof TitleScreen && logoOn()) logo(mc, screen, ctx, p);
        if (!buttonsOn()) return;
        List<ClickableWidget> btns = Screens.getButtons(screen);
        long now = System.currentTimeMillis();
        int i = 0;
        for (ClickableWidget b : btns) {
            if (b.getClass() != ButtonWidget.class || !b.visible) continue;
            SEEN.put(b, now);
            float k = ease((p - 0.25f - i * 0.07f) / 0.5f);
            i++;
            if (k <= 0) continue;
            int x = b.getX(), y = b.getY() + Math.round((1 - k) * 10), w = b.getWidth(), h = b.getHeight();
            boolean hover = b.active && Ui.in(mx, my, b.getX(), b.getY(), w, h);
            int base = !b.active ? 0x22FFFFFF : hover ? 0x70FFFFFF : 0x42FFFFFF;
            int g = hover ? 1 : 0;
            Ui.glass(ctx, x - g, y - g, w + 2 * g, h + 2 * g, Math.min(8, h / 2), fade(base, k));
            Text msg = b.getMessage();
            int tw = mc.textRenderer.getWidth(msg);
            ctx.drawTextWithShadow(mc.textRenderer, msg, x + (w - tw) / 2, y + (h - 8) / 2, fade(b.active ? 0xFFFFFFFF : 0xFF9AA3B5, k));
        }
    }

    private static void logo(MinecraftClient mc, Screen screen, DrawContext ctx, float p) {
        int size = Math.max(24, Math.round(64 * Modules.GENERAL.num("logosize") / 100f * 1.1f));
        float k = ease(p / 0.6f);
        int cx = screen.width / 2, cy = screen.height / 4 + 24 - Math.round((1 - k) * 24);
        int y = cy - size / 2;
        // delikatna poświata, pulsująca w tle
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0);
        int ga = (int) ((18 + 14 * pulse) * k);
        Ui.rrect(ctx, cx - size / 2 - 8, y - 8, size + 16, size + 16, (size + 16) / 2, (ga << 24) | 0xFFE14A);
        float s = size / 64f * (0.55f + 0.45f * k);
        int px = Math.round(cx - 32 * s), py = Math.round(cy - 32 * s);
        Compat.push(ctx);
        Compat.translate(ctx, px, py);
        Compat.scale(ctx, s, s);
        Compat.tex(ctx, LOGO, 64, 64);
        Compat.pop(ctx);
    }
}
