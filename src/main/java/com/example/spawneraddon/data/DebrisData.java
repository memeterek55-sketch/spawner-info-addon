package com.example.spawneraddon.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 497k+ Ancient Debris koordinatini (X,Y,Z) yukler ve hizli yakin sorgulama saglar.
 * Grid: X,Z uzerinden 2D spatial index. Her hucre [x,y,z] uclu dizi listesi tutar.
 */
public class DebrisData {
    private static final int GRID = 64;
    private static final HashMap<Long, List<int[]>> index = new HashMap<>(32768);
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(
                DebrisData.class.getResourceAsStream("/assets/debris-radar/debris.dat.gz")))) {
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                int x = dis.readInt();
                int y = dis.readInt();
                int z = dis.readInt();
                long key = key(Math.floorDiv(x, GRID), Math.floorDiv(z, GRID));
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{x, y, z});
            }
        } catch (IOException e) {
            System.err.println("[DebrisRadar] Veri yukleme hatasi: " + e.getMessage());
        }
    }

    private static long key(int gx, int gz) {
        return ((long) gx << 32) | (gz & 0xFFFFFFFFL);
    }

    /** X,Z yaricapina gore yakın [x,y,z] uclu dizileri dondurur. */
    public static List<int[]> getNearby(double px, double pz, int radius) {
        if (!loaded) load();
        List<int[]> result = new ArrayList<>();
        int gcx = (int) Math.floor(px / GRID);
        int gcz = (int) Math.floor(pz / GRID);
        int gcr = (radius / GRID) + 1;
        double r2 = (double) radius * radius;

        for (int gx = gcx - gcr; gx <= gcx + gcr; gx++) {
            for (int gz = gcz - gcr; gz <= gcz + gcr; gz++) {
                List<int[]> cells = index.get(key(gx, gz));
                if (cells == null) continue;
                for (int[] c : cells) {
                    double dx = c[0] - px;
                    double dz = c[2] - pz;
                    if (dx * dx + dz * dz <= r2) result.add(c);
                }
            }
        }
        return result;
    }
}
