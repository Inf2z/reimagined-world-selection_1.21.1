package com.inf2z.reimagined_world_selection.util;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Method;

public final class NbtCompat {

    private static final Method CONTAINS_WITH_TYPE;
    private static final Method CONTAINS_STRING_ONLY;

    static {
        Method withType = null;
        Method stringOnly = null;

        try {
            withType = CompoundTag.class.getMethod("contains", String.class, int.class);
        } catch (Throwable ignored) {
        }

        try {
            stringOnly = CompoundTag.class.getMethod("contains", String.class);
        } catch (Throwable ignored) {
        }

        CONTAINS_WITH_TYPE = withType;
        CONTAINS_STRING_ONLY = stringOnly;
    }

    private NbtCompat() {
    }

    public static boolean contains(CompoundTag tag, String key, int type) {
        try {
            if (CONTAINS_WITH_TYPE != null) {
                return (boolean) CONTAINS_WITH_TYPE.invoke(tag, key, type);
            }

            if (CONTAINS_STRING_ONLY != null) {
                return (boolean) CONTAINS_STRING_ONLY.invoke(tag, key);
            }

            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean contains(CompoundTag tag, String key) {
        try {
            if (CONTAINS_STRING_ONLY != null) {
                return (boolean) CONTAINS_STRING_ONLY.invoke(tag, key);
            }
            if (CONTAINS_WITH_TYPE != null) {
                return (boolean) CONTAINS_WITH_TYPE.invoke(tag, key, 99);
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}