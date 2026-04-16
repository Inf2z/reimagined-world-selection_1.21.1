package com.inf2z.reimagined_world_selection.mixin;

import com.inf2z.reimagined_world_selection.util.MultiSelectableList;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("rawtypes")
@Mixin(WorldSelectionList.class)
public abstract class WorldSelectionListMultiSelectMixin extends AbstractSelectionList implements MultiSelectableList {

    protected WorldSelectionListMultiSelectMixin(net.minecraft.client.Minecraft mc, int width, int height, int top, int itemHeight) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        long handle = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        boolean ctrlDown =
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;

        if (!ctrlDown) {
            rws$multiSelected.clear();
            return super.mouseClicked(mouseX, mouseY, button);
        }

        WorldSelectionList self = (WorldSelectionList) (Object) this;

        int rowWidth = self.getRowWidth();
        int centerX = self.getX() + self.getWidth() / 2;
        int rowLeft = centerX - rowWidth / 2;
        int rowRight = centerX + rowWidth / 2;

        if (mouseX < rowLeft || mouseX > rowRight) return super.mouseClicked(mouseX, mouseY, button);
        if (mouseY < self.getY() || mouseY > self.getBottom()) return super.mouseClicked(mouseX, mouseY, button);

        int relativeY = (int)(mouseY - self.getY() + self.getScrollAmount()) - 4;
        if (relativeY < 0) return super.mouseClicked(mouseX, mouseY, button);

        int idx = relativeY / 36;
        if (idx < 0 || idx >= self.children().size()) return super.mouseClicked(mouseX, mouseY, button);

        var entry = self.children().get(idx);
        if (!(entry instanceof WorldSelectionList.WorldListEntry worldEntry)) return super.mouseClicked(mouseX, mouseY, button);

        if (self.getSelected() instanceof WorldSelectionList.WorldListEntry currentSelected
                && currentSelected != worldEntry) {
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