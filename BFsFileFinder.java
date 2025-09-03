package search_algorithms;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class BFsFileFinder {

    // Find ALL matches, starting from the shallowest
    public static List<Path> findAll(Path root, String glob) {
        List<Path> results = new ArrayList<>();

        try {
            Path start = root.toRealPath(LinkOption.NOFOLLOW_LINKS);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

            Queue<Path> q = new ArrayDeque<>();
            Set<Path> visited = new HashSet<>();
            q.add(start);
            visited.add(start);

            while (!q.isEmpty()) {
                Path cur = q.remove();

                if (Files.isDirectory(cur)) {
                    // Skip some build folders if you want
                    String name = cur.getFileName() == null ? "" : cur.getFileName().toString();
                    if (name.equalsIgnoreCase("bin") || name.equalsIgnoreCase("target")
                            || name.equalsIgnoreCase("out") || name.equalsIgnoreCase("build")) {
                        continue;
                    }

                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(cur)) {
                        for (Path child : ds) {
                            if (Files.isSymbolicLink(child)) continue; // Avoid symlink loops
                            Path real;
                            try {
                                real = child.toRealPath(LinkOption.NOFOLLOW_LINKS);
                            } catch (IOException e) {
                                continue; // inaccessible file/folder
                            }
                            if (!visited.add(real)) continue;

                            if (Files.isDirectory(real)) {
                                q.add(real); // BFS
                            } else {
                                if (matcher.matches(real.getFileName())) {
                                    results.add(real.toAbsolutePath());
                                }
                            }
                        }
                    } catch (IOException ignore) {}
                } else {
                    if (matcher.matches(cur.getFileName())) {
                        results.add(cur.toAbsolutePath());
                    }
                }
            }
        } catch (IOException ignore) {}

        return results;
    }

    public static void main(String[] args) {
        Path start = Paths.get(args.length > 0 ? args[0] : ".");
        String pattern = args.length > 1 ? args[1] : "*.pdf";

        long t0 = System.currentTimeMillis();
        List<Path> hits = findAll(start, pattern);
        long t1 = System.currentTimeMillis();

        System.out.println("Pattern : " + pattern);
        System.out.println("Start    : " + start.toAbsolutePath());
        System.out.println("Finded      : " + hits.size() + " piece");
        for (Path p : hits) {
            System.out.println(" - " + p);
        }
        System.out.println("Time         : " + (t1 - t0) + " ms");
    }
}
