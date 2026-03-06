package dev.pratya.core.scanner;

import dev.pratya.annotations.Requirement;
import dev.pratya.core.model.TraceEntry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans compiled test class directories via reflection to find {@code @Requirement} annotations.
 */
public class ReflectionAnnotationScanner implements AnnotationScanner {

    @Override
    public List<TraceEntry> scan(List<Path> testClassDirectories) {
        return scan(testClassDirectories, List.of());
    }

    @Override
    public List<TraceEntry> scan(List<Path> testClassDirectories, List<Path> additionalClasspath) {
        List<TraceEntry> entries = new ArrayList<>();

        URL[] urls = Stream.concat(testClassDirectories.stream(), additionalClasspath.stream())
                .map(p -> {
                    try {
                        return p.toUri().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid path: " + p, e);
                    }
                })
                .toArray(URL[]::new);

        try (URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader())) {
            for (Path dir : testClassDirectories) {
                if (!Files.isDirectory(dir)) continue;
                List<String> classNames = findClassFiles(dir);
                for (String className : classNames) {
                    scanClass(cl, className, entries);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to close classloader", e);
        }

        return entries;
    }

    private List<String> findClassFiles(Path dir) {
        List<String> classNames = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".class")) {
                        Path relative = dir.relativize(file);
                        String className = relative.toString()
                                .replace(file.getFileSystem().getSeparator(), ".")
                                .replaceAll("\\.class$", "");
                        classNames.add(className);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + dir, e);
        }
        return classNames;
    }

    private void scanClass(ClassLoader cl, String className, List<TraceEntry> entries) {
        Class<?> clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return; // skip classes that can't be loaded
        }

        Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            return; // skip classes whose method signatures can't be resolved
        }

        for (Method method : methods) {
            List<String> allIds = new ArrayList<>();

            // Check for single @Requirement
            Requirement single = method.getAnnotation(Requirement.class);
            if (single != null) {
                allIds.addAll(Arrays.asList(single.value()));
            }

            // Check for @Requirement.List (repeatable container)
            Requirement.List list = method.getAnnotation(Requirement.List.class);
            if (list != null) {
                for (Requirement r : list.value()) {
                    allIds.addAll(Arrays.asList(r.value()));
                }
            }

            if (!allIds.isEmpty()) {
                entries.add(new TraceEntry(
                        clazz.getName(),
                        method.getName(),
                        allIds.stream().distinct().collect(Collectors.toList())
                ));
            }
        }
    }
}
