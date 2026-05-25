package com.example.spawneraddon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnerInfo extends Module {

    private final SettingGroup sgGenel    = settings.getDefaultGroup();
    private final SettingGroup sgGorunum  = settings.createGroup("Görünüm");
    private final SettingGroup sgRenkler  = settings.createGroup("Renkler");

    // --- Genel ---
    private final Setting<Integer> menzil = sgGenel.add(new IntSetting.Builder()
        .name("menzil")
        .description("Spawner tespiti için maksimum mesafe (blok).")
        .defaultValue(32)
        .min(1)
        .sliderMax(128)
        .build()
    );

    private final Setting<Boolean> chatMesaj = sgGenel.add(new BoolSetting.Builder()
        .name("chat-mesaj")
        .description("Yeni spawner bulununca chat'e bilgi atsın.")
        .defaultValue(true)
        .build()
    );

    // --- Görünüm ---
    private final Setting<Boolean> turGoster = sgGorunum.add(new BoolSetting.Builder()
        .name("tur-goster")
        .description("Spawner türünü etiket olarak göster.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mesafeGoster = sgGorunum.add(new BoolSetting.Builder()
        .name("mesafe-goster")
        .description("Spawner mesafesini etiket olarak göster.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> yaziOlcegi = sgGorunum.add(new DoubleSetting.Builder()
        .name("yazi-olcegi")
        .description("Etiket yazı boyutu.")
        .defaultValue(1.0)
        .min(0.2)
        .sliderMax(3.0)
        .build()
    );

    // --- Renkler ---
    private final Setting<SettingColor> turRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("tur-rengi")
        .description("Spawner türü yazısının rengi.")
        .defaultValue(new SettingColor(255, 165, 0, 255))
        .build()
    );

    private final Setting<SettingColor> mesafeRengi = sgRenkler.add(new ColorSetting.Builder()
        .name("mesafe-rengi")
        .description("Mesafe yazısının rengi.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Set<BlockPos> duyurulanlar = new HashSet<>();
    private final List<SpawnerKaydi> yakinSpawnerlar = new ArrayList<>();

    public SpawnerInfo() {
        super(Categories.Render, "spawner-info",
            "Yakındaki spawnerların türünü ve mesafesini gösterir.");
    }

    @Override
    public void onDeactivate() {
        duyurulanlar.clear();
        yakinSpawnerlar.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        yakinSpawnerlar.clear();
        Vec3d oyuncuPos = mc.player.getPos();
        int r = menzil.get();
        int chunkYaricap = (r >> 4) + 1;
        ChunkPos merkez = mc.player.getChunkPos();

        for (int cx = -chunkYaricap; cx <= chunkYaricap; cx++) {
            for (int cz = -chunkYaricap; cz <= chunkYaricap; cz++) {
                WorldChunk chunk = mc.world.getChunk(merkez.x + cx, merkez.z + cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MobSpawnerBlockEntity spawnerBE)) continue;

                    BlockPos pos = be.getPos();
                    double dist = oyuncuPos.distanceTo(
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    );
                    if (dist > r) continue;

                    String tur = spawnerTuruAl(spawnerBE);
                    yakinSpawnerlar.add(new SpawnerKaydi(pos, tur, dist));

                    if (chatMesaj.get() && !duyurulanlar.contains(pos)) {
                        duyurulanlar.add(pos);
                        info("§6[SpawnerInfo] §fYeni spawner! §eTür: §a"
                            + tur + " §f| §eMesafe: §a"
                            + String.format("%.1f", dist) + " blok");
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null) return;
        if (!turGoster.get() && !mesafeGoster.get()) return;

        for (SpawnerKaydi kayit : yakinSpawnerlar) {
            Vector3d pos = new Vector3d(
                kayit.pos.getX() + 0.5,
                kayit.pos.getY() + 2.0,
                kayit.pos.getZ() + 0.5
            );

            if (!NametagUtils.to2D(pos, yaziOlcegi.get())) continue;
            NametagUtils.begin(pos);

            float yOffset = 0;

            if (turGoster.get()) {
                String satir1 = "§lTür: " + kayit.tur;
                metin(satir1, yOffset, turRengi.get());
                yOffset += 10;
            }

            if (mesafeGoster.get()) {
                String satir2 = String.format("%.1f blok", kayit.mesafe);
                metin(satir2, yOffset, mesafeRengi.get());
            }

            NametagUtils.end();
        }
    }

    private void metin(String yazi, float yOffset,
                       meteordevelopment.meteorclient.utils.render.color.Color renk) {
        meteordevelopment.meteorclient.renderer.text.TextRenderer tr =
            meteordevelopment.meteorclient.renderer.text.TextRenderer.get();
        tr.begin(1.0, false, true);
        float w = (float) tr.getWidth(yazi, false);
        tr.render(yazi, -w / 2f, yOffset, renk, false);
        tr.end();
    }

    private String spawnerTuruAl(MobSpawnerBlockEntity spawnerBE) {
        try {
            NbtCompound nbt = spawnerBE.createNbt(mc.world.getRegistryManager());
            String rawId = "";

            if (nbt.contains("SpawnData")) {
                NbtCompound sd = nbt.getCompound("SpawnData");
                if (sd.contains("entity")) rawId = sd.getCompound("entity").getString("id");
            } else if (nbt.contains("spawn_data")) {
                NbtCompound sd = nbt.getCompound("spawn_data");
                if (sd.contains("entity")) rawId = sd.getCompound("entity").getString("id");
            }

            String path = rawId.contains(":") ? rawId.split(":")[1] : rawId;
            if (path.isBlank()) return "Bilinmiyor";

            return switch (path) {
                case "zombie"            -> "Zombie";
                case "skeleton"          -> "İskelet";
                case "spider"            -> "Örümcek";
                case "cave_spider"       -> "Mağara Örümceği";
                case "blaze"             -> "Blaze";
                case "creeper"           -> "Creeper";
                case "magma_cube"        -> "Magma Küpü";
                case "silverfish"        -> "Gümüş Balık";
                case "endermite"         -> "Endermite";
                case "piglin"            -> "Piglin";
                case "zombified_piglin"  -> "Zombie Piglin";
                case "husk"              -> "Husk";
                case "stray"             -> "Stray";
                case "wither_skeleton"   -> "Wither İskelet";
                case "guardian"          -> "Guardian";
                case "drowned"           -> "Drowned";
                case "zombie_villager"   -> "Zombie Köylü";
                default                  -> kelimeBuyut(path.replace("_", " "));
            };
        } catch (Exception e) {
            return "Bilinmiyor";
        }
    }

    private String kelimeBuyut(String s) {
        if (s == null || s.isBlank()) return s;
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static class SpawnerKaydi {
        final BlockPos pos;
        final String tur;
        final double mesafe;
        SpawnerKaydi(BlockPos pos, String tur, double mesafe) {
            this.pos = pos; this.tur = tur; this.mesafe = mesafe;
        }
    }
}
