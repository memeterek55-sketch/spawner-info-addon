package com.example.spawneraddon.modules;

import com.example.spawneraddon.data.DebrisData;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AncientDebrisModule extends Module {

    private final SettingGroup sgGenel = settings.getDefaultGroup();
    private final SettingGroup sgRadar = settings.createGroup("Radar");
    private final SettingGroup sgESP   = settings.createGroup("ESP");

    // ── GENEL ─────────────────────────────────────────────
    private final Setting<Integer> radarMesafe = sgGenel.add(new IntSetting.Builder()
        .name("radar-mesafe")
        .description("Radar icin X,Z yaricapi (blok).")
        .defaultValue(256).min(32).sliderMax(2048).build());

    private final Setting<Integer> espMesafe = sgGenel.add(new IntSetting.Builder()
        .name("esp-mesafe")
        .description("ESP icin X,Z yaricapi. Chunk yuklu olmali.")
        .defaultValue(64).min(8).sliderMax(512).build());

    private final Setting<Integer> taramaAraligi = sgGenel.add(new IntSetting.Builder()
        .name("tarama-araligi-tick")
        .description("Kac tickte bir ESP bloklari dogrulansin (20=1sn).")
        .defaultValue(40).min(10).sliderMax(200).build());

    // ── RADAR ─────────────────────────────────────────────
    private final Setting<Boolean> radarAktif = sgRadar.add(new BoolSetting.Builder()
        .name("radar-aktif").description("Oval 2D radar'i gosterir/gizler.")
        .defaultValue(true).build());

    private final Setting<Integer> radarGenislik = sgRadar.add(new IntSetting.Builder()
        .name("radar-genislik").description("Oval radar yatay yaricapi (piksel).")
        .defaultValue(110).min(40).sliderMax(300).build());

    private final Setting<Integer> radarYukseklik = sgRadar.add(new IntSetting.Builder()
        .name("radar-yukseklik").description("Oval radar dikey yaricapi (piksel).")
        .defaultValue(75).min(30).sliderMax(300).build());

    private final Setting<Integer> konumX = sgRadar.add(new IntSetting.Builder()
        .name("konum-x").description("Sagdan offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600).build());

    private final Setting<Integer> konumY = sgRadar.add(new IntSetting.Builder()
        .name("konum-y").description("Usttan offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600).build());

    private final Setting<Integer> noktaBoyutu = sgRadar.add(new IntSetting.Builder()
        .name("nokta-boyutu").description("Radar nokta boyutu (piksel, yuvarlak).")
        .defaultValue(4).min(1).sliderMax(10).build());

    private final Setting<SettingColor> arkaplanRengi = sgRadar.add(new ColorSetting.Builder()
        .name("arkaplan").defaultValue(new SettingColor(0, 0, 0, 130)).build());

    private final Setting<SettingColor> cemberRengi = sgRadar.add(new ColorSetting.Builder()
        .name("cember-rengi").defaultValue(new SettingColor(255, 255, 255, 200)).build());

    private final Setting<SettingColor> ortaHatRengi = sgRadar.add(new ColorSetting.Builder()
        .name("orta-hat-rengi").defaultValue(new SettingColor(255, 255, 255, 55)).build());

    private final Setting<SettingColor> radarNoktaRengi = sgRadar.add(new ColorSetting.Builder()
        .name("debris-nokta-rengi").defaultValue(new SettingColor(255, 140, 0, 230)).build());

    private final Setting<SettingColor> oyuncuRengi = sgRadar.add(new ColorSetting.Builder()
        .name("oyuncu-nokta-rengi").defaultValue(new SettingColor(0, 255, 100, 255)).build());

    // ── ESP ───────────────────────────────────────────────
    private final Setting<Boolean> espAktif = sgESP.add(new BoolSetting.Builder()
        .name("esp-aktif").description("3D ESP kutularini gosterir/gizler.")
        .defaultValue(true).build());

    private final Setting<ShapeMode> sekil = sgESP.add(new EnumSetting.Builder<ShapeMode>()
        .name("sekil-modu").description("Dolgu+cizgi, sadece cizgi veya sadece dolgu.")
        .defaultValue(ShapeMode.Both).build());

    private final Setting<SettingColor> espDolguRengi = sgESP.add(new ColorSetting.Builder()
        .name("dolgu-rengi").defaultValue(new SettingColor(255, 140, 0, 40)).build());

    private final Setting<SettingColor> espKenarRengi = sgESP.add(new ColorSetting.Builder()
        .name("kenar-rengi").defaultValue(new SettingColor(255, 140, 0, 220)).build());

    // ── DURUM ─────────────────────────────────────────────
    /** Chunk'ta gercekten Ancient Debris olan dogrulanmis pozisyonlar */
    private final List<BlockPos> dogrulanmis = new ArrayList<>();
    private int tickSayac = 0;

    private static final Color tmpSide = new Color();
    private static final Color tmpLine = new Color();

    public AncientDebrisModule() {
        super(Categories.Render, "ancient-debris",
            "497k Ancient Debris koordinatini radar+ESP ile gosterir. ESP sadece yuklu chunk'ta gercekten var olan bloklari gosterir.");
        DebrisData.load();
    }

    @Override
    public void onActivate() {
        DebrisData.load();
        dogrulanmis.clear();
        tickSayac = 0;
        dogrula();
    }

    @Override
    public void onDeactivate() {
        dogrulanmis.clear();
    }

    // ── TICK: DOGRULAMA ───────────────────────────────────
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (++tickSayac >= taramaAraligi.get()) {
            tickSayac = 0;
            dogrula();
        }
    }

    /**
     * DebrisData'dan ESP mesafesindeki X,Y,Z'leri alir.
     * Chunk yuklusse gercek blogu kontrol eder: Ancient Debris ise listeye ekler.
     * Chunk yuklu degilse o koordinati atlar (gosterme).
     */
    private void dogrula() {
        dogrulanmis.clear();
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        List<int[]> yakin = DebrisData.getNearby(px, pz, espMesafe.get());

        BlockPos.Mutable mut = new BlockPos.Mutable();
        for (int[] c : yakin) {
            int chunkX = c[0] >> 4;
            int chunkZ = c[2] >> 4;
            // Chunk yuklu degilse gosterme
            if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;
            // Blok gercekten Ancient Debris mi?
            mut.set(c[0], c[1], c[2]);
            if (mc.world.getBlockState(mut).getBlock() == Blocks.ANCIENT_DEBRIS) {
                dogrulanmis.add(mut.toImmutable());
            }
        }
    }

    // ── RENDER 3D: ESP ────────────────────────────────────
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!espAktif.get() || mc.player == null) return;
        tmpSide.set(espDolguRengi.get());
        tmpLine.set(espKenarRengi.get());
        for (BlockPos pos : dogrulanmis) {
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                tmpSide, tmpLine, sekil.get(), 0);
        }
    }

    // ── RENDER 2D: RADAR ──────────────────────────────────
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!radarAktif.get() || mc.player == null) return;

        DrawContext ctx = event.drawContext;
        int sw  = mc.getWindow().getScaledWidth();
        int rx  = radarGenislik.get();
        int ry  = radarYukseklik.get();
        int cx  = sw - konumX.get() - rx;
        int cy  = konumY.get() + ry;

        fillEllipse(ctx, cx, cy, rx, ry, arkaplanRengi.get().getPacked());

        int oc = ortaHatRengi.get().getPacked();
        ctx.fill(cx - rx, cy, cx + rx, cy + 1, oc);
        ctx.fill(cx, cy - ry, cx + 1, cy + ry, oc);

        drawEllipseOutline(ctx, cx, cy, rx, ry, 2, cemberRengi.get().getPacked());

        double px      = mc.player.getX();
        double pz      = mc.player.getZ();
        int    rMesafe = radarMesafe.get();
        int    nc      = radarNoktaRengi.get().getPacked();
        int    ns      = noktaBoyutu.get();

        List<int[]> radarKoord = DebrisData.getNearby(px, pz, rMesafe);
        for (int[] c : radarKoord) {
            double dx   = c[0] + 0.5 - px;
            double dz   = c[2] + 0.5 - pz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double oran = Math.min(dist / rMesafe, 1.0);
            double aci  = Math.atan2(dz, dx);

            int dotX = cx + (int)(Math.cos(aci) * oran * (rx - ns - 1));
            int dotY = cy + (int)(Math.sin(aci) * oran * (ry - ns - 1));

            // oval sinir kontrolu
            double ex = (double)(dotX - cx) / rx;
            double ez = (double)(dotY - cy) / ry;
            if (ex * ex + ez * ez <= 1.0) {
                fillRoundDot(ctx, dotX, dotY, ns, nc);
            }
        }

        // Oyuncu merkezi — tam daire
        fillRoundDot(ctx, cx, cy, 5, oyuncuRengi.get().getPacked());

        String bilgi = "Radar:" + radarKoord.size() + "  ESP:" + dogrulanmis.size();
        ctx.drawText(mc.textRenderer, bilgi,
            cx - mc.textRenderer.getWidth(bilgi) / 2,
            cy + ry + 3, 0xFFFFAA00, true);
    }

    // ── YARDIMCI ──────────────────────────────────────────

    /** Dolu elips (oval arka plan) */
    private void fillEllipse(DrawContext ctx, int cx, int cy, int rx, int ry, int color) {
        for (int x = -rx; x <= rx; x++) {
            double t = (double) x / rx;
            int h = (int)(Math.sqrt(Math.max(0.0, 1.0 - t * t)) * ry);
            ctx.fill(cx + x, cy - h, cx + x + 1, cy + h + 1, color);
        }
    }

    /** Elips cercevesi */
    private void drawEllipseOutline(DrawContext ctx, int cx, int cy, int rx, int ry, int thick, int color) {
        int segs = 240;
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            int x = cx + (int)(Math.cos(a) * rx);
            int y = cy + (int)(Math.sin(a) * ry);
            ctx.fill(x - thick / 2, y - thick / 2, x + thick / 2 + 1, y + thick / 2 + 1, color);
        }
    }

    /**
     * Yuvarlak nokta: her satirda genisligi |dy| ye gore daraltan daire yaklasimlari.
     * Kucuk boyutlarda dikdortgenden cok daha yuvarlak gorunur.
     */
    private void fillRoundDot(DrawContext ctx, int cx, int cy, int size, int color) {
        int r = Math.max(1, size / 2);
        double r2 = (double) r * r;
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt(Math.max(0.0, r2 - (double) dy * dy));
            if (w == 0) continue;
            ctx.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }
}
