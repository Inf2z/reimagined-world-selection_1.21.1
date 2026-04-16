package com.inf2z.reimagined_world_selection.util;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import java.util.Set;

public interface MultiSelectableList {
    Set<WorldSelectionList.WorldListEntry> rws$getSelectedEntries();
    void rws$addToSelection(WorldSelectionList.WorldListEntry entry);
    void rws$removeFromSelection(WorldSelectionList.WorldListEntry entry);
    void rws$clearMultiSelection();
    boolean rws$isMultiSelected(WorldSelectionList.WorldListEntry entry);
}