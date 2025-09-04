package com.example.attendance.dao;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.attendance.dto.User;

public class UserDAO {
	private static String sha256(String s){
	    try{
	      MessageDigest md = MessageDigest.getInstance("SHA-256");
	      byte[] h = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	      StringBuilder sb = new StringBuilder();
	      for (byte b : h) sb.append(String.format("%02x", b));
	      return sb.toString();
	    }catch(Exception e){ throw new RuntimeException(e); }
	  }

	  // ---- 認証（ログイン用）----
	  public User findByIdAndPassword(String username, String rawPassword) {
	    String sql = "SELECT username, role, enabled FROM users WHERE username=? AND password_hash=?";
	    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setString(1, username);
	      ps.setString(2, sha256(rawPassword));
	      try (ResultSet rs = ps.executeQuery()) {
	    	  if (rs.next()) {
	    		  return new User(
	    		      rs.getString("username"),
	    		      null,                                 // or ""
	    		      rs.getString("role"),
	    		      rs.getBoolean("enabled"));
	    		}
	      }
	    } catch (SQLException e) { throw new RuntimeException(e); }
	    return null;
	  }

	  // ---- 一覧表示 ----
	  public List<User> findAll(){
	    String sql = "SELECT username, role, enabled FROM users ORDER BY username";
	    List<User> list = new ArrayList<>();
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	    	while (rs.next()) {
	    		  list.add(new User(
	    		      rs.getString("username"),
	    		      null,                                 // or ""
	    		      rs.getString("role"),
	    		      rs.getBoolean("enabled")));
	    		}
	    } catch (SQLException e) { throw new RuntimeException(e); }
	    return list;
	  }

	  // ---- 追加 ----
	  public boolean create(String username, String rawPassword, String role, boolean enabled){
	    String sql = "INSERT INTO users(username, password_hash, role, enabled) VALUES (?,?,?,?)";
	    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setString(1, username);
	      ps.setString(2, sha256(rawPassword));
	      ps.setString(3, role);
	      ps.setBoolean(4, enabled);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { return false; } // 一意制約違反等
	  }

	  // ---- 役割/有効フラグの更新（パスワードは別メソッド）----
	  public boolean update(String username, String role, boolean enabled){
	    String sql = "UPDATE users SET role=?, enabled=? WHERE username=?";
	    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setString(1, role);
	      ps.setBoolean(2, enabled);
	      ps.setString(3, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }

	  // ---- パスワードリセット ----
	  public boolean resetPassword(String username, String newRawPassword){
	    String sql = "UPDATE users SET password_hash=? WHERE username=?";
	    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setString(1, sha256(newRawPassword));
	      ps.setString(2, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }

	  // ---- 有効/無効の切替（そのまま使用可）----
	  public boolean toggleEnabled(String username, boolean enabled){
	    String sql = "UPDATE users SET enabled=? WHERE username=?";
	    try (Connection con = Db.get(); PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setBoolean(1, enabled);
	      ps.setString(2, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }

	  // ---- 削除 ----
	  public boolean delete(String username){
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE username=?")) {
	      ps.setString(1, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }
}
