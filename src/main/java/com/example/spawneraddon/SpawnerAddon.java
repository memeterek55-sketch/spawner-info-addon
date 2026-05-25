package com.example.spawneraddon;

import com.example.spawneraddon.modules.SpawnerInfo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SpawnerAddon extends MeteorAddon {

    @Override
    public void onInitialize() {
        Modules.get().add(new SpawnerInfo());
    }

    @Override
    public String getPackage() {
        return "com.example.spawneraddon";
    }
}
