/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.bernarpa.jphoto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author rnd
 */
public class PhotoDb {

    private Connection conn;

    public PhotoDb() throws SQLException {
        String dbFileStr = System.getProperty("user.home") + System.getProperty("file.separator") + "jPhoto.db";
        Path dbFile = Path.of(dbFileStr);
        boolean createTable = !Files.exists(dbFile);
        String dbUrl = "jdbc:sqlite:" + dbFileStr;
        conn = DriverManager.getConnection(dbUrl);
        if (createTable) {
            String sql = "CREATE TABLE fingerprints (file TEXT PRIMARY KEY NOT NULL, fingerprint TEXT NOT NULL)";
            try ( Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
        }
    }

    public void insertFingerprint(Path photo, String fingerprint) throws SQLException {
        String sql = "INSERT INTO fingerprints VALUES (?, ?)";
        try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, photo.toString());
            stmt.setString(2, fingerprint);
            stmt.executeUpdate();
        }
    }

    public String getFingerprint(Path photo) {
        String sql = "SELECT fingerprint FROM fingerprints WHERE file = ?";
        try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, photo.toString());
            try ( ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("fingerprint");
                } else {
                    return null;
                }
            }
        } catch (SQLException ex) {
            System.err.println("DB GET FINGEPRINT ERROR FOR " + photo + ": " + ex.getMessage());
            return null;
        }
    }
    
    public String findDuplicate(Path photo) {
        String fingerprint = getFingerprint(photo);
        System.out.printf("%s -> %s\n", photo, fingerprint);
        String sql = "SELECT file FROM fingerprints WHERE file NOT LIKE ? AND fingerprint LIKE ?";
        try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, photo.toString());
            stmt.setString(2, fingerprint);
            try ( ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file");
                } else {
                    return null;
                }
            }
        } catch (SQLException ex) {
            System.err.println("DB FIND DUPLICATE ERROR FOR " + photo + ": " + ex.getMessage());
            return null;
        }
    }
    
    public Set<String> getFingerprintedFiles() {
        Set<String> fingerprinted = new HashSet<>();
        String sql = "SELECT file FROM fingerprints WHERE fingerprint IS NOT NULL";
        try ( PreparedStatement stmt = conn.prepareStatement(sql)) {
            try ( ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    fingerprinted.add(rs.getString("file"));
                } 
            }
        } catch (SQLException ex) {
            System.err.println("DB GET FINGEPRINTED FILES ERROR: " + ex.getMessage());
            return null;
        }
        return fingerprinted;
    }

    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }
}
