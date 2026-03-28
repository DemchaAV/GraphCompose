package com.demcha.compose.devtool;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Child-first loader for project classes while keeping dev tool infrastructure
 * parent-first.
 */
public final class SelectiveChildFirstClassLoader extends URLClassLoader {
    private final List<String> childFirstPrefixes;
    private final List<String> parentFirstPrefixes;

    public SelectiveChildFirstClassLoader(URL[] urls,
                                          ClassLoader parent,
                                          List<String> childFirstPrefixes,
                                          List<String> parentFirstPrefixes) {
        super(urls, parent);
        this.childFirstPrefixes = List.copyOf(Objects.requireNonNull(childFirstPrefixes, "childFirstPrefixes"));
        this.parentFirstPrefixes = List.copyOf(Objects.requireNonNull(parentFirstPrefixes, "parentFirstPrefixes"));
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> alreadyLoaded = findLoadedClass(name);
            if (alreadyLoaded != null) {
                if (resolve) {
                    resolveClass(alreadyLoaded);
                }
                return alreadyLoaded;
            }

            Class<?> loadedClass;
            if (shouldUseChildFirst(name)) {
                loadedClass = tryFindClass(name);
                if (loadedClass == null) {
                    loadedClass = super.loadClass(name, false);
                }
            } else {
                loadedClass = super.loadClass(name, false);
            }

            if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }
    }

    @Override
    public URL getResource(String name) {
        URL local = findResource(name);
        return local != null ? local : super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        var resources = new LinkedHashSet<URL>();

        Enumeration<URL> local = findResources(name);
        while (local.hasMoreElements()) {
            resources.add(local.nextElement());
        }

        Enumeration<URL> parent = getParent().getResources(name);
        while (parent.hasMoreElements()) {
            resources.add(parent.nextElement());
        }

        return Collections.enumeration(resources);
    }

    private boolean shouldUseChildFirst(String className) {
        for (String prefix : parentFirstPrefixes) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }

        for (String prefix : childFirstPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private Class<?> tryFindClass(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
