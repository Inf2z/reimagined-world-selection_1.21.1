package com.inf2z.reimagined_world_select.util;

import net.minecraft.client.gui.components.AbstractSelectionList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("rawtypes")
public final class SelectionListCompat {

    private SelectionListCompat() {
    }

    public static double getScrollAmount(AbstractSelectionList list) {
        for (String name : new String[]{"getScrollAmount", "scrollAmount", "getScrollOffset"}) {
            try {
                Method m = AbstractSelectionList.class.getMethod(name);
                Object result = m.invoke(list);
                if (result instanceof Number n) {
                    return n.doubleValue();
                }
            } catch (Throwable ignored) {
            }
        }

        for (Method m : AbstractSelectionList.class.getDeclaredMethods()) {
            try {
                if (m.getParameterCount() == 0 &&
                        (m.getReturnType() == double.class || m.getReturnType() == float.class) &&
                        m.getName().toLowerCase().contains("scroll")) {
                    m.setAccessible(true);
                    Object result = m.invoke(list);
                    if (result instanceof Number n) {
                        return n.doubleValue();
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        for (Field f : AbstractSelectionList.class.getDeclaredFields()) {
            try {
                if ((f.getType() == double.class || f.getType() == float.class) &&
                        f.getName().toLowerCase().contains("scroll")) {
                    f.setAccessible(true);
                    Object result = f.get(list);
                    if (result instanceof Number n) {
                        return n.doubleValue();
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return 0.0D;
    }
}