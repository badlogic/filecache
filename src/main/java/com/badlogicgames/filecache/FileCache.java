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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple file cache with key/value semantics. The key is
 * the file name, an arbitrary character sequence, the value
 * is the binary data of the file. Implementations must be
 * thread safe and multi-process safe.
 * 
 * FIXME specify concurrency semantics, for now they are
 * implementation specific
 * @author badlogic
 *
 */
public interface FileCache {
    /**
     * Writes a file with the given name to the cache. The
     * file name has to be a valid file system name, e.g.
     * <code>"this/directory/has/a/file"</code>.
     */
    public void writeFile(String name, byte[] data) throws IOException;
        
    /**
     * Reads a file from the cache. Throws an {@link IOException}
     * in case the file is not in the cache or couldn't be read.
     */
    public CachedFile readFile(String name) throws IOException;
    
    /**
     * Deletes the file from the cache. Does nothing if
     * the file isn't cached. 
     */
    public void removeFile(String name);
    
    /**
     * @return whether the file is cached
     */
    public boolean isCached(String name);
    
    /**
     * @return the last modification date of the file in UTC or 0 if the file isn't cached.
     */
    public long lastModified(String name);
    
    /**
     * File retrieved from the cache. Read-only. Modifications
     * to the <code>byte[]</code> storing the data will not
     * be written back to the cache. Use {@link FileCache#writeFile(String, byte[])}
     * instead.
     * @author badlogic
     *
     */
    public static class CachedFile {
        private final String name;
        private final byte[] data;
        private final long lastModified;
        
        public CachedFile(String name, byte[] data, long lastModified) {
            this.name = name;
            this.data = data;
            this.lastModified = lastModified;
        }

        public String getName() {
            return name;
        }
        
        public long lastModified() {
            return lastModified;
        }
        
        public byte[] getData() {
            return data;
        }
        
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }
    }
}
