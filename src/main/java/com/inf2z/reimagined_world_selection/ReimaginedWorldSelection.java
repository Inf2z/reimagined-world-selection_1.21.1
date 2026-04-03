package com.inf2z.reimagined_world_selection;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("reimagined_world_selection")
public class ReimaginedWorldSelection {
    public ReimaginedWorldSelection(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}