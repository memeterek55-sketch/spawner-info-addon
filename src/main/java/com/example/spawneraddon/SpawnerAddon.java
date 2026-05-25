package com.example.spawneraddon;

import com.example.spawneraddon.modules.SpawnerInfo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SpawnerAddon extends MeteorAddon {

    public static final Category KATEGORI = new Category("SpawnerInfo");

    @Override
    public void addCategories() {
        Modules.get().addCategory(KATEGORI);
    }

    @Override
    public void onInitialize() {
        Modules.get().add(new SpawnerInfo());
    }

    @Override
    public String getPackage() {
        return "com.example.spawneraddon";
    }
}
