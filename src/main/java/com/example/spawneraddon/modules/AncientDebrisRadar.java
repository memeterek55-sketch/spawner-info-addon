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

    private final SettingGroup sgGenel    = settings.getDefaultGroup();
    private final SettingGroup sgGorsel   = settings.createGroup("Gorsel");
    private final SettingGroup sgRenkler  = settings.createGroup("Renkler");

    // --- Genel ---
    private final Setting<Integer> renderMesafe = sgGenel.add(new IntSetting.Builder()
        .name("render-mesafe")
        .description("Blok cinsinden arama yaricapi.")
        .defaultValue(128)
        .min(16)
        .sliderMax(512)
        .build()
    );

    // --- Gorsel ---
    private final Setting<Integer> radarBoyutu = sgGorsel.add(new IntSetting.Builder()
        .name("radar-boyutu")
        .description("Radar dairesinin ekran piksel yaricapi.")
        .defaultValue(100)
        .min(40)
        .sliderMax(250)
        .build()
    );

    private final Setting<Integer> konumX = sgGorsel.add(new IntSetting.Builder()
        .name("konum-x")
        .description("Ekran sagindaki yatay offset (piksel).")
        .defaultValue(12)
        .min(4)
        .sliderMax(600)
        .build()
    );

    private final Setting<Integer> konumY = sgGorsel.add(new IntSetting.Builder()
        .name("konum-y")
        .description("Ekran ustunden dikey offset (piksel).")
        .defaultValue(12)
        .min(4)
        .sliderMax(600)
        .build()
    );

    private final Setting<Integer> noktaBoyutu = sgGorsel.add(new IntSetting.Builder()
        .name("nokta-boyutu")
        .description("Her debris noktasinin piksel boyutu.")
        .defaultValue(3)
        .min(1)
        .sliderMax(8)
        .build()
    );

    // --- Renkler ---
    private final Setting<SettingColor> arkaplanRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("arkaplan")
        .description("Radar arka plan dolgu rengi.")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .build()
    );

    private final Setting<SettingColor> cemberRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("cember-rengi")
        .description("Radar cevresi rengi.")
        .defaultValue(new SettingColor(255, 255, 255, 200))
        .build()
    );

    private final Setting<SettingColor> ortaHattaRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("orta-hat-rengi")
        .description("Orta yatay/dikey cizgilerin rengi.")
        .defaultValue(new SettingColor(255, 255, 255, 60))
        .build()
    );

    private final Setting<SettingColor> noktaRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("debris-nokta-rengi")
        .description("Ancient debris noktalarinin rengi.")
        .defaultValue(new SettingColor(255, 140, 0, 230))
        .build()
    );

    private final Setting<SettingColor> oyuncuRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("oyuncu-nokta-rengi")
        .description("Merkezdeki oyuncu noktasinin rengi.")
        .defaultValue(new SettingColor(0, 255, 100, 255))
        .build()
    );

    public AncientDebrisRadar() {
        super(Categories.Render, "ancient-debris-radar",
            "Ancient Debris konumlarini dairesel radar'da gosterir.");
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
        int r  = radarBoyutu.get();

        // Sag ust kose - konumX sagdan, konumY usttan
        int cx = sw - konumX.get() - r;
        int cy = konumY.get() + r;

        // Arkaplan dolgusu
        fillCircle(ctx, cx, cy, r, arkaplanRengi.get().getPacked());

        // Orta yardimci hatlar (crosshair)
        int oc = ortaHattaRengi.get().getPacked();
        ctx.fill(cx - r, cy, cx + r, cy + 1, oc);
        ctx.fill(cx, cy - r, cx + 1, cy + r, oc);

        // Cember cercevesi (3 piksel kalinlik)
        drawCircleOutline(ctx, cx, cy, r, 2, cemberRengi.get().getPacked());

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
            double oran  = Math.min(dist / maxDist, 1.0);
            double aci   = Math.atan2(dz, dx);

            int dotX = cx + (int)(Math.cos(aci) * oran * (r - ns - 1));
            int dotY = cy + (int)(Math.sin(aci) * oran * (r - ns - 1));

            fillDot(ctx, dotX, dotY, ns, nc);
        }

        // Oyuncu merkez noktasi
        fillDot(ctx, cx, cy, 4, oyuncuRengi.get().getPacked());
    }

    // Yardimci: Dolu daire cizer
    private void fillCircle(DrawContext ctx, int cx, int cy, int radius, int color) {
        for (int x = -radius; x <= radius; x++) {
            int h = (int) Math.sqrt((double) radius * radius - (double) x * x);
            ctx.fill(cx + x, cy - h, cx + x + 1, cy + h + 1, color);
        }
    }

    // Yardimci: Daire cercevesi cizer
    private void drawCircleOutline(DrawContext ctx, int cx, int cy, int radius, int thick, int color) {
        int segs = 180;
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            int x = cx + (int)(Math.cos(a) * radius);
            int y = cy + (int)(Math.sin(a) * radius);
            ctx.fill(x - thick / 2, y - thick / 2, x + thick / 2 + 1, y + thick / 2 + 1, color);
        }
    }

    // Yardimci: Kucuk nokta cizer
    private void fillDot(DrawContext ctx, int x, int y, int size, int color) {
        int h = size / 2;
        ctx.fill(x - h, y - h, x + h + 1, y + h + 1, color);
    }
}
