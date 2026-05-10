package com.inf2z.reimagined_world_selection;

import com.inf2z.reimagined_world_selection.screen.CustomLinesScreen;
import com.inf2z.reimagined_world_selection.screen.PanelOrderScreen;
import com.inf2z.reimagined_world_selection.util.GuiCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.function.Supplier;

@Mod(value = "reimagined_world_selection", dist = Dist.CLIENT)
public class ReimaginedWorldSelection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReimaginedWorldSelection.class);
    private static ModContainer savedContainer;

    public ReimaginedWorldSelection(ModContainer container) {
        savedContainer = container;
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        registerConfigScreen(container);
    }

    private static void registerConfigScreen(ModContainer container) {
        try {
            Class<?> factoryClass = findConfigScreenFactoryClass();
            if (factoryClass == null) {
                LOGGER.warn("Could not find config screen factory class");
                return;
            }

            Object factoryInstance = createConfigScreenFactoryProxy(factoryClass);

            Method registerMethod = findRegisterExtensionPointMethod(factoryClass);
            if (registerMethod == null) {
                LOGGER.warn("Could not find compatible ModContainer.registerExtensionPoint method");
                return;
            }

            registerMethod.setAccessible(true);

            Class<?> secondParam = registerMethod.getParameterTypes()[1];

            if (secondParam.isInstance(factoryInstance) || secondParam == Object.class) {
                registerMethod.invoke(container, factoryClass, factoryInstance);
                LOGGER.info("Registered config screen factory using direct factory instance: {}", registerMethod);
                return;
            }

            if (Supplier.class.isAssignableFrom(secondParam)) {
                Supplier<Object> supplier = () -> factoryInstance;
                registerMethod.invoke(container, factoryClass, supplier);
                LOGGER.info("Registered config screen factory using Supplier: {}", registerMethod);
                return;
            }

            LOGGER.warn("Unsupported registerExtensionPoint signature: {}", registerMethod);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register config screen factory", t);
        }
    }

    private static Method findRegisterExtensionPointMethod(Class<?> factoryClass) {
        Method supplierMethod = null;
        Method directMethod = null;

        for (Method method : ModContainer.class.getMethods()) {
            if (!method.getName().equals("registerExtensionPoint")) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2) {
                continue;
            }

            if (params[0] != Class.class) {
                continue;
            }

            Class<?> second = params[1];

            if (second.isAssignableFrom(factoryClass) || second == Object.class) {
                directMethod = method;
            }

            if (Supplier.class.isAssignableFrom(second)) {
                supplierMethod = method;
            }
        }

        return directMethod != null ? directMethod : supplierMethod;
    }

    private static Class<?> findConfigScreenFactoryClass() {
        String[] candidates = {
                "net.neoforged.neoforge.client.gui.IConfigScreenFactory",
                "net.neoforged.neoforge.client.ConfigScreenHandler$ConfigScreenFactory",
                "net.neoforged.fml.client.ConfigGuiHandler$ConfigGuiFactory"
        };

        for (String name : candidates) {
            try {
                Class<?> cls = Class.forName(name);
                LOGGER.info("Found config screen factory class: {}", name);
                return cls;
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    private static Object createConfigScreenFactoryProxy(Class<?> factoryClass) {
        return Proxy.newProxyInstance(
                factoryClass.getClassLoader(),
                new Class<?>[]{factoryClass},
                (proxy, method, args) -> {
                    String name = method.getName();

                    if (name.equals("toString")) {
                        return "RWS Config Screen Factory";
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (name.equals("equals")) {
                        return proxy == args[0];
                    }

                    if (Modifier.isAbstract(method.getModifiers())) {
                        Screen parent = extractParentScreen(args);
                        return new ConfigMenuScreen(parent);
                    }

                    return null;
                }
        );
    }

    private static Screen extractParentScreen(Object[] args) {
        if (args != null) {
            for (int i = args.length - 1; i >= 0; i--) {
                if (args[i] instanceof Screen screen) {
                    return screen;
                }
            }
        }
        return new TitleScreen();
    }

    private static Screen createNeoForgeConfigScreen(Screen parent) {
        String[] candidates = {
                "net.neoforged.neoforge.client.gui.ConfigurationScreen"
        };

        for (String className : candidates) {
            try {
                Class<?> cls = Class.forName(className);

                try {
                    return (Screen) cls.getConstructor(ModContainer.class, Screen.class)
                            .newInstance(savedContainer, parent);
                } catch (NoSuchMethodException ignored) {
                }

                try {
                    return (Screen) cls.getConstructor(Screen.class, ModContainer.class)
                            .newInstance(parent, savedContainer);
                } catch (NoSuchMethodException ignored) {
                }
            } catch (Throwable t) {
                LOGGER.warn("Failed to create config screen via {}", className, t);
            }
        }

        return parent;
    }

    static class ConfigMenuScreen extends Screen {
        private final Screen parent;
        private static final int BUTTON_WIDTH = 200;
        private static final int BUTTON_HEIGHT = 20;
        private static final int HEADER_HEIGHT = 33;
        private static final int FOOTER_HEIGHT = 33;

        public ConfigMenuScreen(Screen parent) {
            super(Component.translatable("screen.reimagined_world_selection.config_menu"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2 - BUTTON_WIDTH / 2;
            int startY = 40;

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("screen.reimagined_world_selection.main_config"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(createNeoForgeConfigScreen(this));
                                }
                            }
                    ).bounds(centerX, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("screen.reimagined_world_selection.custom_info_editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new CustomLinesScreen(this));
                                }
                            }
                    ).bounds(centerX, startY + 25, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("screen.reimagined_world_selection.info_order_editor"),
                            btn -> {
                                if (this.minecraft != null) {
                                    this.minecraft.setScreen(new PanelOrderScreen(this));
                                }
                            }
                    ).bounds(centerX, startY + 50, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );

            this.addRenderableWidget(
                    Button.builder(
                            Component.translatable("gui.done"),
                            btn -> this.onClose()
                    ).bounds(this.width / 2 - BUTTON_WIDTH / 2, this.height - 26, BUTTON_WIDTH, BUTTON_HEIGHT).build()
            );
        }

        @Override
        public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            super.render(gui, mouseX, mouseY, partialTick);

            gui.fill(0, 31, this.width, 32, 0x33FFFFFF);
            gui.fill(0, 32, this.width, 33, 0xFF000000);
            gui.fill(0, this.height - 33, this.width, this.height - 32, 0xFF000000);
            gui.fill(0, this.height - 32, this.width, this.height - 31, 0x33FFFFFF);

            GuiCompat.drawCenteredString(gui, this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        }

        @Override
        protected void renderMenuBackground(@NotNull GuiGraphics gui) {
            super.renderMenuBackground(gui);
            gui.fill(0, 33, this.width, this.height - 33, 0x70000000);
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }
    }
}