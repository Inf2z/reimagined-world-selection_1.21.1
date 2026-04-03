package com.inf2z.reimagined_world_selection.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSelectionList.WorldListEntry.class)
public interface EntryAccessor {
    @Accessor("summary")
    LevelSummary getSummary();
}