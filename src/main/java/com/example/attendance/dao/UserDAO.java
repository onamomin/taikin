package com.example.attendance.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.attendance.dto.User;

public class UserDAO {
	private static final Map<String, User> users = new HashMap<>();
	static {
		// Sample users with hashed passwords
		users.put("employee1", new User("employee1", hashPassword("password"), "employee", true));
		users.put("admin1", new User("admin1", hashPassword("adminpass"), "admin", true));
		users.put("employee2", new User("employee2", hashPassword("password"), "employee", true));
		users.put("employee3", new User("a", hashPassword("pass"), "employee", true));
	}
	
	public User findByUsername(String username) { 
		return users.get(username);
	}
	
	public boolean verifyPassword(String username, String password) {
		User user = findByUsername(username);
		return user != null && user.isEnabled() && user.getPassword().equals(hashPassword(password));
		}

	public Collection<User> getAllUsers() {
		return users.values();
	}
	
	public void addUser(User user) { 
		users.put(user.getUsername(), user);
	}
	
	public void updateUser(User user) {
		users.put(user.getUsername(), user);
	}
	
	public void deleteUser(String username) {
		users.remove(username);
	}
	
	public void resetPassword(String username, String newPassword) { User user = users.get(username);
	if (user != null) {
		users.put(username, new User(user.getUsername(), hashPassword(newPassword), user.getRole(), user.isEnabled()));
			}
	}
	
	public void toggleUserEnabled(String username, boolean enabled) { User user = users.get(username);
	if (user != null) {
		users.put(username, new User(user.getUsername(), user.getPassword(), user.getRole(),enabled));
			}
	}
	
	public static String hashPassword(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = md.digest(password.getBytes());
			StringBuilder sb = new StringBuilder();
			for (byte b : hashedBytes) {sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); 
		}
	}
	
	private static String sha256(String s){
	    try{
	      MessageDigest md = MessageDigest.getInstance("SHA-256");
	      byte[] h = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	      StringBuilder sb = new StringBuilder();
	      for (byte b : h) sb.append(String.format("%02x", b));
	      return sb.toString();
	    }catch(Exception e){ throw new RuntimeException(e); }
	}
	
	public User findByIdAndPassword(String username, String rawPassword) {
		String sql = "SELECT username, role, enabled FROM users "
				   + "WHERE username=? AND password_hash=?";
		try (Connection con = Db.get();
				   PreparedStatement ps = con.prepareStatement(sql)) {
			   	ps.setString(1, username);
			   	ps.setString(2, sha256(rawPassword));
			   	try (ResultSet rs = ps.executeQuery()) {
			   		if (rs.next()) {
			   			User u = new User();
			   			u.setUsername(rs.getString(1));
			   			u.setRole(rs.getString(2));
			   			u.setEnabled(rs.getBoolean(3));
			   			return u;
			   		}
			   	}
		    } catch (SQLException e) { throw new RuntimeException(e); }
		    return null;
		    }
	public List<User> findAll(){
	    String sql = "SELECT username, role, enabled FROM users ORDER BY username";
	    List<User> list = new ArrayList<>();
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	      while (rs.next()){
	        User u = new User();
	        u.setUsername(rs.getString(1));
	        u.setRole(rs.getString(2));
	        u.setEnabled(rs.getBoolean(3));
	        list.add(u);
	      }
	    } catch (SQLException e) { throw new RuntimeException(e); }
	    return list;
	  }
	public boolean create(String username, String rawPassword, String role, boolean enabled){
	    String sql = "INSERT INTO users(username, password_hash, role, enabled) VALUES (?,?,?,?)";
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement(sql)) {
	      ps.setString(1, username);
	      ps.setString(2, sha256(rawPassword));
	      ps.setString(3, role);
	      ps.setBoolean(4, enabled);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) {
	      // 一意制約違反などは false に
	      return false;
	    }
	  }
	public boolean delete(String username){
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE username=?")) {
	      ps.setString(1, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }

	  public boolean toggleEnabled(String username, boolean enabled){
	    try (Connection con = Db.get();
	         PreparedStatement ps = con.prepareStatement("UPDATE users SET enabled=? WHERE username=?")) {
	      ps.setBoolean(1, enabled);
	      ps.setString(2, username);
	      return ps.executeUpdate() == 1;
	    } catch (SQLException e) { throw new RuntimeException(e); }
	  }
}
