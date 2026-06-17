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
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public class AncientDebrisModule extends Module {

    // ==================== AYAR GRUPLARI ====================
    private final SettingGroup sgGenel = settings.getDefaultGroup();
    private final SettingGroup sgRadar = settings.createGroup("Radar");
    private final SettingGroup sgESP   = settings.createGroup("ESP");

    // ==================== GENEL ====================
    private final Setting<Integer> radarMesafe = sgGenel.add(new IntSetting.Builder()
        .name("radar-mesafe")
        .description("Radar icin koordinat arama yaricapi (blok). Buyuk olabilir, sadece 2D gosterim.")
        .defaultValue(256).min(32).sliderMax(1024)
        .build());

    private final Setting<Integer> espMesafe = sgGenel.add(new IntSetting.Builder()
        .name("esp-mesafe")
        .description("ESP icin koordinat arama yaricapi. Chunk yuklu olmali, kucuk tutun.")
        .defaultValue(64).min(8).sliderMax(256)
        .build());

    private final Setting<Integer> taramaAraligi = sgGenel.add(new IntSetting.Builder()
        .name("tarama-araligi-tick")
        .description("Kac tickte bir ESP bloklari yeniden taransin (20=1sn).")
        .defaultValue(40).min(10).sliderMax(200)
        .build());

    // ==================== RADAR ====================
    private final Setting<Boolean> radarAktif = sgRadar.add(new BoolSetting.Builder()
        .name("radar-aktif")
        .description("Oval 2D radar'i gosterir/gizler.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> radarGenislik = sgRadar.add(new IntSetting.Builder()
        .name("radar-genislik")
        .description("Oval radar yatay yaricapi (piksel).")
        .defaultValue(110).min(40).sliderMax(300)
        .build());

    private final Setting<Integer> radarYukseklik = sgRadar.add(new IntSetting.Builder()
        .name("radar-yukseklik")
        .description("Oval radar dikey yaricapi (piksel).")
        .defaultValue(75).min(30).sliderMax(300)
        .build());

    private final Setting<Integer> konumX = sgRadar.add(new IntSetting.Builder()
        .name("konum-x").description("Sagdan offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600)
        .build());

    private final Setting<Integer> konumY = sgRadar.add(new IntSetting.Builder()
        .name("konum-y").description("Usttan offset (piksel).")
        .defaultValue(12).min(4).sliderMax(600)
        .build());

    private final Setting<Integer> noktaBoyutu = sgRadar.add(new IntSetting.Builder()
        .name("nokta-boyutu").description("Radar nokta boyutu (piksel).")
        .defaultValue(3).min(1).sliderMax(8)
        .build());

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

    // ==================== ESP ====================
    private final Setting<Boolean> espAktif = sgESP.add(new BoolSetting.Builder()
        .name("esp-aktif")
        .description("3D ESP kutularini gosterir/gizler.")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> sekil = sgESP.add(new EnumSetting.Builder<ShapeMode>()
        .name("sekil-modu")
        .description("Dolgu+cizgi, sadece cizgi veya sadece dolgu.")
        .defaultValue(ShapeMode.Both)
        .build());

    private final Setting<SettingColor> espDolguRengi = sgESP.add(new ColorSetting.Builder()
        .name("dolgu-rengi").defaultValue(new SettingColor(255, 140, 0, 40)).build());

    private final Setting<SettingColor> espKenarRengi = sgESP.add(new ColorSetting.Builder()
        .name("kenar-rengi").defaultValue(new SettingColor(255, 140, 0, 220)).build());

    // ==================== DURUM ====================
    // ESP: gercekte bulunan blok pozisyonlari (tam X,Y,Z)
    private final List<BlockPos> espBloklari = new ArrayList<>();
    private int tickSayac = 0;

    private static final Color tmpSide = new Color();
    private static final Color tmpLine = new Color();

    public AncientDebrisModule() {
        super(Categories.Render, "ancient-debris",
            "464k Ancient Debris koordinatini radar+ESP ile gosterir. ESP sadece gercekte var olan bloklari cizip dogru Y'yi bulur.");
        DebrisData.load();
    }

    @Override
    public void onActivate() {
        DebrisData.load();
        espBloklari.clear();
        tickSayac = 0;
        espTara();
    }

    @Override
    public void onDeactivate() {
        espBloklari.clear();
    }

    // ==================== TICK ====================
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        tickSayac++;
        if (tickSayac >= taramaAraligi.get()) {
            tickSayac = 0;
            espTara();
        }
    }

    /**
     * DebrisData'dan yakın X,Z koordinatlarını alır.
     * Her koordinat için chunk yüklüyse gerçek Y'yi tarar.
     * Blok gerçekten Ancient Debris ise listeye ekler.
     */
    private void espTara() {
        espBloklari.clear();
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int r = espMesafe.get();

        // DebrisData: tüm 464k içinden yakın X,Z çiftleri
        List<int[]> yakin = DebrisData.getNearby(px, pz, r);

        BlockPos.Mutable mut = new BlockPos.Mutable();

        for (int[] coord : yakin) {
            int bx = coord[0];
            int bz = coord[1];
            int chunkX = bx >> 4;
            int chunkZ = bz >> 4;

            if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;
            WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);

            // Nether'da Ancient Debris Y=8..122 arasında çıkabilir
            // Chunk section'larını kullanarak verimli tara
            var sections = chunk.getSectionArray();
            for (int si = 0; si < sections.length; si++) {
                var sec = sections[si];
                if (sec == null || sec.isEmpty()) continue;
                int baseY = chunk.sectionIndexToCoord(si);
                if (baseY > 122 || baseY + 15 < 0) continue;

                int lx = bx & 15;
                int lz = bz & 15;
                for (int by = 0; by < 16; by++) {
                    if (sec.getBlockState(lx, by, lz).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        mut.set(bx, baseY + by, bz);
                        espBloklari.add(mut.toImmutable());
                    }
                }
            }
        }
    }

    // ==================== RENDER 3D: ESP ====================
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!espAktif.get() || mc.player == null) return;

        tmpSide.set(espDolguRengi.get());
        tmpLine.set(espKenarRengi.get());

        for (BlockPos pos : espBloklari) {
            event.renderer.box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                tmpSide, tmpLine, sekil.get(), 0
            );
        }
    }

    // ==================== RENDER 2D: RADAR ====================
    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!radarAktif.get() || mc.player == null) return;

        DrawContext ctx = event.drawContext;
        int sw = mc.getWindow().getScaledWidth();
        int rx = radarGenislik.get();
        int ry = radarYukseklik.get();

        int cx = sw - konumX.get() - rx;
        int cy = konumY.get() + ry;

        fillEllipse(ctx, cx, cy, rx, ry, arkaplanRengi.get().getPacked());

        int oc = ortaHatRengi.get().getPacked();
        ctx.fill(cx - rx, cy, cx + rx, cy + 1, oc);
        ctx.fill(cx, cy - ry, cx + 1, cy + ry, oc);

        drawEllipseOutline(ctx, cx, cy, rx, ry, 2, cemberRengi.get().getPacked());

        // Radar: DebrisData'dan geniş menzilde koordinatlar (radar-mesafe)
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int rMesafe = radarMesafe.get();
        int nc = radarNoktaRengi.get().getPacked();
        int ns = noktaBoyutu.get();

        List<int[]> radarKoord = DebrisData.getNearby(px, pz, rMesafe);
        for (int[] coord : radarKoord) {
            double dx = coord[0] + 0.5 - px;
            double dz = coord[1] + 0.5 - pz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double oran = Math.min(dist / rMesafe, 1.0);
            double aci  = Math.atan2(dz, dx);

            int dotX = cx + (int)(Math.cos(aci) * oran * (rx - ns - 1));
            int dotY = cy + (int)(Math.sin(aci) * oran * (ry - ns - 1));

            double ex = (double)(dotX - cx) / rx;
            double ez = (double)(dotY - cy) / ry;
            if (ex * ex + ez * ez <= 1.0) {
                fillDot(ctx, dotX, dotY, ns, nc);
            }
        }

        // Oyuncu merkezi
        fillDot(ctx, cx, cy, 4, oyuncuRengi.get().getPacked());

        // Radar koordinat sayısı + ESP blok sayısı
        String radarText = "Radar: " + radarKoord.size() + "  ESP: " + espBloklari.size();
        ctx.drawText(mc.textRenderer, radarText,
            cx - mc.textRenderer.getWidth(radarText) / 2,
            cy + ry + 3, 0xFFFFAA00, true);
    }

    // ==================== YARDIMCI ====================
    private void fillEllipse(DrawContext ctx, int cx, int cy, int rx, int ry, int color) {
        for (int x = -rx; x <= rx; x++) {
            double t = (double) x / rx;
            int h = (int)(Math.sqrt(Math.max(0, 1.0 - t * t)) * ry);
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
