package com.inf2z.reimagined_world_select.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class GuiCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiCompat.class);

    private static Method resolvedBlit = null;
    private static Object[] resolvedBlitExtra = null;
    private static boolean blitResolved = false;

    private static Method resolvedDrawString = null;
    private static Method resolvedDrawStringComp = null;
    private static Method resolvedDrawStringFCS = null;
    private static Method resolvedDrawCenteredComp = null;
    private static Method resolvedDrawCenteredStr = null;
    private static boolean drawResolved = false;

    private static Method resolvedPose = null;
    private static Field resolvedPoseField = null;
    private static boolean poseResolved = false;

    private GuiCompat() {
    }

    public static void blit(GuiGraphics gui, ResourceLocation texture,
                            int x, int y, float u, float v,
                            int width, int height,
                            int texWidth, int texHeight) {
        if (!blitResolved) {
            resolveBlit();
        }

        if (resolvedBlit != null) {
            try {
                Object[] args = buildResolvedBlitArgs(resolvedBlit.getParameterTypes(),
                        texture, x, y, u, v, width, height, texWidth, texHeight);
                if (args != null) {
                    resolvedBlit.invoke(gui, args);
                    return;
                }
            } catch (Throwable t) {
                LOGGER.warn("Cached blit failed, re-resolving", t);
                resolvedBlit = null;
                blitResolved = false;
                resolveBlit();
            }
        }
    }

    private static void resolveBlit() {
        blitResolved = true;

        List<Method> methods = new ArrayList<>();
        for (Method m : GuiGraphics.class.getMethods()) {
            if (m.getName().equals("blit")) methods.add(m);
        }
        for (Method m : GuiGraphics.class.getDeclaredMethods()) {
            if (m.getName().equals("blit") && !methods.contains(m)) methods.add(m);
        }

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();

            if (params.length == 9
                    && params[0] == ResourceLocation.class
                    && params[1] == int.class && params[2] == int.class
                    && params[3] == float.class && params[4] == float.class
                    && params[5] == int.class && params[6] == int.class
                    && params[7] == int.class && params[8] == int.class) {
                m.setAccessible(true);
                resolvedBlit = m;
                LOGGER.info("Resolved blit: legacy 9-param");
                return;
            }
        }

        Function<ResourceLocation, ?> rtFunc = createRenderTypeFunction();

        if (rtFunc != null) {
            for (Method m : methods) {
                Class<?>[] params = m.getParameterTypes();

                if (params.length == 10
                        && Function.class.isAssignableFrom(params[0])
                        && params[1] == ResourceLocation.class
                        && params[2] == int.class && params[3] == int.class
                        && params[4] == float.class && params[5] == float.class
                        && params[6] == int.class && params[7] == int.class
                        && params[8] == int.class && params[9] == int.class) {
                    m.setAccessible(true);
                    resolvedBlit = m;
                    resolvedBlitExtra = new Object[]{rtFunc};
                    LOGGER.info("Resolved blit: Function+RL 10-param");
                    return;
                }
            }
        }

        for (Method m : methods) {
            Class<?>[] params = m.getParameterTypes();
            if (params.length >= 9 && hasResourceLocation(params)) {
                m.setAccessible(true);
                resolvedBlit = m;
                LOGGER.info("Resolved blit: fallback {} params: {}", params.length, m);
                return;
            }
        }

        LOGGER.warn("Could not resolve any blit method. Available:");
        for (Method m : methods) {
            LOGGER.warn("  {}", m);
        }
    }

    private static boolean hasResourceLocation(Class<?>[] params) {
        for (Class<?> p : params) {
            if (p == ResourceLocation.class) return true;
        }
        return false;
    }

    private static Object[] buildResolvedBlitArgs(Class<?>[] params,
                                                  ResourceLocation texture,
                                                  int x, int y, float u, float v,
                                                  int width, int height,
                                                  int texWidth, int texHeight) {
        Object[] args = new Object[params.length];
        boolean textureUsed = false;
        boolean funcUsed = false;
        int intIdx = 0;
        int floatIdx = 0;

        int[] intVals = {x, y, width, height, texWidth, texHeight, -1, 0, 0};
        float[] floatVals = {u, v, 0f, 0f, (float) width, (float) height, (float) texWidth, (float) texHeight};

        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];

            if (p == ResourceLocation.class && !textureUsed) {
                args[i] = texture;
                textureUsed = true;
            } else if (Function.class.isAssignableFrom(p) && !funcUsed) {
                if (resolvedBlitExtra != null && resolvedBlitExtra.length > 0) {
                    args[i] = resolvedBlitExtra[0];
                } else {
                    Function<ResourceLocation, ?> f = createRenderTypeFunction();
                    if (f == null) return null;
                    args[i] = f;
                }
                funcUsed = true;
            } else if (p == int.class) {
                args[i] = intVals[Math.min(intIdx++, intVals.length - 1)];
            } else if (p == float.class) {
                args[i] = floatVals[Math.min(floatIdx++, floatVals.length - 1)];
            } else if (p == boolean.class) {
                args[i] = false;
            } else if (p == String.class) {
                args[i] = "rws";
            } else if (java.util.function.Supplier.class.isAssignableFrom(p)) {
                args[i] = (java.util.function.Supplier<String>) () -> "rws";
            } else {
                Object special = findStaticFieldOfType(p);
                if (special == null) return null;
                args[i] = special;
            }
        }

        return textureUsed ? args : null;
    }

    @SuppressWarnings("unchecked")
    private static Function<ResourceLocation, ?> createRenderTypeFunction() {
        try {
            Class<?> rtClass = Class.forName("net.minecraft.client.renderer.RenderType");
            for (String name : new String[]{"guiTextured", "text", "guiTexturedOverlay"}) {
                try {
                    Method m = rtClass.getMethod(name, ResourceLocation.class);
                    return loc -> {
                        try { return m.invoke(null, loc); }
                        catch (Throwable t) { throw new RuntimeException(t); }
                    };
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object findStaticFieldOfType(Class<?> type) {
        String[] classes = {
                "net.minecraft.client.renderer.RenderPipelines",
                "com.mojang.blaze3d.pipeline.RenderPipeline",
                "com.mojang.blaze3d.pipeline.RenderPipelines",
                "net.minecraft.client.renderer.RenderPipeline"
        };
        for (String cls : classes) {
            try {
                Class<?> c = Class.forName(cls);
                for (Field f : c.getFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && type.isAssignableFrom(f.getType())) {
                        try {
                            Object val = f.get(null);
                            if (val != null && f.getName().toUpperCase().contains("GUI")) return val;
                        } catch (Throwable ignored) {}
                    }
                }
                for (Field f : c.getFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && type.isAssignableFrom(f.getType())) {
                        try {
                            Object val = f.get(null);
                            if (val != null) return val;
                        } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // ========== drawString ==========

    public static int drawString(GuiGraphics gui, Font font, String text, int x, int y, int color) {
        return drawString(gui, font, text, x, y, color, false);
    }

    public static int drawString(GuiGraphics gui, Font font, String text, int x, int y, int color, boolean shadow) {
        if (!drawResolved) resolveDrawMethods();
        if (resolvedDrawString != null) {
            try {
                Class<?>[] p = resolvedDrawString.getParameterTypes();
                Object result;
                if (p.length >= 6) {
                    result = resolvedDrawString.invoke(gui, font, text, x, y, color, shadow);
                } else {
                    result = resolvedDrawString.invoke(gui, font, text, x, y, color);
                }
                return result instanceof Integer i ? i : 0;
            } catch (Throwable ignored) {}
        }
        return bruteForce(gui, "drawString", font, text, x, y, color, shadow);
    }

    public static int drawString(GuiGraphics gui, Font font, Component text, int x, int y, int color) {
        if (!drawResolved) resolveDrawMethods();
        if (resolvedDrawStringComp != null) {
            try {
                Class<?>[] p = resolvedDrawStringComp.getParameterTypes();
                Object result;
                if (p.length >= 6) {
                    result = resolvedDrawStringComp.invoke(gui, font, text, x, y, color, false);
                } else {
                    result = resolvedDrawStringComp.invoke(gui, font, text, x, y, color);
                }
                return result instanceof Integer i ? i : 0;
            } catch (Throwable ignored) {}
        }
        return drawString(gui, font, text.getString(), x, y, color, false);
    }

    public static int drawString(GuiGraphics gui, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        if (!drawResolved) resolveDrawMethods();
        if (resolvedDrawStringFCS != null) {
            try {
                Class<?>[] p = resolvedDrawStringFCS.getParameterTypes();
                Object result;
                if (p.length >= 6) {
                    result = resolvedDrawStringFCS.invoke(gui, font, text, x, y, color, shadow);
                } else {
                    result = resolvedDrawStringFCS.invoke(gui, font, text, x, y, color);
                }
                return result instanceof Integer i ? i : 0;
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static void drawCenteredString(GuiGraphics gui, Font font, Component text, int x, int y, int color) {
        if (!drawResolved) resolveDrawMethods();
        if (resolvedDrawCenteredComp != null) {
            try {
                resolvedDrawCenteredComp.invoke(gui, font, text, x, y, color);
                return;
            } catch (Throwable ignored) {}
        }
        String s = text.getString();
        drawString(gui, font, s, x - font.width(s) / 2, y, color, false);
    }

    public static void drawCenteredString(GuiGraphics gui, Font font, String text, int x, int y, int color) {
        if (!drawResolved) resolveDrawMethods();
        if (resolvedDrawCenteredStr != null) {
            try {
                resolvedDrawCenteredStr.invoke(gui, font, text, x, y, color);
                return;
            } catch (Throwable ignored) {}
        }
        drawString(gui, font, text, x - font.width(text) / 2, y, color, false);
    }

    private static void resolveDrawMethods() {
        drawResolved = true;
        for (Method m : GuiGraphics.class.getMethods()) {
            if (!m.getName().equals("drawString")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length < 5 || p[0] != Font.class) continue;
            m.setAccessible(true);

            if (p[1] == String.class && resolvedDrawString == null) resolvedDrawString = m;
            if (Component.class.isAssignableFrom(p[1]) && resolvedDrawStringComp == null) resolvedDrawStringComp = m;
            if (p[1] == FormattedCharSequence.class && resolvedDrawStringFCS == null) resolvedDrawStringFCS = m;
        }

        for (Method m : GuiGraphics.class.getMethods()) {
            if (!m.getName().equals("drawCenteredString")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length < 5 || p[0] != Font.class) continue;
            m.setAccessible(true);

            if (Component.class.isAssignableFrom(p[1]) && resolvedDrawCenteredComp == null) resolvedDrawCenteredComp = m;
            if (p[1] == String.class && resolvedDrawCenteredStr == null) resolvedDrawCenteredStr = m;
        }
    }

    private static int bruteForce(GuiGraphics gui, String methodName, Font font, Object text, int x, int y, int color, boolean shadow) {
        for (Method m : GuiGraphics.class.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length < 5 || p[0] != Font.class || !p[1].isInstance(text)) continue;
            try {
                m.setAccessible(true);
                Object result;
                if (p.length >= 6) result = m.invoke(gui, font, text, x, y, color, shadow);
                else result = m.invoke(gui, font, text, x, y, color);
                return result instanceof Integer i ? i : 0;
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    // ========== pose ==========

    public static PoseStack pose(GuiGraphics gui) {
        if (!poseResolved) resolvePose();

        if (resolvedPose != null) {
            try { return (PoseStack) resolvedPose.invoke(gui); }
            catch (Throwable ignored) {}
        }
        if (resolvedPoseField != null) {
            try {
                Object v = resolvedPoseField.get(gui);
                if (v instanceof PoseStack ps) return ps;
            } catch (Throwable ignored) {}
        }
        return new PoseStack();
    }

    private static void resolvePose() {
        poseResolved = true;
        for (Method m : GuiGraphics.class.getMethods()) {
            if (m.getName().equals("pose") && m.getParameterCount() == 0 && m.getReturnType() == PoseStack.class) {
                m.setAccessible(true);
                resolvedPose = m;
                return;
            }
        }
        for (Field f : GuiGraphics.class.getDeclaredFields()) {
            if (f.getType() == PoseStack.class) {
                f.setAccessible(true);
                resolvedPoseField = f;
                return;
            }
        }
    }
}