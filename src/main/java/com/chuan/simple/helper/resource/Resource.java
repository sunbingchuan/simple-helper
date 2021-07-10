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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.chuan.simple.helper.exception.SimpleHelperException;


/**
 * Entity for resource.
 */
public class Resource {

    private static final String SCHEME_FILE = "file";

    private final File file;

    private final String path;

    private final URI uri;

    public Resource(File file) {
        this.file = file;
        this.path = PathHelper.cleanPath(file.getPath());
        this.uri = file.toURI();
    }

    public Resource(String path) {
        this.path = PathHelper.cleanPath(path);
        this.file = new File(this.path);
        this.uri = file.toURI();
    }

    public Resource(URL url) throws URISyntaxException {
        this(url.toURI());
    }

    public Resource(URI uri) {
        this.uri = uri;
        if (SCHEME_FILE.equals(uri.getScheme())) {
            this.file = new File(this.uri);
            this.path = PathHelper.cleanPath(this.file.getPath());
        } else {
            this.file = null;
            this.path = null;
        }
    }

    public Boolean exists() {
        return this.file != null ? this.file.exists() : null;
    }

    /**
     * @return true if the scheme is file
     */
    public boolean isFile() {
        return this.file != null;
    }

    public File getFile() {
        return this.file;
    }

    public Long lastModified() {
        return this.file != null ? this.file.lastModified() : null;
    }

    public String getName() {
        return this.file != null ? this.file.getName() : uri.getPath();
    }

    public InputStream getInputStream() {
        try {
            return this.file != null ? Files.newInputStream(this.file.toPath())
                    : uri.toURL().openConnection().getInputStream();
        } catch (Exception e) {
            throw new SimpleHelperException(
                    "Load config file '" + this + "' error!", e);
        }
    }

    public URI getURI() {
        return uri;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return this.file != null ? "file [" + this.file.getAbsolutePath() + "]"
                : uri.getScheme() + "[" + uri.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            Resource r = (Resource) obj;
            if (this.file != null) {
                return this.file.equals(r.getFile());
            }
            return this.uri.equals(r.uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.file != null) {
            return this.file.hashCode();
        }
        return this.uri.hashCode();
        
    }

}
