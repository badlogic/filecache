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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.badlogicgames.filecache.FileCache.CachedFile;

/**
 * Extend this test for every cache implementation and override
 * {@link #newCache()}.
 * 
 * @author badlogic
 *
 */
public abstract class AbstractFileBaseTest {
    
    protected abstract FileCache newCache() throws IOException;

    @Test
    public void testWriteExistsReadDelete() throws IOException {
        FileCache cache = newCache();
        writeExistsReadDelete(cache, "some/directory/structure/small", new byte[] { 1, 2, 3 });

        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++)
            largeData[i] = (byte) i;
        writeExistsReadDelete(cache, "some/directory/structure/small", largeData);
    }

    private void writeExistsReadDelete(FileCache cache, String name, byte[] data) throws IOException {
        Date now = new Date();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
        assertFalse(cache.isCached(name));
        cache.writeFile(name, data);
        assertTrue(cache.isCached(name));
        CachedFile file = cache.readFile(name);
        assertEquals(name, file.getName());
        assertArrayEquals(data, file.getData());
        assertTrue(now.getTime() < file.lastModified());
        cache.removeFile(name);
        assertFalse(cache.isCached(name));
        try {
            cache.readFile(name);
            assertTrue("Expected IOException reading an uncached file", false);
        } catch(IOException e) {
            // expected
        }
    }
    
    @Test
    public void testPerformance() throws IOException {
        FileCache cache = newCache();
        
        long start = System.nanoTime();
        byte[] data = new byte[20 * 1024];
        int NUM_FILES = 1000;
        for(int i = 0; i < NUM_FILES; i++) {
            cache.writeFile("file-" + i, data);
            if(i % 100 == 0 && i != 0) {
                System.out.println("Wrote " + i + " files");
            }
        }
        System.out.println("Writing " + NUM_FILES + " files a 20kb took " + (System.nanoTime() - start) / 1000000000.0 + " secs");
        
        start = System.nanoTime();
        for(int i = 0; i < NUM_FILES; i++) {
            cache.readFile("file-" + i);
            if(i % 100 == 0 && i != 0) {
                System.out.println("Read " + i + " files");
            }
        }
        System.out.println("Reading " + NUM_FILES + " files a 20kb took " + (System.nanoTime() - start) / 1000000000.0 + " secs");
    }
}
