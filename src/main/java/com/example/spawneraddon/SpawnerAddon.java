package com.example.spawneraddon;

import com.example.spawneraddon.modules.AncientDebrisESP;
import com.example.spawneraddon.modules.AncientDebrisRadar;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class SpawnerAddon extends MeteorAddon {

    @Override
    public void onInitialize() {
        Modules.get().add(new AncientDebrisRadar());
        Modules.get().add(new AncientDebrisESP());
    }

    @Override
    public String getPackage() {
        return "com.example.spawneraddon";
    }
}
