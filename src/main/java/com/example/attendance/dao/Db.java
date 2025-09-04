package com.example.attendance.dao;

import java.sql.*;

public final class Db {
	  private static final String URL  = "jdbc:postgresql://localhost:5432/taikin";
	  private static final String USER = "taikin_app";
	  private static final String PASS = "changeme";

	  static {
	    try { Class.forName("org.postgresql.Driver"); } 
	    catch (ClassNotFoundException e) { throw new RuntimeException(e); }
	  }
	  public static Connection get() throws SQLException {
	    return DriverManager.getConnection(URL, USER, PASS);
	  }
	}