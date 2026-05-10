package com.inf2z.reimagined_world_selection.util;

import net.minecraft.client.gui.components.AbstractWidget;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class WidgetCompat {

    private static final MethodHandle SET_X;
    private static final MethodHandle SET_Y;
    private static final MethodHandle SET_WIDTH;
    private static final MethodHandle GET_X;
    private static final MethodHandle GET_Y;
    private static final MethodHandle GET_WIDTH;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        SET_X = findMethod(lookup, "setX", void.class, int.class);
        SET_Y = findMethod(lookup, "setY", void.class, int.class);
        SET_WIDTH = findMethod(lookup, "setWidth", void.class, int.class);
        GET_X = findMethod(lookup, "getX", int.class);
        GET_Y = findMethod(lookup, "getY", int.class);
        GET_WIDTH = findMethod(lookup, "getWidth", int.class);
    }

    private static MethodHandle findMethod(MethodHandles.Lookup lookup, String name, Class<?> returnType, Class<?>... params) {
        try {
            return lookup.findVirtual(AbstractWidget.class, name, MethodType.methodType(returnType, params));
        } catch (Throwable ignored) {
        }
        return null;
    }

    private WidgetCompat() {
    }

    public static void setX(AbstractWidget widget, int x) {
        try {
            if (SET_X != null) {
                SET_X.invoke(widget, x);
            } else {
                widget.setX(x);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void setY(AbstractWidget widget, int y) {
        try {
            if (SET_Y != null) {
                SET_Y.invoke(widget, y);
            } else {
                widget.setY(y);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void setWidth(AbstractWidget widget, int width) {
        try {
            if (SET_WIDTH != null) {
                SET_WIDTH.invoke(widget, width);
            } else {
                widget.setWidth(width);
            }
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int getX(AbstractWidget widget) {
        try {
            if (GET_X != null) {
                return (int) GET_X.invoke(widget);
            }
            return widget.getX();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int getY(AbstractWidget widget) {
        try {
            if (GET_Y != null) {
                return (int) GET_Y.invoke(widget);
            }
            return widget.getY();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int getWidth(AbstractWidget widget) {
        try {
            if (GET_WIDTH != null) {
                return (int) GET_WIDTH.invoke(widget);
            }
            return widget.getWidth();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}