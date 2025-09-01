package search_algorithms;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DfsFileFinder {
	// Order of visit
    public enum Order { DIRS_FIRST, FILES_FIRST }

    // Simple holder: to move the dfsFirst result out
    static final class Holder<T> { T value; }

    // Get the first match
    public static Optional<Path> findFirst(Path root, String glob, Order order) {
        PathMatcher m = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        Set<Path> visited = new HashSet<>();
        Holder<Path> ans = new Holder<>();
        boolean ok = dfsFirst(root, m, visited, ans, order);
        return ok ? Optional.of(ans.value.toAbsolutePath()) : Optional.empty();
    }

    // Get all matches (in DFS order)
    public static List<Path> findAll(Path root, String glob, Order order) {
        PathMatcher m = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        Set<Path> visited = new HashSet<>();
        List<Path> out = new ArrayList<>();
        dfsAll(root, m, visited, out, order);
        return out;
    }


    private static boolean dfsFirst(Path p, PathMatcher m, Set<Path> v, Holder<Path> ans, Order order) {
        try {
            if (Files.isSymbolicLink(p)) return false;

            Path real;
            try { real = p.toRealPath(LinkOption.NOFOLLOW_LINKS); }
            catch (IOException e) { return false; }

            if (!v.add(real)) return false; 

            if (Files.isDirectory(real)) {
                // don't take
                String name = real.getFileName() == null ? "" : real.getFileName().toString();
                if (name.equalsIgnoreCase("bin") || name.equalsIgnoreCase("target")
                        || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("build")) return false;

                List<Path> dirs  = new ArrayList<>();
                List<Path> files = new ArrayList<>();
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(real)) {
                    for (Path c : ds) if (Files.isDirectory(c)) dirs.add(c); else files.add(c);
                } catch (IOException ignored) {}

                // deterministik: alfabetik
                dirs.sort(Comparator.naturalOrder());
                files.sort(Comparator.naturalOrder());

                if (order == Order.DIRS_FIRST) {
                    for (Path f : files)
                        if (m.matches(f.getFileName())) { ans.value = f; return true; }
                    for (Path d : dirs)
                        if (dfsFirst(d, m, v, ans, order)) return true;
                } else { // DIRS_FIRST
                    for (Path d : dirs)
                        if (dfsFirst(d, m, v, ans, order)) return true;
                    for (Path f : files)
                        if (m.matches(f.getFileName())) { ans.value = f; return true; }
                }
            } else {
                if (m.matches(real.getFileName())) { ans.value = real; return true; }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void dfsAll(Path p, PathMatcher m, Set<Path> v, List<Path> out, Order order) {
        try {
            if (Files.isSymbolicLink(p)) return;

            Path real;
            try { real = p.toRealPath(LinkOption.NOFOLLOW_LINKS); }
            catch (IOException e) { return; }

            if (!v.add(real)) return;

            if (Files.isDirectory(real)) {
                String name = real.getFileName() == null ? "" : real.getFileName().toString();
                if (name.equalsIgnoreCase("bin") || name.equalsIgnoreCase("target")
                        || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("build")) return;

                List<Path> dirs  = new ArrayList<>();
                List<Path> files = new ArrayList<>();
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(real)) {
                    for (Path c : ds) if (Files.isDirectory(c)) dirs.add(c); else files.add(c);
                } catch (IOException ignored) {}

                dirs.sort(Comparator.naturalOrder());
                files.sort(Comparator.naturalOrder());

                if (order == Order.DIRS_FIRST) {
                    for (Path f : files) if (m.matches(f.getFileName())) out.add(f.toAbsolutePath());
                    for (Path d : dirs) dfsAll(d, m, v, out, order);
                } else { // DIRS_FIRST
                    for (Path d : dirs) dfsAll(d, m, v, out, order);
                    for (Path f : files) if (m.matches(f.getFileName())) out.add(f.toAbsolutePath());
                }
            } else {
                if (m.matches(real.getFileName())) out.add(real.toAbsolutePath());
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        Path start      = Paths.get(args.length > 0 ? args[0] : ".");
        String pattern  = args.length > 1 ? args[1] : "*.pdf";
        String mode     = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "first"; // "first" | "all"
        Order order     = args.length > 3 && args[3].equalsIgnoreCase("files")
                            ? Order.FILES_FIRST : Order.DIRS_FIRST; // "dirs"(def) | "files"

        System.out.printf("Start=%s \n Pattern=%s  Mode=%s  Order=%s \n",
                start.toAbsolutePath(), pattern, mode, order);
        if (mode.equals("first")) {
            System.out.println(findFirst(start, pattern, order).orElse(null));
        } else {
            findAll(start, pattern, order).forEach(System.out::println);
        }
    }
}