package com.example.spawneraddon.modules;

import com.example.spawneraddon.data.DebrisData;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

public class AncientDebrisESP extends Module {

    private final SettingGroup sgGenel   = settings.getDefaultGroup();
    private final SettingGroup sgGorsel  = settings.createGroup("Gorsel");
    private final SettingGroup sgRenkler = settings.createGroup("Renkler");

    // --- Genel ---
    private final Setting<Integer> renderMesafe = sgGenel.add(new IntSetting.Builder()
        .name("render-mesafe")
        .description("Blok cinsinden arama yaricapi.")
        .defaultValue(64).min(8).sliderMax(256)
        .build());

    private final Setting<Integer> minY = sgGenel.add(new IntSetting.Builder()
        .name("min-y")
        .description("ESP kutusunun alt Y siniri (Nether'da debris en cok 8-22 arasi).")
        .defaultValue(8).min(0).sliderMax(128)
        .build());

    private final Setting<Integer> maxY = sgGenel.add(new IntSetting.Builder()
        .name("max-y")
        .description("ESP kutusunun ust Y siniri.")
        .defaultValue(22).min(0).sliderMax(128)
        .build());

    // --- Gorsel ---
    private final Setting<ShapeMode> sekil = sgGorsel.add(new EnumSetting.Builder<ShapeMode>()
        .name("sekil-modu")
        .description("Dolgu + cizgi, sadece cizgi veya sadece dolgu.")
        .defaultValue(ShapeMode.Both)
        .build());

    // --- Renkler ---
    private final Setting<SettingColor> dolguRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("dolgu-rengi")
        .description("ESP kutu ici dolgu rengi.")
        .defaultValue(new SettingColor(255, 140, 0, 35))
        .build());

    private final Setting<SettingColor> kenarRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("kenar-rengi")
        .description("ESP kutu cizgi rengi.")
        .defaultValue(new SettingColor(255, 140, 0, 220))
        .build());

    private static final Color tmpSide = new Color();
    private static final Color tmpLine = new Color();

    public AncientDebrisESP() {
        super(Categories.Render, "ancient-debris-esp",
            "Yakin Ancient Debris konumlarini 3D ESP kutusu olarak gosterir.");
        DebrisData.load();
    }

    @Override
    public void onActivate() {
        DebrisData.load();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int dist = renderMesafe.get();
        int yBot = minY.get();
        int yTop = maxY.get();
        if (yBot >= yTop) yTop = yBot + 1;

        tmpSide.set(dolguRengi.get());
        tmpLine.set(kenarRengi.get());

        List<int[]> yakin = DebrisData.getNearby(px, pz, dist);
        for (int[] coord : yakin) {
            int x = coord[0];
            int z = coord[1];
            event.renderer.box(x, yBot, z, x + 1, yTop, z + 1,
                tmpSide, tmpLine, sekil.get(), 0);
        }
    }
}
