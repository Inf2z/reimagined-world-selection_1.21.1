package com.inf2z.reimagined_world_selection.util;

import net.minecraft.client.Minecraft;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class MinecraftCompat {

    private static final MethodHandle EXECUTE;
    private static final MethodHandle TELL;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        MethodHandle execute = null;
        MethodHandle tell = null;

        try {
            execute = lookup.findVirtual(
                    Minecraft.class,
                    "execute",
                    MethodType.methodType(void.class, Runnable.class)
            );
        } catch (Throwable ignored) {
        }

        try {
            tell = lookup.findVirtual(
                    Minecraft.class,
                    "tell",
                    MethodType.methodType(void.class, Runnable.class)
            );
        } catch (Throwable ignored) {
        }

        EXECUTE = execute;
        TELL = tell;
    }

    private MinecraftCompat() {
    }

    public static void runOnMainThread(Minecraft mc, Runnable task) {
        try {
            if (EXECUTE != null) {
                EXECUTE.invoke(mc, task);
            } else if (TELL != null) {
                TELL.invoke(mc, task);
            } else {
                task.run();
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to schedule task on main thread", t);
        }
    }
}