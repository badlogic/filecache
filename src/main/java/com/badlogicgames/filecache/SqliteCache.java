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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Sqlite-based {@link FileCache} implementation
 * 
 * @author badlogic
 *
 */
public class SqliteCache implements FileCache {
    // @formatter:off
    private static final String SQL_CREATE_TABLE =
            "create table if not exists files ("
          + "  name varchar(255) not null,"
          + "  data Blob,"
          + "  lastModified Long"
          + ")";
    
    private static final String SQL_WRITE = 
            "insert into files"
          + "  (name, data, lastModified)"
          + "  values (?, ?, ?)";
                    
    private static final String SQL_READ = 
            "select data, lastModified from files"
          + "  where name = ?";
    
    private static final String SQL_EXISTS =
            "select name from files"
          + "  where name = ?";
    
    private static final String SQL_LAST_MODIFIED = 
            "select data, lastModified from files"
          + "  where name = ?";
    
    private static final String SQL_REMOVE =
            "delete from files where name = ?";
    // @formatter:on    

    // we keep a map of connections around, based
    // on their jdbc string
    private static Map<String, SingletonConnectionPool> connectionPools = new HashMap<>();
    private final SingletonConnectionPool connectionPool;

    public SqliteCache(File dbFile) throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
            String jdbc = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            synchronized (connectionPools) {
                SingletonConnectionPool connection = connectionPools.get(jdbc);
                if (connection == null) {
                    connection = new SingletonConnectionPool(jdbc);
                }
                connectionPools.put(jdbc, connection);
                this.connectionPool = connection;
            }
            createSchemaIfNeeded();
        } catch (Throwable t) {
            throw new IOException("Couldn't create Sqlite cache", t);
        }
    }

    public void createSchemaIfNeeded() throws SQLException {
        Connection conn = connectionPool.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(SQL_CREATE_TABLE);
        }
    }

    @Override
    public void writeFile(String name, byte[] data) throws IOException {
        try {
            Connection conn = connectionPool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_WRITE)) {
                stmt.setString(1, name);
                stmt.setBytes(2, data);
                stmt.setLong(3, new Date().getTime());
                stmt.executeUpdate();
            }
        } catch (Throwable t) {
            throw new IOException("Couldn't write file " + name, t);
        }
    }

    @Override
    public CachedFile readFile(String name) throws IOException {
        try {
            Connection conn = connectionPool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_READ)) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new IOException("File " + name + " not in cache");
                }
                byte[] data = rs.getBytes(1);
                long lastModified = rs.getLong(2);
                return new CachedFile(name, data, lastModified);
            }
        } catch (Throwable t) {
            throw new IOException("Couldn't read file " + name, t);
        }
    }

    @Override
    public void removeFile(String name) {
        try {
            Connection conn = connectionPool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_REMOVE)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
        } catch (Throwable t) {
            throw new RuntimeException("Couldn't delete file " + name, t);
        }
    }

    @Override
    public boolean isCached(String name) {
        try {
            Connection conn = connectionPool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_EXISTS)) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public long lastModified(String name) {
        try {
            Connection conn = connectionPool.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_LAST_MODIFIED)) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new IOException("File " + name + " not in cache");
                }
                return rs.getLong(1);
            }
        } catch (Throwable t) {
            return 0;
        }
    }

    private static class SingletonConnectionPool {
        private final String jdbc;
        private Connection connection;

        public SingletonConnectionPool(String jdbc) {
            this.jdbc = jdbc;
        }

        public Connection getConnection() throws SQLException {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(jdbc);
            }
            return connection;
        }
    }
}
