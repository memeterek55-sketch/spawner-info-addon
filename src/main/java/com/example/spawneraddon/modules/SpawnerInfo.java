package com.example.spawneraddon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
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

    private final SettingGroup sgGenel   = settings.getDefaultGroup();
    private final SettingGroup sgGorunum = settings.createGroup("Görünüm");
    private final SettingGroup sgRenkler = settings.createGroup("Renkler");

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

    private final Set<BlockPos> duyurulanlar    = new HashSet<>();
    private final List<SpawnerKaydi> spawnerlar = new ArrayList<>();

    public SpawnerInfo() {
        super(Categories.Render, "spawner-info",
            "Yakındaki spawnerların türünü ve mesafesini 3D etiketle gösterir.");
    }

    @Override
    public void onDeactivate() {
        duyurulanlar.clear();
        spawnerlar.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        spawnerlar.clear();

        Vec3d oyuncuPos = mc.player.getPos();
        int r            = menzil.get();
        int chunkYaricap = (r >> 4) + 1;

        // 1.21.11'de getChunkPos() kaldırıldı — BlockPos üzerinden ChunkPos yap
        ChunkPos merkez = new ChunkPos(mc.player.getBlockPos());

        for (int cx = -chunkYaricap; cx <= chunkYaricap; cx++) {
            for (int cz = -chunkYaricap; cz <= chunkYaricap; cz++) {
                // getWorldChunk: yüklenmemişse null döner, asla crash vermez
                WorldChunk chunk = mc.world.getChunkManager()
                    .getWorldChunk(merkez.x + cx, merkez.z + cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MobSpawnerBlockEntity spawnerBE)) continue;

                    BlockPos pos  = be.getPos();
                    Vec3d center  = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    double dist   = oyuncuPos.distanceTo(center);
                    if (dist > r) continue;

                    String tur = spawnerTuruAl(spawnerBE);
                    spawnerlar.add(new SpawnerKaydi(pos, tur, dist));

                    if (chatMesaj.get() && duyurulanlar.add(pos)) {
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

        for (SpawnerKaydi kayit : spawnerlar) {
            Vector3d pos = new Vector3d(
                kayit.pos.getX() + 0.5,
                kayit.pos.getY() + 2.2,
                kayit.pos.getZ() + 0.5
            );

            if (!NametagUtils.to2D(pos, yaziOlcegi.get())) continue;

            NametagUtils.begin(pos);

            // begin/end BİR KEZ — birden fazla çağırmak crash verir
            TextRenderer tr = TextRenderer.get();
            tr.begin(yaziOlcegi.get(), false, true);

            float yOff = 0;

            if (turGoster.get()) {
                String line = "Tür: " + kayit.tur;
                tr.render(line, -(float) tr.getWidth(line, false) / 2f, yOff, turRengi.get(), false);
                yOff += 10;
            }

            if (mesafeGoster.get()) {
                String line = String.format("%.1f blok", kayit.mesafe);
                tr.render(line, -(float) tr.getWidth(line, false) / 2f, yOff, mesafeRengi.get(), false);
            }

            tr.end();
            NametagUtils.end();
        }
    }

    private String spawnerTuruAl(MobSpawnerBlockEntity be) {
        try {
            NbtCompound nbt = be.createNbt(mc.world.getRegistryManager());
            String rawId    = "";

            for (String key : new String[]{"SpawnData", "spawn_data"}) {
                if (nbt.contains(key)) {
                    NbtCompound sd = nbt.getCompound(key);
                    if (sd.contains("entity")) {
                        rawId = sd.getCompound("entity").getString("id");
                        break;
                    }
                }
            }

            String path = rawId.contains(":") ? rawId.split(":")[1] : rawId;
            if (path.isBlank()) return "Bilinmiyor";

            return switch (path) {
                case "zombie"           -> "Zombie";
                case "skeleton"         -> "İskelet";
                case "spider"           -> "Örümcek";
                case "cave_spider"      -> "Mağara Örümceği";
                case "blaze"            -> "Blaze";
                case "creeper"          -> "Creeper";
                case "magma_cube"       -> "Magma Küpü";
                case "silverfish"       -> "Gümüş Balık";
                case "endermite"        -> "Endermite";
                case "piglin"           -> "Piglin";
                case "zombified_piglin" -> "Zombie Piglin";
                case "husk"             -> "Husk";
                case "stray"            -> "Stray";
                case "wither_skeleton"  -> "Wither İskelet";
                case "guardian"         -> "Guardian";
                case "drowned"          -> "Drowned";
                case "zombie_villager"  -> "Zombie Köylü";
                default                 -> kelimeBuyut(path.replace("_", " "));
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

    private record SpawnerKaydi(BlockPos pos, String tur, double mesafe) {}
}