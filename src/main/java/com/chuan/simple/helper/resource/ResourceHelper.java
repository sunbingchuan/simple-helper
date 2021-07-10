/*
 * Copyright 2018-2021 Bingchuan Sun.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chuan.simple.helper.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chuan.simple.helper.clazz.ClassHelper;
import com.chuan.simple.helper.common.ObjectHelper;
import com.chuan.simple.helper.common.PatternHelper;
import com.chuan.simple.helper.common.StringHelper;
import com.chuan.simple.helper.exception.SimpleHelperException;

/**
 * Tools for loading resource.
 */
public final class ResourceHelper {

    private static final Log LOG = LogFactory.getLog(ResourceHelper.class);

    public static final String DEFAULT_CLASS_RESOURCE_PATTERN = "**/*.class";

    private static final String JAR_FILE_EXTENSION = ".jar";

    public static final String JAR_URL_PREFIX = "jar:";

    public static final String JAR_URL_SEPARATOR = "!/";

    private ResourceHelper() {
    }

    public static Resource resource() {
        return resource(StringHelper.EMPTY);
    }
    public static Set<Resource> resources() {
        return resources(StringHelper.EMPTY);
    }

    public static Resource resource(String path) {
        Set<Resource> resources = resources(path, null);
        if (ObjectHelper.isNotEmpty(resources)) {
            resources.iterator().next();
        }
        return null;
    }

    public static Set<Resource> resources(String path) {
        return resources(path, null);
    }

    private static Set<Resource> resources(String path,
            ClassLoader classLoader) {
        Set<Resource> resources = new HashSet<>();
        try {
            Enumeration<URL> em;
            if (classLoader == null) {
                classLoader = ClassHelper.getDefaultClassLoader();
            }
            if (classLoader == null) {
                em = ClassLoader.getSystemResources(path);
            } else {
                em = classLoader.getResources(path);
            }
            while (em.hasMoreElements()) {
                URL url = em.nextElement();
                resources.add(new Resource(url));
            }
        } catch (Exception e) {
            LOG.debug("Get resources of path '" + path + "' failed", e);
        }
        return resources;
    }

    public static Set<Resource> expandResources() {
        return expandResources(null);
    }

    public static Set<Resource> expandResources(ClassLoader classLoader) {
        Set<Resource> result = new HashSet<>();
        if (classLoader == null) {
            classLoader = ClassHelper.getDefaultClassLoader();
        }
        expandResources(classLoader, result);
        return result;
    }

    private static void expandResources(ClassLoader classLoader,
            Set<Resource> result) {
        if (classLoader instanceof URLClassLoader) {
            try {
                for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                    Resource jarResource = new Resource(url);
                    if (jarResource.exists()) {
                        result.add(jarResource);
                    }
                }
            } catch (Exception ex) {
                LOG.debug("Cannot introspect jar files since ClassLoader ["
                        + classLoader + "] does not support 'getURLs()': "
                        + ex);
            }
        }

