/*
 * Copyright (C) 2015 Mario Zechner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogicgames.filecache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link FileCache} implementation that uses the file system directly.
 * All files are stored relative to a base directory.
 * @author badlogic
 *
 */
public class FileSystemCache implements FileCache {
    private final File baseDirectory;
    
    public FileSystemCache(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public void writeFile(String name, byte[] data) throws IOException {
        File file = new File(baseDirectory, name);
        if(!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()) {
                throw new IOException("Couldn't create parent directory of file '" + name + "'");
            }
        }
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            out.write(data);
        }
    }

    @Override
    public CachedFile readFile(String name) throws IOException {
        File file = new File(baseDirectory, name);        
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] bytes = new byte[(int)file.length()];
            in.readFully(bytes);
            return new CachedFile(name, bytes, file.lastModified());
        }
    }

    @Override
    public void removeFile(String name) {
        new File(baseDirectory, name).delete();
    }

    @Override
    public boolean isCached(String name) {
        return new File(baseDirectory, name).exists();
    }

    @Override
    public long lastModified(String name) {
        return new File(baseDirectory, name).lastModified();
    }       
}
