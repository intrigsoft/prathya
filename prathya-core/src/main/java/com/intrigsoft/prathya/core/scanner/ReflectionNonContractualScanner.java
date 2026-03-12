package com.intrigsoft.prathya.core.scanner;

import com.intrigsoft.prathya.annotations.NonContractual;
import com.intrigsoft.prathya.core.model.NonContractualEntry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans compiled production class directories via reflection to find {@code @NonContractual} annotations.
 */
public class ReflectionNonContractualScanner implements NonContractualScanner {

    @Override
    public List<NonContractualEntry> scan(List<Path> classDirectories) {
        return scan(classDirectories, List.of());
    }

    @Override
    public List<NonContractualEntry> scan(List<Path> classDirectories, List<Path> additionalClasspath) {
        List<NonContractualEntry> entries = new ArrayList<>();

        URL[] urls = Stream.concat(classDirectories.stream(), additionalClasspath.stream())
                .map(p -> {
                    try {
                        return p.toUri().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid path: " + p, e);
                    }
                })
                .toArray(URL[]::new);

        try (URLClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader())) {
            for (Path dir : classDirectories) {
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

    private void scanClass(ClassLoader cl, String className, List<NonContractualEntry> entries) {
        Class<?> clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return; // skip classes that can't be loaded
        }

        // Check class-level @NonContractual
        NonContractual classAnnotation = clazz.getAnnotation(NonContractual.class);
        if (classAnnotation != null) {
            entries.add(new NonContractualEntry(clazz.getName(), null, classAnnotation.reason()));
            return; // class-level excludes the whole class, no need to check methods
        }

        // Check method-level @NonContractual
        Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            return; // skip classes whose method signatures can't be resolved
        }

        for (Method method : methods) {
            NonContractual methodAnnotation = method.getAnnotation(NonContractual.class);
            if (methodAnnotation != null) {
                entries.add(new NonContractualEntry(clazz.getName(), method.getName(), methodAnnotation.reason()));
            }
        }
    }
}