        if (classLoader != null) {
            try {
                // Hierarchy traversal...
                expandResources(classLoader.getParent(), result);
            } catch (Exception e) {
                LOG.debug(
                        "Cannot introspect jar files in parent ClassLoader since ["
                                + classLoader
                                + "] does not support 'getParent()': ",
                        e);
            }
        }
    }
    
    @Deprecated
    private static void doMatchResourcesOld(String fullPattern, File dir,
            Set<File> result) {
        LOG.debug("Searching directory [" + dir.getAbsolutePath()
                + "] for files matching pattern [" + fullPattern + "]");
        for (File content : listDirectory(dir)) {
            String currPath = StringHelper.replace(content.getAbsolutePath(),
                    File.separator, PathHelper.FOLDER_SEPARATOR);
            if (content.isDirectory()) {
                if (PatternHelper.matchStart(fullPattern,
                        currPath + PathHelper.FOLDER_SEPARATOR)) {
                    if (!content.canRead()) {
                        LOG.debug("Skipping subdirectory [" + dir.getAbsolutePath()
                        + "] because the application is not allowed to read the directory");
                    } else {
                        doMatchResourcesOld(fullPattern, content, result);
                    }
                }
            }else if (PatternHelper.matchPath(fullPattern, currPath)) {
                result.add(content);
            }
        }
    }
    
    private static void doMatchResources(String fullPattern, File dir,
            Set<File> result) {
        ResourceFileVisitor resourceFileVisitor = new ResourceFileVisitor(fullPattern, result);
        try {
            Files.walkFileTree(dir.toPath(), resourceFileVisitor);
        } catch (IOException e) {
            throw new SimpleHelperException("Searching resource failed",e);
        }
    }

    public static Set<File> matchResources(String pattern) {
        Set<File> result = new HashSet<>();
        Set<Resource> resources = resources();
        for (Resource resource : resources) {
            File dir = resource.getFile();
            if (dir == null) {//Not file Scheme
                continue;
            }
            String absolutePath = StringHelper.replace(dir.getAbsolutePath(),
                    File.separator, PathHelper.FOLDER_SEPARATOR);
            String fullPattern =
                    absolutePath + PathHelper.FOLDER_SEPARATOR + pattern;
            result.addAll(matchResources(fullPattern, dir));
        }
        return result;
    }

    private static Set<File> matchResources(String fullPattern, File dir) {
        Set<File> result = new HashSet<>();
        doMatchResources(fullPattern, dir, result);
        return result;
    }

    public static Set<InputStream> matchExpandResources(String pattern) {
        Set<InputStream> result = new HashSet<>();
        Set<Resource> resources = expandResources();
        for (Resource resource : resources) {
            if (resource.getName().endsWith(JAR_FILE_EXTENSION)) {
                try {
                    JarURLConnection conn =
                            (JarURLConnection) new URL(JAR_URL_PREFIX
                                    + resource.getURI() + JAR_URL_SEPARATOR)
                                            .openConnection();
                    JarFile jar = conn.getJarFile();
                    Enumeration<JarEntry> e = jar.entries();
                    while (e.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) e.nextElement();
                        if (PatternHelper.matchPath(pattern,
                                jarEntry.getName())) {
                            result.add(jar.getInputStream(jarEntry));
                        }
                    }
                } catch (IOException e) {
                    LOG.debug("Fetch resource of jar '" + resource + "' failed",
                            e);
                }
            } else if (resource.isFile()) {
                if (resource.getFile().isFile()) {
                    if (PatternHelper.matchPath(pattern,
                            resource.getFile().getName())) {
                        addInputStream(resource.getFile(), result);
                    }
                } else {
                    String rootDir = resource.getFile().getAbsolutePath()
                            .replaceAll(PathHelper.WINDOWS_FOLDER_SEPARATOR,
                                    PathHelper.FOLDER_SEPARATOR);
                    String fullPattern =
                            rootDir + PathHelper.FOLDER_SEPARATOR + pattern;
                    Set<File> files =
                            matchResources(fullPattern, resource.getFile());
                    for (File file : files) {
                        addInputStream(file, result);
                    }
                }
            } else {
                // May be extended in the future.
            }
        }
        return result;
    }

    private static void addInputStream(File file, Set<InputStream> result) {
        try {
            result.add(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOG.debug("Skip file '" + file + "' which is not exists", e);
        }
    }

    private static final File[] DIRECTORY_EMPTY = new File[0];

    private static File[] listDirectory(File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("directory should not be null");
        }
        File[] files = dir.listFiles();
        if (files == null) {
            LOG.debug("Couldn't retrieve contents of directory ["
                    + dir.getAbsolutePath() + "]");
            return DIRECTORY_EMPTY;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    public static Properties loadProperties(String name) {
        return loadProperties(name, null);
    }

    private static Properties loadProperties(String name,
            ClassLoader classLoader) {
        Properties props = new Properties();
        if (classLoader == null) {
            classLoader = ClassHelper.getDefaultClassLoader();
        }
        try {
            Enumeration<URL> urls =
                    (classLoader != null ? classLoader.getResources(name)
                            : ClassLoader.getSystemResources(name));
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                URLConnection con = url.openConnection();
                InputStream is = con.getInputStream();
                try {
                    props.load(is);
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            LOG.debug("Load property file '" + name + "' failed", e);
        }
        return props;
    }

    public static class ResourceFileVisitor implements FileVisitor<Path> {

        private final String fullPattern; 
        
        private final  Set<File> result;
        
        private ResourceFileVisitor(String fullPattern,Set<File> result) {
            this.fullPattern = fullPattern;
            this.result=result;
        }
        
        private String changeSeparator(String ori) {
            return StringHelper.replace(ori,
                    File.separator, PathHelper.FOLDER_SEPARATOR);
        }
        
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            LOG.debug("Searching directory [" + dir
            + "] for files matching pattern [" + fullPattern + "]");
            if(PatternHelper.matchStart(fullPattern,
                    changeSeparator(dir.toString()) 
                    + PathHelper.FOLDER_SEPARATOR)) {
                return FileVisitResult.CONTINUE;
            }else {
                return FileVisitResult.SKIP_SUBTREE;
            }
            
        }

        @Override
        public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                throws IOException {
           if (PatternHelper.matchPath(fullPattern, changeSeparator(filePath.toString()))) {
                result.add(filePath.toFile());
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc)
                throws IOException {
            LOG.debug("Loading file [" + file
            + "] failed",exc);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }

    
}
