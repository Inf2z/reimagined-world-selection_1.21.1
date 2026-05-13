package com.inf2z.reimagined_world_select.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public final class ScreenshotCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotCompat.class);

    private ScreenshotCompat() {
    }

    public static boolean takeScreenshot(Minecraft mc, Consumer<NativeImage> consumer) {
        Object target = mc.getMainRenderTarget();

        for (Method method : Screenshot.class.getDeclaredMethods()) {
            if (!method.getName().equals("takeScreenshot")) continue;
            if (!Modifier.isStatic(method.getModifiers())) continue;

            Class<?>[] params = method.getParameterTypes();

            try {
                method.setAccessible(true);

                if (params.length == 1 && params[0].isInstance(target) && method.getReturnType() == NativeImage.class) {
                    NativeImage image = (NativeImage) method.invoke(null, target);
                    if (image != null) {
                        consumer.accept(image);
                        return true;
                    }
                }

                if (params.length == 2
                        && params[0].isInstance(target)
                        && Consumer.class.isAssignableFrom(params[1])) {
                    method.invoke(null, target, consumer);
                    return true;
                }

                if (params.length == 3
                        && params[0].isInstance(target)
                        && Consumer.class.isAssignableFrom(params[1])
                        && params[2] == String.class) {
                    method.invoke(null, target, consumer, "screenshot");
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }

        LOGGER.warn("No compatible Screenshot.takeScreenshot overload found");
        return false;
    }
}