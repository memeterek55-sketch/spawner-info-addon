package com.example.spawneraddon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

        for (BlockEntity be : mc.world.blockEntities) {
            if (!(be instanceof MobSpawnerBlockEntity spawnerBE)) continue;

            BlockPos pos = be.getPos();
            double dist = playerPos.distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            if (dist > r) continue;

            String entityName = getSpawnerEntityName(spawnerBE);
            nearbySpawners.add(new SpawnerEntry(pos, entityName, dist));

            if (chatMessage.get() && !announcedSpawners.contains(pos)) {
                announcedSpawners.add(pos);
                info("§6[SpawnerInfo] §fYeni spawner bulundu! §eTür: §a" + entityName + " §f| §eMesafe: §a" + String.format("%.1f", dist) + " blok");
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null) return;

        for (SpawnerEntry entry : nearbySpawners) {
            Vec3d renderPos = new Vec3d(
                entry.pos.getX() + 0.5,
                entry.pos.getY() + 1.8,
                entry.pos.getZ() + 0.5
            );

            Vector3d screenPos = new Vector3d();
            if (!NametagUtils.to2D(renderPos, 1.0, screenPos)) continue;

            NametagUtils.begin(screenPos);

            String line1 = "§lTür: " + entry.entityName;
            String line2 = String.format("Mesafe: %.1f blok", entry.distance);

            drawText(line1, 0, -20, textColor.get(), event);
            drawText(line2, 0, -8, distanceColor.get(), event);

            NametagUtils.end();
        }
    }

    private void drawText(String text, float x, float y, Color color, Render3DEvent event) {
        meteordevelopment.meteorclient.renderer.text.TextRenderer renderer =
            meteordevelopment.meteorclient.renderer.text.TextRenderer.get();

        float width = renderer.getWidth(text, false);
        renderer.begin(1.0, false, true);
        renderer.render(text, x - width / 2f, y, color, false);
        renderer.end();
    }

    private String getSpawnerEntityName(MobSpawnerBlockEntity spawnerBE) {
        try {
            var logic = spawnerBE.getLogic();
            var entry = logic.getRenderedEntry();
            if (entry == null) return "Bilinmiyor";

            EntityType<?> type = entry.type().value();
            String id = EntityType.getId(type).getPath();

            return switch (id) {
                case "zombie"          -> "Zombie";
                case "skeleton"        -> "İskelet";
                case "spider"          -> "Örümcek";
                case "cave_spider"     -> "Mağara Örümceği";
                case "blaze"           -> "Blaze";
                case "creeper"         -> "Creeper";
                case "magma_cube"      -> "Magma Küpü";
                case "silverfish"      -> "Gümüş Balık";
                case "endermite"       -> "Endermite";
                case "piglin"          -> "Piglin";
                case "zombified_piglin"-> "Zombie Piglin";
                case "husk"            -> "Husk";
                case "stray"           -> "Stray";
                default                -> capitalize(id.replace("_", " "));
            };
        } catch (Exception e) {
            return "Bilinmiyor";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
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
