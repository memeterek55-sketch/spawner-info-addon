package com.example.spawneraddon.modules;

import com.example.spawneraddon.data.DebrisData;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class AncientDebrisRadar extends Module {

    private final SettingGroup sgGenel   = settings.getDefaultGroup();
    private final SettingGroup sgGorsel  = settings.createGroup("Gorsel");
    private final SettingGroup sgRenkler = settings.createGroup("Renkler");

    // --- Genel ---
    private final Setting<Integer> renderMesafe = sgGenel.add(new IntSetting.Builder()
        .name("render-mesafe")
        .description("Blok cinsinden arama yaricapi.")
        .defaultValue(128).min(16).sliderMax(512)
        .build());

    // --- Gorsel ---
    private final Setting<Integer> radarGenislik = sgGorsel.add(new IntSetting.Builder()
        .name("radar-genislik")
        .description("Oval radar yatay yaricapi (piksel).")
        .defaultValue(110).min(40).sliderMax(300)
        .build());

    private final Setting<Integer> radarYukseklik = sgGorsel.add(new IntSetting.Builder()
        .name("radar-yukseklik")
        .description("Oval radar dikey yaricapi (piksel).")
        .defaultValue(80).min(30).sliderMax(300)
        .build());

    private final Setting<Integer> konumX = sgGorsel.add(new IntSetting.Builder()
        .name("konum-x")
        .description("Sagdan yatay offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600)
        .build());

    private final Setting<Integer> konumY = sgGorsel.add(new IntSetting.Builder()
        .name("konum-y")
        .description("Usttan dikey offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600)
        .build());

    private final Setting<Integer> noktaBoyutu = sgGorsel.add(new IntSetting.Builder()
        .name("nokta-boyutu")
        .description("Debris noktasi piksel boyutu.")
        .defaultValue(3).min(1).sliderMax(8)
        .build());

    // --- Renkler ---
    private final Setting<SettingColor> arkaplanRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("arkaplan")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .build());

    private final Setting<SettingColor> cemberRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("cember-rengi")
        .defaultValue(new SettingColor(255, 255, 255, 200))
        .build());

    private final Setting<SettingColor> ortaHatRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("orta-hat-rengi")
        .defaultValue(new SettingColor(255, 255, 255, 60))
        .build());

    private final Setting<SettingColor> noktaRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("debris-nokta-rengi")
        .defaultValue(new SettingColor(255, 140, 0, 230))
        .build());

    private final Setting<SettingColor> oyuncuRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("oyuncu-nokta-rengi")
        .defaultValue(new SettingColor(0, 255, 100, 255))
        .build());

    public AncientDebrisRadar() {
        super(Categories.Render, "ancient-debris-radar",
            "Ancient Debris konumlarini oval radar'da gosterir.");
        DebrisData.load();
    }

    @Override
    public void onActivate() {
        DebrisData.load();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        DrawContext ctx = event.drawContext;
        int sw = mc.getWindow().getScaledWidth();
        int rx = radarGenislik.get();
        int ry = radarYukseklik.get();

        // Sag ust kose merkezi
        int cx = sw - konumX.get() - rx;
        int cy = konumY.get() + ry;

        // Arkaplan oval
        fillEllipse(ctx, cx, cy, rx, ry, arkaplanRengi.get().getPacked());

        // Orta hatlar
        int oc = ortaHatRengi.get().getPacked();
        ctx.fill(cx - rx, cy, cx + rx, cy + 1, oc);
        ctx.fill(cx, cy - ry, cx + 1, cy + ry, oc);

        // Oval cerceve
        drawEllipseOutline(ctx, cx, cy, rx, ry, 2, cemberRengi.get().getPacked());

        // Debris noktalari
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int maxDist = renderMesafe.get();
        int nc = noktaRengi.get().getPacked();
        int ns = noktaBoyutu.get();

        List<int[]> yakin = DebrisData.getNearby(px, pz, maxDist);
        for (int[] coord : yakin) {
            double dx = coord[0] - px;
            double dz = coord[1] - pz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double oran = Math.min(dist / maxDist, 1.0);
            double aci  = Math.atan2(dz, dx);

            int dotX = cx + (int)(Math.cos(aci) * oran * (rx - ns - 1));
            int dotY = cy + (int)(Math.sin(aci) * oran * (ry - ns - 1));

            // oval siniri asma kontrolu
            double ex = (double)(dotX - cx) / rx;
            double ez = (double)(dotY - cy) / ry;
            if (ex * ex + ez * ez <= 1.0) {
                fillDot(ctx, dotX, dotY, ns, nc);
            }
        }

        // Oyuncu merkez noktasi
        fillDot(ctx, cx, cy, 4, oyuncuRengi.get().getPacked());
    }

    private void fillEllipse(DrawContext ctx, int cx, int cy, int rx, int ry, int color) {
        for (int x = -rx; x <= rx; x++) {
            double t = (double) x / rx;
            int h = (int)(Math.sqrt(1.0 - t * t) * ry);
            ctx.fill(cx + x, cy - h, cx + x + 1, cy + h + 1, color);
        }
    }

    private void drawEllipseOutline(DrawContext ctx, int cx, int cy, int rx, int ry, int thick, int color) {
        int segs = 240;
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            int x = cx + (int)(Math.cos(a) * rx);
            int y = cy + (int)(Math.sin(a) * ry);
            ctx.fill(x - thick / 2, y - thick / 2, x + thick / 2 + 1, y + thick / 2 + 1, color);
        }
    }

    private void fillDot(DrawContext ctx, int x, int y, int size, int color) {
        int h = size / 2;
        ctx.fill(x - h, y - h, x + h + 1, y + h + 1, color);
    }
}
