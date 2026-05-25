package com.example.spawneraddon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
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

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("menzil")
        .description("Spawner'ları kaç blok mesafede tespit etsin.")
        .defaultValue(32)
        .min(1)
        .sliderMax(128)
        .build()
    );

    private final Setting<Boolean> chatMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-mesaj")
        .description("Yeni spawner bulunduğunda chat'e mesaj atsın.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("yazi-rengi")
        .description("Spawner etiketinin rengi.")
        .defaultValue(new SettingColor(255, 165, 0, 255))
        .build()
    );

    private final Setting<SettingColor> distanceColor = sgGeneral.add(new ColorSetting.Builder()
        .name("mesafe-rengi")
        .description("Mesafe yazısının rengi.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Set<BlockPos> announcedSpawners = new HashSet<>();
    private final List<SpawnerEntry> nearbySpawners = new ArrayList<>();

    public SpawnerInfo() {
        super(Categories.Render, "spawner-info", "Yakındaki spawner'ların türünü ve mesafesini gösterir.");
    }

    @Override
    public void onDeactivate() {
        announcedSpawners.clear();
        nearbySpawners.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        nearbySpawners.clear();
        Vec3d playerPos = mc.player.getPos();
        int r = range.get();
        int chunkRadius = (r >> 4) + 1;
        ChunkPos center = mc.player.getChunkPos();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                WorldChunk chunk = mc.world.getChunk(center.x + cx, center.z + cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MobSpawnerBlockEntity spawnerBE)) continue;

                    BlockPos pos = be.getPos();
                    double dist = playerPos.distanceTo(
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    );
                    if (dist > r) continue;

                    String entityName = getSpawnerEntityName(spawnerBE);
                    nearbySpawners.add(new SpawnerEntry(pos, entityName, dist));

                    if (chatMessage.get() && !announcedSpawners.contains(pos)) {
                        announcedSpawners.add(pos);
                        info("§6[SpawnerInfo] §fYeni spawner! §eTür: §a"
                            + entityName + " §f| §eMesafe: §a"
                            + String.format("%.1f", dist) + " blok");
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null) return;

        for (SpawnerEntry entry : nearbySpawners) {
            Vector3d worldPos = new Vector3d(
                entry.pos.getX() + 0.5,
                entry.pos.getY() + 1.8,
                entry.pos.getZ() + 0.5
            );

            Vector3d screenPos = NametagUtils.to2D(worldPos, 1.0);
            if (screenPos == null) continue;

            NametagUtils.begin(screenPos);

            String line1 = "Tür: " + entry.entityName;
            String line2 = String.format("Mesafe: %.1f blok", entry.distance);

            renderLine(line1, -10, textColor.get());
            renderLine(line2, 0, distanceColor.get());

            NametagUtils.end();
        }
    }

    private void renderLine(String text, float yOffset, Color color) {
        meteordevelopment.meteorclient.renderer.text.TextRenderer tr =
            meteordevelopment.meteorclient.renderer.text.TextRenderer.get();
        tr.begin(1.0, false, true);
        float w = (float) tr.getWidth(text, false);
        tr.render(text, -w / 2f, yOffset, color, false);
        tr.end();
    }

    private String getSpawnerEntityName(MobSpawnerBlockEntity spawnerBE) {
        try {
            NbtCompound nbt = spawnerBE.createNbt(mc.world.getRegistryManager());

            String rawId = "bilinmiyor";

            if (nbt.contains("SpawnData")) {
                NbtCompound spawnData = nbt.getCompound("SpawnData");
                if (spawnData.contains("entity")) {
                    NbtCompound entity = spawnData.getCompound("entity");
                    rawId = entity.getString("id");
                }
            } else if (nbt.contains("spawn_data")) {
                NbtCompound spawnData = nbt.getCompound("spawn_data");
                if (spawnData.contains("entity")) {
                    NbtCompound entity = spawnData.getCompound("entity");
                    rawId = entity.getString("id");
                }
            }

            String path = rawId.contains(":") ? rawId.split(":")[1] : rawId;

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
                case "elder_guardian"   -> "Elder Guardian";
                case "drowned"          -> "Drowned";
                default                 -> capitalize(path.replace("_", " "));
            };
        } catch (Exception e) {
            return "Bilinmiyor";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static class SpawnerEntry {
        final BlockPos pos;
        final String entityName;
        final double distance;

        SpawnerEntry(BlockPos pos, String entityName, double distance) {
            this.pos = pos;
            this.entityName = entityName;
            this.distance = distance;
        }
    }
}
