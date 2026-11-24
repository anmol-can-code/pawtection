package org.straydog;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Db {
    private static HikariDataSource ds;

    public static void init() {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(env("DB_URL", "jdbc:mysql://localhost:3306/straydog"));
        c.setUsername(env("DB_USER", "root"));
        c.setPassword(env("DB_PASS", ""));
        c.setMaximumPoolSize(5);
        ds = new HikariDataSource(c);
    }

    public static Connection get() throws SQLException { return ds.getConnection(); }

    static String env(String k, String def) {
        String v = System.getenv(k);
        return v == null ? def : v;
    }
}
