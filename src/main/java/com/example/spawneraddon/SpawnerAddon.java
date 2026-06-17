package com.example.spawneraddon;

import com.example.spawneraddon.modules.AncientDebrisRadar;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SpawnerAddon extends MeteorAddon {

    @Override
    public void onInitialize() {
        Modules.get().add(new AncientDebrisRadar());
    }

    @Override
    public String getPackage() {
        return "com.example.spawneraddon";
    }
}
