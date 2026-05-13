package com.inf2z.reimagined_world_select.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class TextureCompat {

    private TextureCompat() {
    }

    public static DynamicTexture createFromImage(NativeImage image) {
        for (Constructor<?> ctor : DynamicTexture.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();

            if (params.length == 1 && params[0] == NativeImage.class) {
                try {
                    ctor.setAccessible(true);
                    return (DynamicTexture) ctor.newInstance(image);
                } catch (Throwable ignored) {
                }
            }
        }

        for (Constructor<?> ctor : DynamicTexture.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();

            if (params.length == 2
                    && Supplier.class.isAssignableFrom(params[0])
                    && params[1] == NativeImage.class) {
                try {
                    ctor.setAccessible(true);
                    Supplier<String> debugName = () -> "rws_icon";
                    return (DynamicTexture) ctor.newInstance(debugName, image);
                } catch (Throwable ignored) {
                }
            }
        }

        for (Constructor<?> ctor : DynamicTexture.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();

            if (params.length == 4
                    && Supplier.class.isAssignableFrom(params[0])
                    && params[1] == int.class
                    && params[2] == int.class
                    && params[3] == boolean.class) {
                try {
                    ctor.setAccessible(true);
                    Supplier<String> debugName = () -> "rws_icon";
                    DynamicTexture texture = (DynamicTexture) ctor.newInstance(
                            debugName, image.getWidth(), image.getHeight(), false
                    );
                    copyPixelsAndUpload(texture, image);
                    return texture;
                } catch (Throwable ignored) {
                }
            }

            if (params.length == 4
                    && params[0] == String.class
                    && params[1] == int.class
                    && params[2] == int.class
                    && params[3] == boolean.class) {
                try {
                    ctor.setAccessible(true);
                    DynamicTexture texture = (DynamicTexture) ctor.newInstance(
                            "rws_icon", image.getWidth(), image.getHeight(), false
                    );
                    copyPixelsAndUpload(texture, image);
                    return texture;
                } catch (Throwable ignored) {
                }
            }
        }

        for (Constructor<?> ctor : DynamicTexture.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();

            if (params.length == 3
                    && params[0] == int.class
                    && params[1] == int.class
                    && params[2] == boolean.class) {
                try {
                    ctor.setAccessible(true);
                    DynamicTexture texture = (DynamicTexture) ctor.newInstance(
                            image.getWidth(), image.getHeight(), false
                    );
                    copyPixelsAndUpload(texture, image);
                    return texture;
                } catch (Throwable ignored) {
                }
            }
        }

        throw new UnsupportedOperationException(
                "No compatible DynamicTexture constructor found. Available: " + getConstructorInfo()
        );
    }

    private static void copyPixelsAndUpload(DynamicTexture texture, NativeImage source) {
        try {
            NativeImage pixels = null;

            for (Method m : DynamicTexture.class.getDeclaredMethods()) {
                if (m.getReturnType() == NativeImage.class && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    pixels = (NativeImage) m.invoke(texture);
                    if (pixels != null) break;
                }
            }

            if (pixels != null) {
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        pixels.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
                    }
                }
            }

            for (Method m : DynamicTexture.class.getDeclaredMethods()) {
                if (m.getName().equals("upload") && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    m.invoke(texture);
                    break;
                }
            }

            source.close();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to copy pixels to DynamicTexture", t);
        }
    }

    private static String getConstructorInfo() {
        StringBuilder sb = new StringBuilder();
        for (Constructor<?> ctor : DynamicTexture.class.getDeclaredConstructors()) {
            sb.append("\n  ").append(ctor);
        }
        return sb.toString();
    }
}