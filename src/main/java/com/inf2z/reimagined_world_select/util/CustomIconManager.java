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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CustomIconManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomIconManager.class);
    private static final Map<String, CachedAsset> ASSET_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, WorldIconState> WORLD_STATES = new ConcurrentHashMap<>();
    private static final Queue<Runnable> PENDING = new ConcurrentLinkedQueue<>();
    private static final Set<String> HAS_CUSTOM_ICONS_DIR = ConcurrentHashMap.newKeySet();
    private static final Set<String> DIR_CHECKED = ConcurrentHashMap.newKeySet();

    private CustomIconManager() {}

    public static void processPendingRegistrations() {
        Runnable task;
        int limit = 100;
        while ((task = PENDING.poll()) != null && limit-- > 0) {
            try { task.run(); } catch (Exception e) { LOGGER.warn("Failed pending icon registration", e); }
        }
    }

    @Nullable
    public static Path getCustomIconDirectory(String worldId) {
        try {
            if (worldId == null || worldId.isBlank()) return null;
            return Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(worldId).resolve("rws_custom_icons");
        } catch (Exception e) { return null; }
    }

    @Nonnull
    public static Path ensureCustomIconDirectory(String worldId) throws Exception {
        Path dir = getCustomIconDirectory(worldId);
        if (dir == null) throw new IllegalStateException("Invalid world id");
        Files.createDirectories(dir);
        return dir;
    }

    public static boolean hasCustomIconsOnDisk(String worldId) {
        if (!DIR_CHECKED.contains(worldId)) {
            DIR_CHECKED.add(worldId);
            Path dir = getCustomIconDirectory(worldId);
            if (dir != null && Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    boolean any = stream.anyMatch(p -> {
                        if (!Files.isRegularFile(p)) return false;
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".png") || n.endsWith(".gif");
                    });
                    if (any) HAS_CUSTOM_ICONS_DIR.add(worldId);
                } catch (Exception ignored) {}
            }
        }
        return HAS_CUSTOM_ICONS_DIR.contains(worldId);
    }

    @Nonnull
    public static DisplayedIcon getDisplayedIcon(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);

        if (state == null) {
            startLoadWithFirstSync(worldId);
            state = WORLD_STATES.get(worldId);
            if (state == null) return DisplayedIcon.EMPTY;
        }

        if (state.assets != null && !state.assets.isEmpty()) {
            if (!state.loading && System.currentTimeMillis() - state.lastScanTime > 5000L) {
                startAsyncRescan(worldId);
            }
            return buildDisplayedIcon(state);
        }

        if (state.loading) return DisplayedIcon.EMPTY;

        if (System.currentTimeMillis() - state.lastScanTime > 5000L) {
            startLoadWithFirstSync(worldId);
            state = WORLD_STATES.get(worldId);
            if (state != null && state.assets != null && !state.assets.isEmpty()) {
                return buildDisplayedIcon(state);
            }
        }

        return DisplayedIcon.EMPTY;
    }

    public static boolean hasActiveSlideshow(String worldId) {
        WorldIconState s = WORLD_STATES.get(worldId);
        return Config.CUSTOM_ICON_SLIDESHOW.get() && s != null && s.assets != null && s.assets.size() > 1;
    }

    public static boolean hasAnyCustomIcon(String worldId) {
        WorldIconState s = WORLD_STATES.get(worldId);
        if (s != null && s.assets != null && !s.assets.isEmpty()) return true;
        return hasCustomIconsOnDisk(worldId);
    }

    public static void preloadWorld(String worldId) {
        WorldIconState s = WORLD_STATES.get(worldId);
        if (s == null) {
            startLoadWithFirstSync(worldId);
        } else if (!s.loading && System.currentTimeMillis() - s.lastScanTime > 5000L) {
            startAsyncRescan(worldId);
        }
    }

    public static void releaseWorld(String worldId) {
        WORLD_STATES.remove(worldId);
        DIR_CHECKED.remove(worldId);
        HAS_CUSTOM_ICONS_DIR.remove(worldId);
        Iterator<Map.Entry<String, CachedAsset>> it = ASSET_CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CachedAsset> e = it.next();
            if (e.getValue().worldId().equals(worldId)) { releaseAsset(e.getValue().asset()); it.remove(); }
        }
    }

    public static void clear() {
        WORLD_STATES.clear(); PENDING.clear(); DIR_CHECKED.clear(); HAS_CUSTOM_ICONS_DIR.clear();
        for (CachedAsset a : ASSET_CACHE.values()) releaseAsset(a.asset());
        ASSET_CACHE.clear();
    }

    private static void startLoadWithFirstSync(String worldId) {
        WorldIconState state = WORLD_STATES.computeIfAbsent(worldId, k -> new WorldIconState());
        if (state.loading) return;

        List<Path> files = listAvailableIconFiles(worldId);
        if (files.isEmpty()) {
            state.lastScanTime = System.currentTimeMillis();
            return;
        }

        List<IconAsset> syncAssets = new ArrayList<>();
        Path firstFile = files.getFirst();
        IconAsset firstAsset = loadAssetSync(worldId, 0, firstFile);
        if (firstAsset != null) syncAssets.add(firstAsset);

        state.assets = List.copyOf(syncAssets);
        state.lastScanTime = System.currentTimeMillis();
        if (state.slideshowStartTime == 0L) state.slideshowStartTime = System.currentTimeMillis();

        if (files.size() > 1) {
            state.loading = true;
            List<Path> remainingFiles = files.subList(1, files.size());

            CompletableFuture.supplyAsync(() -> {
                List<PreparedImage> prepared = new ArrayList<>();
                for (int i = 0; i < remainingFiles.size(); i++) {
                    PreparedImage img = prepareImage(remainingFiles.get(i), i + 1);
                    if (img != null) prepared.add(img);
                }
                return prepared;
            }).thenAccept(prepared -> PENDING.add(() -> {
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s == null) return;
                try {
                    List<IconAsset> allAssets = new ArrayList<>();
                    if (s.assets != null) allAssets.addAll(s.assets);
                    for (PreparedImage img : prepared) {
                        IconAsset a = registerPreparedImage(worldId, img);
                        if (a != null) allAssets.add(a);
                    }
                    s.assets = List.copyOf(allAssets);
                    s.lastScanTime = System.currentTimeMillis();
                } catch (Exception e) { LOGGER.warn("Icon load failed for '{}'", worldId, e); }
                s.loading = false;
            })).exceptionally(t -> {
                WorldIconState s = WORLD_STATES.get(worldId);
                if (s != null) { s.loading = false; s.lastScanTime = System.currentTimeMillis(); }
                return null;
            });
        }
    }

    private static void startAsyncRescan(String worldId) {
        WorldIconState state = WORLD_STATES.get(worldId);
        if (state == null || state.loading) return;
        state.loading = true;
        state.lastScanTime = System.currentTimeMillis();

        CompletableFuture.supplyAsync(() -> {
            List<Path> files = listAvailableIconFiles(worldId);
            List<PreparedImage> newImgs = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                String ck = buildCacheKey(files.get(i));
                if (ck != null && ASSET_CACHE.containsKey(ck)) continue;
                PreparedImage img = prepareImage(files.get(i), i);
                if (img != null) newImgs.add(img);
            }
            return new RescanResult(files, newImgs);
        }).thenAccept(result -> PENDING.add(() -> {
            WorldIconState s = WORLD_STATES.get(worldId);
            if (s == null) return;
            try {
                for (PreparedImage img : result.newImages) registerPreparedImage(worldId, img);
                List<IconAsset> newAssets = new ArrayList<>();
                for (Path path : result.files) {
                    String ck = buildCacheKey(path);
                    if (ck != null) {
                        CachedAsset ca = ASSET_CACHE.get(ck);
                        if (ca != null) newAssets.add(ca.asset());
                    }
                }
                if (!newAssets.isEmpty()) s.assets = List.copyOf(newAssets);
            } catch (Exception ignored) {}
            s.loading = false;
        })).exceptionally(t -> {
            WorldIconState s = WORLD_STATES.get(worldId);
            if (s != null) s.loading = false;
            return null;
        });
    }

    @Nullable
    private static IconAsset loadAssetSync(String worldId, int orderIndex, Path path) {
        if (path == null || !Files.exists(path)) return null;
        try {
            long stamp = Files.getLastModifiedTime(path).toMillis();
            String cacheKey = path.toAbsolutePath().normalize() + "|" + stamp;
            CachedAsset cached = ASSET_CACHE.get(cacheKey);
            if (cached != null) return cached.asset();

            String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
            PreparedImage prepared;
            if (lower.endsWith(".gif")) {
                prepared = prepareGif(path, orderIndex, stamp, cacheKey);
            } else {
                prepared = prepareStatic(path, orderIndex, stamp, cacheKey);
            }
            if (prepared == null) return null;
            return registerPreparedImage(worldId, prepared);
        } catch (Exception e) {
            LOGGER.warn("Failed sync load '{}'", path, e);
            return null;
        }
    }

    @Nonnull
    private static DisplayedIcon buildDisplayedIcon(WorldIconState state) {
        List<IconAsset> assets = state.assets;
        if (assets == null || assets.isEmpty()) return DisplayedIcon.EMPTY;
        long now = System.currentTimeMillis();
        if (assets.size() == 1 || !Config.CUSTOM_ICON_SLIDESHOW.get()) {
            IconAsset a = assets.getFirst();
            return new DisplayedIcon(List.of(new IconLayer(a.textureAt(now), a.width(), a.height(), 1.0f)), a.width(), a.height());
        }
        long start = state.slideshowStartTime > 0 ? state.slideshowStartTime : now;
        long dispMs = Math.max(1000L, Config.CUSTOM_ICON_SLIDESHOW_INTERVAL_SECONDS.get() * 1000L);
        long fadeMs = Math.max(100L, Math.round(Config.CUSTOM_ICON_SLIDESHOW_FADE_SECONDS.get().doubleValue() * 1000.0));
        long segMs = dispMs + fadeMs;
        long total = segMs * assets.size();
        long elapsed = Math.max(0L, now - start);
        long looped = total <= 0 ? 0 : elapsed % total;
        int ci = (int) (looped / segMs);
        long tis = looped % segMs;
        int ni = (ci + 1) % assets.size();
        IconAsset cur = assets.get(ci), nxt = assets.get(ni);
        if (tis < dispMs) return new DisplayedIcon(List.of(new IconLayer(cur.textureAt(now), cur.width(), cur.height(), 1.0f)), cur.width(), cur.height());
        float p = Math.max(0, Math.min(1, (float) (tis - dispMs) / fadeMs));
        float sp = p * p * (3 - 2 * p);
        return new DisplayedIcon(List.of(
                new IconLayer(cur.textureAt(now), cur.width(), cur.height(), 1.0f - sp),
                new IconLayer(nxt.textureAt(now), nxt.width(), nxt.height(), sp)
        ), cur.width(), cur.height());
    }

    @Nonnull
    private static List<Path> listAvailableIconFiles(String worldId) {
        List<Path> files = new ArrayList<>();
        Path dir = getCustomIconDirectory(worldId);
        if (dir == null || !Files.isDirectory(dir)) return files;
        try (var stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) continue;
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.endsWith(".png") || n.endsWith(".gif")) files.add(p);
            }
        } catch (Exception e) { LOGGER.warn("Failed to list icons for '{}'", worldId, e); }
        files.sort((a, b) -> naturalCompare(a.getFileName().toString().toLowerCase(Locale.ROOT), b.getFileName().toString().toLowerCase(Locale.ROOT)));
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
                try { long va = Long.parseLong(a.substring(sa, ia)), vb = Long.parseLong(b.substring(sb, ib)); if (va != vb) return Long.compare(va, vb); }
                catch (NumberFormatException ignored) { int c = a.substring(sa, ia).compareTo(b.substring(sb, ib)); if (c != 0) return c; }
            } else { if (ca != cb) return Character.compare(ca, cb); ia++; ib++; }
        }
        return Integer.compare(a.length(), b.length());
    }

    @Nullable
    private static String buildCacheKey(Path path) {
        try { return path.toAbsolutePath().normalize() + "|" + Files.getLastModifiedTime(path).toMillis(); } catch (Exception e) { return null; }
    }

    @Nullable
    private static PreparedImage prepareImage(Path path, int idx) {
        try {
            long stamp = Files.getLastModifiedTime(path).toMillis();
            String ck = path.toAbsolutePath().normalize() + "|" + stamp;
            if (ASSET_CACHE.containsKey(ck)) return null;
            String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return lower.endsWith(".gif") ? prepareGif(path, idx, stamp, ck) : prepareStatic(path, idx, stamp, ck);
        } catch (Exception e) { LOGGER.warn("Failed to prepare '{}'", path, e); return null; }
    }

    @Nullable
    private static PreparedImage prepareStatic(Path path, int idx, long stamp, String ck) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bi == null) return null;
            return new PreparedImage(ck, idx, stamp, bi.getWidth(), bi.getHeight(), List.of(new PreparedFrame(bytes, 0)), false);
        } catch (Exception e) { LOGGER.warn("Failed to prepare static '{}'", path, e); return null; }
    }

    @Nullable
    private static PreparedImage prepareGif(Path path, int idx, long stamp, String ck) {
        try (InputStream is = Files.newInputStream(path); ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, false, false);
                int[] ls = readLogicalScreenSize(reader);
                int cw = ls[0], ch = ls[1];
                if (cw <= 0 || ch <= 0) { cw = reader.getWidth(0); ch = reader.getHeight(0); }
                int fc = reader.getNumImages(true);
                if (fc <= 0) return null;
                BufferedImage master = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
                List<PreparedFrame> frames = new ArrayList<>();
                for (int i = 0; i < fc; i++) {
                    BufferedImage frame = reader.read(i);
                    GifFrameMetadata meta = readGifFrameMetadata(reader.getImageMetadata(i));
                    BufferedImage backup = copyImage(master);
                    Graphics2D g = master.createGraphics();
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(frame, meta.left(), meta.top(), null);
                    g.dispose();
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        ImageIO.write(copyImage(master), "png", out);
                        frames.add(new PreparedFrame(out.toByteArray(), Math.max(20, meta.delayMs())));
                    }
                    if ("restoreToPrevious".equals(meta.disposalMethod())) master = backup;
                    else if ("restoreToBackgroundColor".equals(meta.disposalMethod())) {
                        Graphics2D cl = master.createGraphics();
                        cl.setComposite(AlphaComposite.Clear);
                        cl.fillRect(meta.left(), meta.top(), frame.getWidth(), frame.getHeight());
                        cl.dispose();
                    }
                }
                if (frames.isEmpty()) return null;
                return new PreparedImage(ck, idx, stamp, cw, ch, frames, true);
            } finally { reader.dispose(); }
        } catch (Exception e) { LOGGER.warn("Failed to prepare gif '{}'", path, e); return null; }
    }

    @Nullable
    private static IconAsset registerPreparedImage(String worldId, PreparedImage p) {
        try {
            if (ASSET_CACHE.containsKey(p.cacheKey)) return ASSET_CACHE.get(p.cacheKey).asset();
            if (!p.isGif) {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(p.frames.getFirst().data));
                ResourceLocation tex = registerTexture(worldId, p.orderIndex, 0, p.stamp, img);
                IconAsset a = new StaticIconAsset(tex, p.width, p.height);
                ASSET_CACHE.put(p.cacheKey, new CachedAsset(worldId, a));
                return a;
            } else {
                List<GifFrame> gf = new ArrayList<>();
                int td = 0;
                for (int i = 0; i < p.frames.size(); i++) {
                    PreparedFrame pf = p.frames.get(i);
                    NativeImage img = NativeImage.read(new ByteArrayInputStream(pf.data));
                    ResourceLocation tex = registerTexture(worldId, p.orderIndex, i, p.stamp, img);
                    gf.add(new GifFrame(tex, pf.delayMs));
                    td += pf.delayMs;
                }
                if (gf.isEmpty()) return null;
                IconAsset a = new GifIconAsset(gf, p.width, p.height, td);
                ASSET_CACHE.put(p.cacheKey, new CachedAsset(worldId, a));
                return a;
            }
        } catch (Exception e) { LOGGER.warn("Failed to register image for '{}'", worldId, e); return null; }
    }

    private static int[] readLogicalScreenSize(ImageReader r) {
        try {
            IIOMetadata m = r.getStreamMetadata();
            if (m == null || m.getNativeMetadataFormatName() == null) return new int[]{r.getWidth(0), r.getHeight(0)};
            Node root = m.getAsTree(m.getNativeMetadataFormatName());
            Node d = findNode(root, "LogicalScreenDescriptor");
            if (d == null) return new int[]{r.getWidth(0), r.getHeight(0)};
            return new int[]{getIntAttr(d, "logicalScreenWidth", r.getWidth(0)), getIntAttr(d, "logicalScreenHeight", r.getHeight(0))};
        } catch (Exception e) {
            try { return new int[]{r.getWidth(0), r.getHeight(0)}; } catch (Exception ignored) { return new int[]{64, 64}; }
        }
    }

    @Nonnull
    private static GifFrameMetadata readGifFrameMetadata(@Nullable IIOMetadata m) {
        if (m == null || m.getNativeMetadataFormatName() == null) return new GifFrameMetadata(0, 0, 100, "none");
        try {
            Node root = m.getAsTree(m.getNativeMetadataFormatName());
            Node id = findNode(root, "ImageDescriptor");
            Node gc = findNode(root, "GraphicControlExtension");
            int l = id != null ? getIntAttr(id, "imageLeftPosition", 0) : 0;
            int t = id != null ? getIntAttr(id, "imageTopPosition", 0) : 0;
            int d = gc != null ? getIntAttr(gc, "delayTime", 10) * 10 : 100;
            String dm = gc != null ? getStrAttr(gc, "disposalMethod", "none") : "none";
            return new GifFrameMetadata(l, t, d <= 0 ? 100 : d, dm);
        } catch (Exception e) { return new GifFrameMetadata(0, 0, 100, "none"); }
    }

    @Nullable
    private static Node findNode(@Nullable Node root, String name) {
        if (root == null) return null;
        if (name.equals(root.getNodeName())) return root;
        Node c = root.getFirstChild();
        while (c != null) { Node f = findNode(c, name); if (f != null) return f; c = c.getNextSibling(); }
        return null;
    }

    private static int getIntAttr(Node n, String name, int fb) {
        NamedNodeMap a = n.getAttributes();
        if (a == null) return fb;
        Node at = a.getNamedItem(name);
        if (at == null) return fb;
        try { return Integer.parseInt(at.getNodeValue()); } catch (Exception e) { return fb; }
    }

    @Nonnull
    private static String getStrAttr(Node n, String name, String fb) {
        NamedNodeMap a = n.getAttributes();
        if (a == null) return fb;
        Node at = a.getNamedItem(name);
        if (at == null) return fb;
        String v = at.getNodeValue();
        return v == null ? fb : v;
    }

    @Nonnull
    private static BufferedImage copyImage(BufferedImage s) {
        BufferedImage c = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(s, 0, 0, null);
        g.dispose();
        return c;
    }

    @Nonnull
    private static ResourceLocation registerTexture(String worldId, int idx, int frame, long stamp, NativeImage image) {
        String sw = worldId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./\\-]", "_").replaceAll("_+", "_");
        long h = (stamp ^ ((long) idx << 16) ^ frame) & 0xFFFFFFFFL;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("reimagined_world_select", "custom_icons/" + sw + "_" + idx + "_" + frame + "_" + h);
        Minecraft.getInstance().getTextureManager().register(loc, TextureCompat.createFromImage(image));
        return loc;
    }

    private static void releaseAsset(IconAsset a) {
        if (Minecraft.getInstance() == null) return;
        for (ResourceLocation t : a.textures()) Minecraft.getInstance().getTextureManager().release(t);
    }

    public record IconLayer(ResourceLocation texture, int width, int height, float alpha) {}
    public record DisplayedIcon(List<IconLayer> layers, int referenceWidth, int referenceHeight) {
        public static final DisplayedIcon EMPTY = new DisplayedIcon(List.of(), 64, 64);
        public boolean isPresent() { return !layers.isEmpty(); }
    }

    private interface IconAsset {
        int width(); int height(); ResourceLocation textureAt(long t); List<ResourceLocation> textures();
    }

    private record StaticIconAsset(ResourceLocation texture, int width, int height) implements IconAsset {
        @Override public ResourceLocation textureAt(long t) { return texture; }
        @Override public List<ResourceLocation> textures() { return List.of(texture); }
    }

    private record GifFrame(ResourceLocation texture, int delayMs) {}

    private record GifIconAsset(List<GifFrame> frames, int width, int height, int totalDuration) implements IconAsset {
        @Override public ResourceLocation textureAt(long t) {
            if (frames.isEmpty()) throw new IllegalStateException();
            if (totalDuration <= 0) return frames.getFirst().texture();
            long time = Math.floorMod(t, totalDuration);
            for (GifFrame f : frames) { if (time < f.delayMs()) return f.texture(); time -= f.delayMs(); }
            return frames.getLast().texture();
        }
        @Override public List<ResourceLocation> textures() {
            List<ResourceLocation> l = new ArrayList<>(frames.size());
            for (GifFrame f : frames) l.add(f.texture());
            return l;
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