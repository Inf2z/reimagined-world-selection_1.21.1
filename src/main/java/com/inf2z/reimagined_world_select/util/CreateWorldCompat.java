package com.inf2z.reimagined_world_select.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class CreateWorldCompat {

    private static final MethodHandle OPEN_FRESH;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        MethodHandle handle = null;

        try {
            Class<?> createWorldScreen = Class.forName(
                    "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen"
            );
            handle = lookup.findStatic(
                    createWorldScreen,
                    "openFresh",
                    MethodType.methodType(void.class, Minecraft.class, Screen.class)
            );
        } catch (Throwable ignored) {
        }

        OPEN_FRESH = handle;
    }

    private CreateWorldCompat() {
    }

    public static void openFresh(Minecraft mc, Screen lastScreen) {
        try {
            if (OPEN_FRESH != null) {
                OPEN_FRESH.invoke(mc, lastScreen);
            } else {
                throw new UnsupportedOperationException("CreateWorldScreen.openFresh not found");
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke CreateWorldScreen.openFresh", t);
        }
    }
}