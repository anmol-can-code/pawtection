package org.straydog;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** All JDBC code lives here. Always PreparedStatement, always try-with-resources. */
public class Dao {

    public record Dog(String dogId, String name, String sex, Integer ageMonths,
                      String colorMarkings, String area, boolean sterilized,
                      String sterilizedOn, String caretakerPhone) {}

    public record Vax(Integer id, String dogId, String vaccineType, String givenOn,
                      String nextDue, String batchNo, String givenBy) {}

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final SecureRandom RND = new SecureRandom();

    static Date date(String s) { return (s == null || s.isBlank()) ? null : Date.valueOf(s); }

    static String newId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    public static String createDog(Dog d) throws SQLException {
        String sql = "INSERT INTO dogs (dog_id,name,sex,approx_age_months,color_markings,area,sterilized,sterilized_on,caretaker_phone) VALUES (?,?,?,?,?,?,?,?,?)";
        String id = newId();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, d.name());
            ps.setString(3, d.sex());
            if (d.ageMonths() == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, d.ageMonths());
            ps.setString(5, d.colorMarkings());
            ps.setString(6, d.area());
            ps.setBoolean(7, d.sterilized());
            ps.setDate(8, date(d.sterilizedOn()));
            ps.setString(9, d.caretakerPhone());
            ps.executeUpdate();
        }
        return id;
    }

    public static Dog findDog(String id) throws SQLException {
        String sql = "SELECT * FROM dogs WHERE dog_id = ?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int age = rs.getInt("approx_age_months");
                return new Dog(rs.getString("dog_id"), rs.getString("name"), rs.getString("sex"),
                        rs.wasNull() ? null : age, rs.getString("color_markings"), rs.getString("area"),
                        rs.getBoolean("sterilized"), rs.getString("sterilized_on"), rs.getString("caretaker_phone"));
            }
        }
    }

    public static void addVax(Vax v) throws SQLException {
        String sql = "INSERT INTO vaccinations (dog_id,vaccine_type,given_on,next_due,batch_no,given_by) VALUES (?,?,?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.dogId());
            ps.setString(2, v.vaccineType().toUpperCase());
            ps.setDate(3, date(v.givenOn()));
            ps.setDate(4, date(v.nextDue()));
            ps.setString(5, v.batchNo());
            ps.setString(6, v.givenBy());
            ps.executeUpdate();
        }
    }

    /** Newest first. */
    public static List<Vax> vaccinations(String dogId) throws SQLException {
        String sql = "SELECT * FROM vaccinations WHERE dog_id = ? ORDER BY given_on DESC";
        List<Vax> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dogId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Vax(rs.getInt("id"), rs.getString("dog_id"),
                        rs.getString("vaccine_type"), rs.getString("given_on"), rs.getString("next_due"),
                        rs.getString("batch_no"), rs.getString("given_by")));
            }
        }
        return out;
    }

    // ---- users ----
    public static String passwordHash(String username) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public static void createUser(String username, String hash, String role) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users (username,password_hash,role) VALUES (?,?,?)")) {
            ps.setString(1, username); ps.setString(2, hash); ps.setString(3, role);
            ps.executeUpdate();
        }
    }
}
