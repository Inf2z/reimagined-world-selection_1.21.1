package com.inf2z.reimagined_world_select.util;

import com.inf2z.reimagined_world_select.Config;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomIconManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomIconManager.class);
    private static final Map<String, CachedAsset> ASSET_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, WorldIconState> WORLD_STATES = new ConcurrentHashMap<>();

    private CustomIconManager() {}

    @Nullable
    public static Path getCustomIconDirectory(String worldId) {
        try {
            if (worldId == null || worldId.isBlank()) return null;
            return Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("saves").resolve(worldId).resolve("rws_custom_icons");
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    public static Path ensureCustomIconDirectory(String worldId) throws Exception {
        Path dir = getCustomIconDirectory(worldId);
        if (dir == null) throw new IllegalStateException("Invalid world id");
        Files.createDirectories(dir);
        return dir;
    }

    @Nonnull
    public static DisplayedIcon getDisplayedIcon(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);

        if (state == null) {
            startAsyncLoad(worldId);
            return DisplayedIcon.EMPTY;
        }

        if (state.loading) {
            if (state.assets != null && !state.assets.isEmpty()) {
                return buildDisplayedIcon(state);
            }
            return DisplayedIcon.EMPTY;
        }

        if (state.assets == null || state.assets.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - state.lastScanTime > 5000L) {
                startAsyncLoad(worldId);
            }
            return DisplayedIcon.EMPTY;
        }

        long now = System.currentTimeMillis();
        if (now - state.lastScanTime > 5000L) {
            startAsyncRescan(worldId);
        }

        return buildDisplayedIcon(state);
    }

    public static boolean hasActiveSlideshow(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);
        return Config.CUSTOM_ICON_SLIDESHOW.get() && state != null && state.assets != null && state.assets.size() > 1;
    }

    public static boolean hasAnyCustomIcon(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);
        return state != null && state.assets != null && !state.assets.isEmpty();
    }

    public static void preloadWorld(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);
        if (state == null || (!state.loading && System.currentTimeMillis() - state.lastScanTime > 5000L)) {
            startAsyncLoad(worldId);
        }
    }

    public static void releaseWorld(String worldId) {
        WORLD_STATES.remove(worldId);
        Iterator<Map.Entry<String, CachedAsset>> iterator = ASSET_CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedAsset> entry = iterator.next();
            if (entry.getValue().worldId().equals(worldId)) {
                releaseAsset(entry.getValue().asset());
                iterator.remove();
            }
        }
    }

    public static void clear() {
        WORLD_STATES.clear();
        for (CachedAsset asset : ASSET_CACHE.values()) releaseAsset(asset.asset());
        ASSET_CACHE.clear();
    }

    private static void startAsyncLoad(String worldId) {
        WorldIconState state = WORLD_STATES.computeIfAbsent(worldId, k -> new WorldIconState());
        if (state.loading) return;
        state.loading = true;

        CompletableFuture.supplyAsync(() -> {
            List<Path> files = listAvailableIconFiles(worldId);
            List<PreparedImage> prepared = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                PreparedImage img = prepareImage(files.get(i), i);
                if (img != null) prepared.add(img);
            }
            return prepared;
        }).thenAcceptAsync(prepared -> {
            try {
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s == null) return;

                List<IconAsset> assets = new ArrayList<>();
                for (PreparedImage img : prepared) {
                    IconAsset asset = registerPreparedImage(worldId, img);
                    if (asset != null) assets.add(asset);
                }

                s.assets = List.copyOf(assets);
                s.lastScanTime = System.currentTimeMillis();
                if (s.slideshowStartTime == 0L) s.slideshowStartTime = System.currentTimeMillis();
                s.loading = false;
            } catch (Exception e) {
                LOGGER.warn("Failed to finalize icon load for world '{}'", worldId, e);
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s != null) { s.loading = false; s.lastScanTime = System.currentTimeMillis(); }
            }
        }, Minecraft.getInstance()).exceptionally(t -> {
            LOGGER.warn("Failed to load custom icons for world '{}'", worldId, t);
            WorldIconState s = WORLD_STATES.get(worldId);
            if (s != null) { s.loading = false; s.lastScanTime = System.currentTimeMillis(); }
            return null;
        });
    }

    private static void startAsyncRescan(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);
        if (state == null || state.loading) return;
        state.loading = true;
        state.lastScanTime = System.currentTimeMillis();

        CompletableFuture.supplyAsync(() -> {
            List<Path> files = listAvailableIconFiles(worldId);
            List<PreparedImage> newImages = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                String cacheKey = buildCacheKey(files.get(i));
                if (cacheKey != null && ASSET_CACHE.containsKey(cacheKey)) continue;
                PreparedImage img = prepareImage(files.get(i), i);
                if (img != null) newImages.add(img);
            }
            return new RescanResult(files, newImages);
        }).thenAcceptAsync(result -> {
            try {
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s == null) return;

                for (PreparedImage img : result.newImages) {
                    registerPreparedImage(worldId, img);
                }

                List<IconAsset> newAssets = new ArrayList<>();
                for (int i = 0; i < result.files.size(); i++) {
                    Path path = result.files.get(i);
                    String cacheKey = buildCacheKey(path);
                    if (cacheKey != null) {
                        CachedAsset cached = ASSET_CACHE.get(cacheKey);
                        if (cached != null) { newAssets.add(cached.asset()); continue; }
                    }
                }

                if (!newAssets.isEmpty()) {
                    s.assets = List.copyOf(newAssets);
                }
                s.loading = false;
            } catch (Exception e) {
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s != null) s.loading = false;
            }
        }, Minecraft.getInstance()).exceptionally(t -> {
            WorldIconState s = WORLD_STATES.get(worldId);
            if (s != null) s.loading = false;
            return null;
        });
    }

    @Nonnull
    private static DisplayedIcon buildDisplayedIcon(WorldIconState state) {
        List<IconAsset> assets = state.assets;
        if (assets == null || assets.isEmpty()) return DisplayedIcon.EMPTY;

        long now = System.currentTimeMillis();

        if (assets.size() == 1 || !Config.CUSTOM_ICON_SLIDESHOW.get()) {
            IconAsset asset = assets.getFirst();
            return new DisplayedIcon(
                    List.of(new IconLayer(asset.textureAt(now), asset.width(), asset.height(), 1.0f)),
                    asset.width(), asset.height()
            );
        }

        long startTime = state.slideshowStartTime > 0 ? state.slideshowStartTime : now;
        long displayMs = Math.max(1000L, Config.CUSTOM_ICON_SLIDESHOW_INTERVAL_SECONDS.get() * 1000L);
        long fadeMs = Math.max(100L, Math.round(Config.CUSTOM_ICON_SLIDESHOW_FADE_SECONDS.get().doubleValue() * 1000.0));
        long segmentMs = displayMs + fadeMs;
        long totalCycle = segmentMs * assets.size();

        long elapsed = Math.max(0L, now - startTime);
        long loopedElapsed = totalCycle <= 0L ? 0L : elapsed % totalCycle;

        int currentIndex = (int) (loopedElapsed / segmentMs);
        long timeInsideSegment = loopedElapsed % segmentMs;
        int nextIndex = (currentIndex + 1) % assets.size();

        IconAsset currentAsset = assets.get(currentIndex);
        IconAsset nextAsset = assets.get(nextIndex);

        if (timeInsideSegment < displayMs) {
            return new DisplayedIcon(
                    List.of(new IconLayer(currentAsset.textureAt(now), currentAsset.width(), currentAsset.height(), 1.0f)),
                    currentAsset.width(), currentAsset.height()
            );
        }

        float progress = Math.max(0.0f, Math.min(1.0f, (float) (timeInsideSegment - displayMs) / (float) fadeMs));
        float smooth = progress * progress * (3.0f - 2.0f * progress);

        return new DisplayedIcon(
                List.of(
                        new IconLayer(currentAsset.textureAt(now), currentAsset.width(), currentAsset.height(), 1.0f - smooth),
                        new IconLayer(nextAsset.textureAt(now), nextAsset.width(), nextAsset.height(), smooth)
                ),
                currentAsset.width(), currentAsset.height()
        );
    }

    @Nonnull
    private static List<Path> listAvailableIconFiles(String worldId) {
        List<Path> files = new ArrayList<>();
        Path dir = getCustomIconDirectory(worldId);
        if (dir == null || !Files.isDirectory(dir)) return files;

        try (var stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                if (!Files.isRegularFile(path)) continue;
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".png") || name.endsWith(".gif")) files.add(path);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to list custom icons for world '{}'", worldId, e);
        }

        files.sort((a, b) -> naturalCompare(
                a.getFileName().toString().toLowerCase(Locale.ROOT),
                b.getFileName().toString().toLowerCase(Locale.ROOT)
        ));

        return files;
    }

    private static int naturalCompare(String a, String b) {
        int ia = 0, ib = 0;
        while (ia < a.length() && ib < b.length()) {
            char ca = a.charAt(ia), cb = b.charAt(ib);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int sa = ia, sb = ib;
                while (ia < a.length() && Character.isDigit(a.charAt(ia))) ia++;
                while (ib < b.length() && Character.isDigit(b.charAt(ib))) ib++;
                try {
                    long va = Long.parseLong(a.substring(sa, ia));
                    long vb = Long.parseLong(b.substring(sb, ib));
                    if (va != vb) return Long.compare(va, vb);
                } catch (NumberFormatException ignored) {
                    int cmp = a.substring(sa, ia).compareTo(b.substring(sb, ib));
                    if (cmp != 0) return cmp;
                }
            } else {
                if (ca != cb) return Character.compare(ca, cb);
                ia++; ib++;
            }
        }
        return Integer.compare(a.length(), b.length());
    }

    @Nullable
    private static String buildCacheKey(Path path) {
        try {
            long stamp = Files.getLastModifiedTime(path).toMillis();
            return path.toAbsolutePath().normalize() + "|" + stamp;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static PreparedImage prepareImage(Path path, int orderIndex) {
        try {
            long stamp = Files.getLastModifiedTime(path).toMillis();
            String cacheKey = path.toAbsolutePath().normalize() + "|" + stamp;
            if (ASSET_CACHE.containsKey(cacheKey)) return null;

            String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".gif")) {
                return prepareGif(path, orderIndex, stamp, cacheKey);
            } else {
                return prepareStatic(path, orderIndex, stamp, cacheKey);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to prepare image '{}'", path, e);
            return null;
        }
    }

    @Nullable
    private static PreparedImage prepareStatic(Path path, int orderIndex, long stamp, String cacheKey) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(bytes));
            if (buffered == null) return null;
            int width = buffered.getWidth();
            int height = buffered.getHeight();
            return new PreparedImage(cacheKey, orderIndex, stamp, width, height, List.of(new PreparedFrame(bytes, 0)), false);
        } catch (Exception e) {
            LOGGER.warn("Failed to prepare static image '{}'", path, e);
            return null;
        }
    }

    @Nullable
    private static PreparedImage prepareGif(Path path, int orderIndex, long stamp, String cacheKey) {
        try (InputStream is = Files.newInputStream(path); ImageInputStream imageInput = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, false);

                int[] logicalSize = readLogicalScreenSize(reader);
                int canvasWidth = logicalSize[0], canvasHeight = logicalSize[1];
                if (canvasWidth <= 0 || canvasHeight <= 0) {
                    canvasWidth = reader.getWidth(0);
                    canvasHeight = reader.getHeight(0);
                }

                int frameCount = reader.getNumImages(true);
                if (frameCount <= 0) return null;

                BufferedImage master = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                List<PreparedFrame> frames = new ArrayList<>();

                for (int i = 0; i < frameCount; i++) {
                    BufferedImage frame = reader.read(i);
                    GifFrameMetadata metadata = readGifFrameMetadata(reader.getImageMetadata(i));
                    BufferedImage backup = copyImage(master);

                    Graphics2D g = master.createGraphics();
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(frame, metadata.left(), metadata.top(), null);
                    g.dispose();

                    BufferedImage composed = copyImage(master);
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        ImageIO.write(composed, "png", out);
                        int delay = Math.max(20, metadata.delayMs());
                        frames.add(new PreparedFrame(out.toByteArray(), delay));
                    }

                    if ("restoreToPrevious".equals(metadata.disposalMethod())) {
                        master = backup;
                    } else if ("restoreToBackgroundColor".equals(metadata.disposalMethod())) {
                        Graphics2D clear = master.createGraphics();
                        clear.setComposite(AlphaComposite.Clear);
                        clear.fillRect(metadata.left(), metadata.top(), frame.getWidth(), frame.getHeight());
                        clear.dispose();
                    }
                }

                if (frames.isEmpty()) return null;
                return new PreparedImage(cacheKey, orderIndex, stamp, canvasWidth, canvasHeight, frames, true);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to prepare gif '{}'", path, e);
            return null;
        }
    }

    @Nullable
    private static IconAsset registerPreparedImage(String worldId, PreparedImage prepared) {
        try {
            if (ASSET_CACHE.containsKey(prepared.cacheKey)) {
                return ASSET_CACHE.get(prepared.cacheKey).asset();
            }

            if (!prepared.isGif) {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(prepared.frames.getFirst().data));
                ResourceLocation texture = registerTexture(worldId, prepared.orderIndex, 0, prepared.stamp, image);
                IconAsset asset = new StaticIconAsset(texture, prepared.width, prepared.height);
                ASSET_CACHE.put(prepared.cacheKey, new CachedAsset(worldId, asset));
                return asset;
            } else {
                List<GifFrame> gifFrames = new ArrayList<>();
                int totalDuration = 0;
                for (int i = 0; i < prepared.frames.size(); i++) {
                    PreparedFrame pf = prepared.frames.get(i);
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(pf.data));
                    ResourceLocation texture = registerTexture(worldId, prepared.orderIndex, i, prepared.stamp, image);
                    gifFrames.add(new GifFrame(texture, pf.delayMs));
                    totalDuration += pf.delayMs;
                }
                if (gifFrames.isEmpty()) return null;
                IconAsset asset = new GifIconAsset(gifFrames, prepared.width, prepared.height, totalDuration);
                ASSET_CACHE.put(prepared.cacheKey, new CachedAsset(worldId, asset));
                return asset;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register prepared image for world '{}'", worldId, e);
            return null;
        }
    }

    private static int[] readLogicalScreenSize(ImageReader reader) {
        try {
            IIOMetadata metadata = reader.getStreamMetadata();
            if (metadata == null || metadata.getNativeMetadataFormatName() == null)
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            Node descriptor = findNode(root, "LogicalScreenDescriptor");
            if (descriptor == null) return new int[]{reader.getWidth(0), reader.getHeight(0)};
            int width = getIntAttribute(descriptor, "logicalScreenWidth", reader.getWidth(0));
            int height = getIntAttribute(descriptor, "logicalScreenHeight", reader.getHeight(0));
            return new int[]{width, height};
        } catch (Exception e) {
            try { return new int[]{reader.getWidth(0), reader.getHeight(0)}; }
            catch (Exception ignored) { return new int[]{64, 64}; }
        }
    }

    @Nonnull
    private static GifFrameMetadata readGifFrameMetadata(@Nullable IIOMetadata metadata) {
        if (metadata == null || metadata.getNativeMetadataFormatName() == null)
            return new GifFrameMetadata(0, 0, 100, "none");
        try {
            Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            Node imageDescriptor = findNode(root, "ImageDescriptor");
            Node graphicControl = findNode(root, "GraphicControlExtension");
            int left = imageDescriptor != null ? getIntAttribute(imageDescriptor, "imageLeftPosition", 0) : 0;
            int top = imageDescriptor != null ? getIntAttribute(imageDescriptor, "imageTopPosition", 0) : 0;
            int delay = graphicControl != null ? getIntAttribute(graphicControl, "delayTime", 10) * 10 : 100;
            String disposal = graphicControl != null ? getStringAttribute(graphicControl, "disposalMethod", "none") : "none";
            return new GifFrameMetadata(left, top, delay <= 0 ? 100 : delay, disposal);
        } catch (Exception e) {
            return new GifFrameMetadata(0, 0, 100, "none");
        }
    }

    @Nullable
    private static Node findNode(@Nullable Node root, String name) {
        if (root == null) return null;
        if (name.equals(root.getNodeName())) return root;
        Node child = root.getFirstChild();
        while (child != null) {
            Node found = findNode(child, name);
            if (found != null) return found;
            child = child.getNextSibling();
        }
        return null;
    }

    private static int getIntAttribute(Node node, String name, int fallback) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) return fallback;
        Node attr = attributes.getNamedItem(name);
        if (attr == null) return fallback;
        try { return Integer.parseInt(attr.getNodeValue()); }
        catch (Exception e) { return fallback; }
    }

    @Nonnull
    private static String getStringAttribute(Node node, String name, String fallback) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) return fallback;
        Node attr = attributes.getNamedItem(name);
        if (attr == null) return fallback;
        String value = attr.getNodeValue();
        return value == null ? fallback : value;
    }

    @Nonnull
    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    @Nonnull
    private static ResourceLocation registerTexture(String worldId, int orderIndex, int frameIndex, long stamp, NativeImage image) {
        String safeWorld = worldId.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./\\-]", "_").replaceAll("_+", "_");
        long hash = (stamp ^ ((long) orderIndex << 16) ^ frameIndex) & 0xFFFFFFFFL;
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                "reimagined_world_select",
                "custom_icons/" + safeWorld + "_" + orderIndex + "_" + frameIndex + "_" + hash
        );
        DynamicTexture texture = TextureCompat.createFromImage(image);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        return location;
    }

    private static void releaseAsset(IconAsset asset) {
        if (Minecraft.getInstance() == null) return;
        for (ResourceLocation texture : asset.textures()) {
            Minecraft.getInstance().getTextureManager().release(texture);
        }
    }

    public record IconLayer(ResourceLocation texture, int width, int height, float alpha) {}

    public record DisplayedIcon(List<IconLayer> layers, int referenceWidth, int referenceHeight) {
        public static final DisplayedIcon EMPTY = new DisplayedIcon(List.of(), 64, 64);
        public boolean isPresent() { return !layers.isEmpty(); }
    }

    private interface IconAsset {
        int width();
        int height();
        ResourceLocation textureAt(long timeMs);
        List<ResourceLocation> textures();
    }

    private record StaticIconAsset(ResourceLocation texture, int width, int height) implements IconAsset {
        @Override public ResourceLocation textureAt(long timeMs) { return texture; }
        @Override public List<ResourceLocation> textures() { return List.of(texture); }
    }

    private record GifFrame(ResourceLocation texture, int delayMs) {}

    private record GifIconAsset(List<GifFrame> frames, int width, int height, int totalDuration) implements IconAsset {
        @Override
        public ResourceLocation textureAt(long timeMs) {
            if (frames.isEmpty()) throw new IllegalStateException("No gif frames");
            if (totalDuration <= 0) return frames.getFirst().texture();
            long time = Math.floorMod(timeMs, totalDuration);
            for (GifFrame frame : frames) {
                if (time < frame.delayMs()) return frame.texture();
                time -= frame.delayMs();
            }
            return frames.getLast().texture();
        }

        @Override
        public List<ResourceLocation> textures() {
            List<ResourceLocation> list = new ArrayList<>(frames.size());
            for (GifFrame frame : frames) list.add(frame.texture());
            return list;
        }
    }

    private record CachedAsset(String worldId, IconAsset asset) {}
    private record GifFrameMetadata(int left, int top, int delayMs, String disposalMethod) {}
    private record PreparedFrame(byte[] data, int delayMs) {}
    private record PreparedImage(String cacheKey, int orderIndex, long stamp, int width, int height, List<PreparedFrame> frames, boolean isGif) {}
    private record RescanResult(List<Path> files, List<PreparedImage> newImages) {}

    private static class WorldIconState {
        volatile List<IconAsset> assets;
        volatile long lastScanTime;
        volatile long slideshowStartTime;
        volatile boolean loading;
    }
}