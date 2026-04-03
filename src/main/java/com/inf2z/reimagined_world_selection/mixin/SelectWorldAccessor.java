package com.inf2z.reimagined_world_selection.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SelectWorldScreen.class)
public interface SelectWorldAccessor {
    @Accessor("list")
    WorldSelectionList getList();

    @Accessor("searchBox")
    EditBox getSearchBox();
}