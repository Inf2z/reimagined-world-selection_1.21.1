package com.inf2z.reimagined_world_select.util;

import java.lang.reflect.Method;

public final class RenderCompat {
    private static final Method ENABLE_BLEND = resolve("enableBlend");
    private static final Method DISABLE_BLEND = resolve("disableBlend");
    private static final Method DEFAULT_BLEND_FUNC = resolve("defaultBlendFunc");

    private RenderCompat() {
    }

    private static Method resolve(String name) {
        try {
            Class<?> cls = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            Method method = cls.getMethod(name);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void invoke(Method method) {
        if (method == null) return;
        try {
            method.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    public static void enableBlend() {
        invoke(ENABLE_BLEND);
        invoke(DEFAULT_BLEND_FUNC);
    }

    public static void disableBlend() {
        invoke(DISABLE_BLEND);
    }
}