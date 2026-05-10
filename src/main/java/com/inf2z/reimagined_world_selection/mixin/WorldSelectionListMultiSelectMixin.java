package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.util.MultiSelectableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("rawtypes")
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMultiSelectMixin extends AbstractSelectionList implements MultiSelectableList {

    protected WorldSelectionListMultiSelectMixin(Minecraft mc, int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
    }

    @Unique
    private final Set<WorldSelectionList.WorldListEntry> rws$multiSelected = new LinkedHashSet<>();

    @Override
    public Set<WorldSelectionList.WorldListEntry> rws$getSelectedEntries() {
        return rws$multiSelected;
    }

    @Override
    public void rws$addToSelection(WorldSelectionList.WorldListEntry entry) {
        rws$multiSelected.add(entry);
    }

    @Override
    public void rws$removeFromSelection(WorldSelectionList.WorldListEntry entry) {
        rws$multiSelected.remove(entry);
    }

    @Override
    public void rws$clearMultiSelection() {
        rws$multiSelected.clear();
    }

    @Override
    public boolean rws$isMultiSelected(WorldSelectionList.WorldListEntry entry) {
        return rws$multiSelected.contains(entry);
    }

    @Unique
    @Nullable
    private WorldSelectionList.WorldListEntry rws$getClickedEntry(double mouseX, double mouseY) {
        Object entry = this.getEntryAtPosition(mouseX, mouseY);
        if (entry instanceof WorldSelectionList.WorldListEntry worldEntry) {
            return worldEntry;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        long handle = Minecraft.getInstance().getWindow().getWindow();
        boolean ctrlDown =
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        WorldSelectionList.WorldListEntry worldEntry = rws$getClickedEntry(mouseX, mouseY);

        if (!ctrlDown) {
            if (worldEntry != null) {
                rws$multiSelected.clear();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (worldEntry == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        WorldSelectionList self = (WorldSelectionList) (Object) this;

        if (self.getSelected() instanceof WorldSelectionList.WorldListEntry currentSelected && currentSelected != worldEntry) {
            rws$multiSelected.add(currentSelected);
        }

        if (rws$multiSelected.contains(worldEntry)) {
            rws$multiSelected.remove(worldEntry);
        } else {
            rws$multiSelected.add(worldEntry);
        }

        self.setSelected(worldEntry);
        return true;
    }
}