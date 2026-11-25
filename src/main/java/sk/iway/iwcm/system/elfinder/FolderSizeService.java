package sk.iway.iwcm.system.elfinder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsVolume;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.io.IwcmFile;

/** Counts readable file contents for an open Info dialog. Scan handles are local to a login and domain. */
@Service
public class FolderSizeService implements DisposableBean {
    public record Owner(String sessionId, int userId, String domain) {}
    public record Progress(String scanId, String state, String bytes, long folders) {}

    private final Map<String, Scan> scans = new HashMap<>();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16));
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor();
    private static final long IDLE_NANOS = TimeUnit.MINUTES.toNanos(2);

    public FolderSizeService() {
        cleanup.scheduleWithFixedDelay(this::expire, 30, 30, TimeUnit.SECONDS);
    }

    public static boolean supports(FsVolume volume, boolean root) {
        return volume instanceof IwcmFsVolume && !(root && (volume instanceof IwcmLibraryFsVolume || volume instanceof IwcmActualPageFsVolume));
    }

    public Progress start(Owner owner, Identity user, FsService service, FsItem target) throws IOException {
        if (!(target instanceof IwcmFsItem item) || !(item.getVolume() instanceof IwcmFsVolume volume)
                || !supports(volume, volume.isRoot(item))) throw new IOException("Unsupported folder");
        if ("iwcm_2".equals(service.getVolumeId(volume)) && user.isDisabledItem("menuFbrowser")) throw new IOException("Folder unavailable");

        Predicate<IwcmFile> visible = volume.visibleEntries(user);
        checkTarget(service, volume, item, visible);
        Scan scan = new Scan(owner);
        synchronized (this) {
            scan.work = workers.submit(() -> run(scan, user, service, volume, item.getFile(), visible));
            scans.put(scan.id, scan);
        }
        return scan.progress();
    }

    private static void checkTarget(FsService service, IwcmFsVolume volume, IwcmFsItem item, Predicate<IwcmFile> visible) throws IOException {
        IwcmFile folder = item.getFile();
        if (!folder.exists() || !folder.isDirectory()) throw new IOException("Folder unavailable");
        String path = folder.getVirtualPath();
        if (!path.startsWith("/") || path.startsWith("//")) throw new IOException("Invalid folder");
        Path target = Path.of(path.substring(1));
        if (!target.equals(target.normalize())) throw new IOException("Invalid folder");
        if (volume instanceof IwcmLibraryFsVolume || volume instanceof IwcmActualPageFsVolume) {
            boolean mounted = false;
            for (FsItem root : volume.listChildren(volume.getRoot())) {
                if (target.startsWith(Path.of(volume.asFile(root).getVirtualPath().substring(1)))) mounted = true;
            }
            if (!mounted) throw new IOException("Folder outside mounted roots");
        }
        // Planned folders inherit destination permissions, not permissions of their storage ancestors.
        if ((!"/".equals(path) && !visible.test(folder)) || !service.getSecurityChecker().isReadable(service, item)) {
            throw new IOException("Folder unavailable");
        }
        for (Path part = target; part != null; part = part.getParent()) {
            IwcmFile ancestor = part.equals(target) ? folder : IwcmFile.fromVirtualPath("/" + part.toString().replace('\\', '/'));
            if (ancestor.localStoragePath() != null && Files.isSymbolicLink(ancestor.localStoragePath())) {
                throw new IOException("Folder unavailable");
            }
        }
    }

    private void run(Scan scan, Identity user, FsService service, IwcmFsVolume volume, IwcmFile root, Predicate<IwcmFile> visible) {
        RequestBean context = new RequestBean();
        context.setUser(user);
        context.setDomain(scan.owner.domain());
        try (var ignored = SetCharacterEncodingFilter.openIsolatedDataContext(null)) {
            SetCharacterEncodingFilter.setCurrentRequestBean(context);
            RequestBean.setAttribute(sk.iway.iwcm.system.elfinder.FsService.BACKGROUND_USER, user);
            walk(root, entry -> visible.test(entry) && service.getSecurityChecker().isReadable(service, new IwcmFsItem(volume, entry)), scan);
            scan.state = "complete";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            Logger.error(ex);
            scan.state = "failed";
        }
    }

    @FunctionalInterface
    interface Readable { boolean test(IwcmFile entry) throws IOException; }

    static void walk(IwcmFile root, Readable readable, Scan scan) throws IOException, InterruptedException {
        ArrayDeque<IwcmFile> pending = new ArrayDeque<>();
        Set<Object> visited = new HashSet<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            scan.checkActive();
            IwcmFile folder = pending.pop();
            Path path = folder.localStoragePath();
            if (path == null) {
                if (!visited.add(folder.getVirtualPath())) continue;
                IwcmFile[] children = folder.listFiles();
                if (children == null) throw new IOException("Cannot list folder");
                for (IwcmFile child : children) count(child, readable, scan, pending);
            } else {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!attrs.isDirectory()) throw new IOException("Folder changed during scan");
                if (!visited.add(attrs.fileKey() == null ? path.toRealPath() : attrs.fileKey())) continue;
                try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                    for (Path child : children) {
                        IwcmFile entry = new IwcmFile(child.toFile());
                        if (FilePathTools.isExternalDir(entry.getVirtualPath())) {
                            entry = IwcmFile.fromVirtualPath(entry.getVirtualPath());
                        }
                        count(entry, readable, scan, pending);
                    }
                }
            }
            scan.folders++;
        }
    }

    private static void count(IwcmFile child, Readable readable, Scan scan, ArrayDeque<IwcmFile> pending)
            throws IOException, InterruptedException {
        scan.checkActive();
        if (!readable.test(child)) return;
        Path local = child.localStoragePath();
        long length;
        if (local != null) {
            BasicFileAttributes attrs = Files.readAttributes(local, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs.isDirectory()) pending.push(child);
            if (!attrs.isRegularFile()) return;
            length = attrs.size();
        } else {
            if (child.isFile()) length = child.getLength();
            else if (child.exists() && child.isDirectory()) {
                pending.push(child);
                return;
            } else throw new IOException("Missing storage entry");
        }
        if (length < 0) throw new IOException("Unknown file length");
        scan.bytes = Math.addExact(scan.bytes, length);
    }

    public synchronized Progress status(Owner owner, String id) {
        Scan scan = scans.get(id);
        if (scan == null || !scan.owner.equals(owner)) return null;
        scan.lastPoll = System.nanoTime();
        return scan.progress();
    }

    public synchronized void cancel(Owner owner, String id) {
        Scan scan = scans.get(id);
        if (scan != null && scan.owner.equals(owner)) {
            scans.remove(id);
            scan.work.cancel(true);
            workers.purge();
        }
    }

    private synchronized void expire() {
        long now = System.nanoTime();
        scans.values().removeIf(scan -> {
            if (now - scan.lastPoll < IDLE_NANOS) return false;
            scan.work.cancel(true);
            return true;
        });
        workers.purge();
    }

    @Override
    public synchronized void destroy() {
        cleanup.shutdownNow();
        workers.shutdownNow();
        scans.clear();
    }

    static final class Scan {
        final Owner owner;
        final String id = UUID.randomUUID().toString();
        volatile String state = "running";
        volatile long bytes;
        volatile long folders;
        long lastPoll = System.nanoTime();
        Future<?> work;

        Scan(Owner owner) { this.owner = owner; }

        Progress progress() { return new Progress(id, state, Long.toString(bytes), folders); }

        void checkActive() throws InterruptedException {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        }
    }
}
