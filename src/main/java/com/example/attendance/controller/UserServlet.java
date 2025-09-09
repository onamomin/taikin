package com.example.attendance.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.example.attendance.dao.UserDAO;
import com.example.attendance.dto.User;

@WebServlet("/users")
public class UserServlet extends HttpServlet {
	private final UserDAO userDAO = new UserDAO();
	private static String trim(String s){ return s == null ? null : s.trim(); }
	private static String genTempPassword() {
	    java.security.SecureRandom r = new java.security.SecureRandom();
	    String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
	    StringBuilder sb = new StringBuilder(12);
	    for (int i = 0; i < 12; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
	    return sb.toString();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String ctx = req.getContextPath();
        HttpSession session = req.getSession(false);
        if (session == null) {
        	resp.sendRedirect(ctx + "/login.jsp");
        	return;
        }
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
        	resp.sendRedirect(ctx + "/login.jsp");
            return;
        }
        // Retrieve and clear message from session
        //String message = (String) session.getAttribute("successMessage");
        Object ok = session.getAttribute("successMessage");
        if (ok != null) { req.setAttribute("successMessage", ok); session.removeAttribute("successMessage"); }
        Object ng = session.getAttribute("errorMessage");
        if (ng != null) { req.setAttribute("errorMessage", ng); session.removeAttribute("errorMessage"); }
        
        String action = req.getParameter("action");
        
        if (action == null || "list".equals(action)) {
        	req.setAttribute("users", userDAO.findAll());
            //session.removeAttribute("successMessage");
        	req.getRequestDispatcher("/jsp/user_management.jsp").forward(req, resp);
            return;
        }
        
        if ("edit".equals(action)) {
            String username = req.getParameter("username");
            if (username == null || username.isBlank()) {
                resp.sendRedirect(ctx + "/users?action=list");
                return;
            }
            User user = userDAO.findOne(username);                 // findByUsername → findOne
            if (user == null) {
                session.setAttribute("errorMessage", "指定のユーザーが見つかりません。");
                resp.sendRedirect(ctx + "/users?action=list");
                return;
            }
            req.setAttribute("userToEdit", user);
            req.setAttribute("users", userDAO.findAll());
            req.getRequestDispatcher("/jsp/user_management.jsp").forward(req, resp);
            return;
        }

        // 想定外のactionは一覧へ
        resp.sendRedirect(ctx + "/users?action=list");
    }
        
        /*if ("list".equals(action) || action == null) {
        	Collection<User> users = userDAO.getAllUsers();
        	req.setAttribute("users", users);
        	RequestDispatcher rd = req.getRequestDispatcher("/jsp/user_management.jsp");
        	 rd.forward(req, resp);
        }else if ("edit".equals(action)) {
        	String username = req.getParameter("username");
        	User user = userDAO.findByUsername(username); req.setAttribute("userToEdit", user);
        	Collection<User> users = userDAO.getAllUsers(); req.setAttribute("users", users);
        	RequestDispatcher rd = req.getRequestDispatcher("/jsp/user_management.jsp");
        	rd.forward(req, resp);
        }else {
        	resp.sendRedirect("users?action=list");
        }
	}*/
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		String ctx = req.getContextPath();
        HttpSession session = req.getSession(false);
        
        User currentUser = (User) session.getAttribute("user");        
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
        	resp.sendRedirect(ctx + "/login.jsp");
            return;
        }
        //多分ここを変えてDBにユーザーを追加する
        String action = req.getParameter("action");
        if ("add".equals(action))  {
        	 String username = req.getParameter("username");
             String password = req.getParameter("password");
             String role = req.getParameter("role");
             //新しく追加
             boolean enabled = "true".equals(req.getParameter("enabled"));
             boolean ok = userDAO.create(username, password, role, enabled); // ★DAOでハッシュ化＆INSERT
             
             //userDAO.findByUsername(username) == null を ok に変更
             if (ok) {
            	 //userDAO.addUser(new User(username,UserDAO.hashPassword(password), role));
            	 session.setAttribute("successMessage","ユーザーを追加しました。");
            	 resp.sendRedirect(ctx + "/users?action=list");
             }else {
            	 req.setAttribute("errorMessage", "ユーザーIDは既に存在します。");
            	 req.setAttribute("users", userDAO.findAll());
            	 req.getRequestDispatcher("/jsp/user_management.jsp").forward(req, resp); // ← 同画面でエラー表示
             } 
             return;
        }
             //アップデート
        else if ("update".equals(action)) {
        	String username = trim(req.getParameter("username"));
            String role     = trim(req.getParameter("role"));
            // チェックボックスは未チェックだと null → false に
            boolean enabled = req.getParameter("enabled") != null;
            
            // 最小バリデーション
            if (username == null || username.isEmpty() || !("admin".equals(role) || "employee".equals(role))) {
            	req.setAttribute("errorMessage", "入力が不正です。ユーザーID/役割を確認してください。");
                req.setAttribute("users", userDAO.findAll());
                req.getRequestDispatcher("/jsp/user_management.jsp").forward(req, resp);
                return;
            }
            //存在確認(ないなら一覧)
            User existingUser = userDAO.findOne(username);   // findByUsername → findOne に置換
            if (existingUser == null) {
            	session.setAttribute("errorMessage", "指定のユーザーが見つかりません。");
                resp.sendRedirect(req.getContextPath() + "/users?action=list");
                return;
            }
            boolean ok = userDAO.update(username, role, enabled);  // updateUser(...) → update(...)
            if (ok) {
            	session.setAttribute("successMessage", "ユーザー情報を更新しました。");
            }else {
            	session.setAttribute("errorMessage", "ユーザー情報の更新に失敗しました。");
            }
            resp.sendRedirect(req.getContextPath() + "/users?action=list");
            //userDAO.updateUser(new User(username, existingUser.getPassword(), role, enabled));
        	return;
        }
        
        //デリート
        else if ("delete".equals(action)) {
        	String username = trim(req.getParameter("username"));
        	//userDAO.deleteUser(username);
        	if (username == null || username.isEmpty()) {
                session.setAttribute("errorMessage", "ユーザーIDが指定されていません。");
                resp.sendRedirect(ctx + "/users?action=list");
                return;
            }
        	if (currentUser != null && username.equals(currentUser.getUsername())) {
                session.setAttribute("errorMessage", "自分自身のアカウントは削除できません。");
                resp.sendRedirect(ctx + "/users?action=list");
                return;
            }
        	User target = userDAO.findOne(username);
        	if (target != null && "admin".equals(target.getRole())) {
        	    // 有効な他の管理者がいなければ削除不可
        	    if (!userDAO.existsAnotherEnabledAdmin(username)) {
        	        session.setAttribute("errorMessage", "最後の管理者は削除できません。");
        	        resp.sendRedirect(ctx + "/users?action=list");
        	        return;
        	    }
        	}
        	boolean ok = userDAO.delete(username);  // deleteUser(...) → delete(...)
        	if (ok) {
        		session.setAttribute("successMessage", "ユーザーを削除しました。");
        	}else {
            	session.setAttribute("errorMessage", "ユーザーの削除に失敗しました。");
            }
        	resp.sendRedirect(ctx + "/users?action=list");
            return;
        }
        //パスワードリセット
        else if ("reset_password".equals(action))  {
        	String username = trim(req.getParameter("username"));
        	String newPassword = req.getParameter("newPassword");
        	
        	if (username == null || username.isEmpty()) {
                session.setAttribute("errorMessage", "ユーザーIDが指定されていません。");
                resp.sendRedirect(ctx + "/users?action=list");
                return;
            }
        	
        	//userが見つからない場合
        	User t = userDAO.findOne(username);
        	if (t == null) {
        	    session.setAttribute("errorMessage", "指定のユーザーが見つかりません。");
        	    resp.sendRedirect(ctx + "/users?action=list");
        	    return;
        	}
        	// 空や短すぎる場合の簡易ガード（任意）
            if (newPassword == null || newPassword.length() < 4) {
                newPassword = genTempPassword(); // デフォルト
            }
            
            boolean ok = userDAO.resetPassword(username, newPassword);
            if (ok) {
            	session.setAttribute("successMessage",
            			username + "のパスワードをリセットしました。(デフォルトパスワード: " + newPassword + ")");
            }else {
            	session.setAttribute("errorMessage", "パスワードのリセットに失敗しました。");
            }
            resp.sendRedirect(ctx + "/users?action=list");
            return;
        }
        
        //トグルなんとか
        else if ("toggle_enabled".equals(action)) {
        	 String username = trim(req.getParameter("username"));
        	 String enabledParam = req.getParameter("enabled");
        	 
        	 if (enabledParam == null) {
     		    session.setAttribute("errorMessage", "有効/無効の指定が取得できません。");
     		    resp.sendRedirect(ctx + "/users?action=list");
     		    return;
     		}
        	 boolean enabled = "true".equalsIgnoreCase(enabledParam) || "1".equals(enabledParam) || "on".equalsIgnoreCase(enabledParam);
        	 //boolean enabled = Boolean.parseBoolean(req.getParameter("enabled"));
        	 
        	 //入力がない時
        	 if (username == null || username.isEmpty()) {
        		 session.setAttribute("errorMessage", "ユーザーIDが指定されていません。");
        		 	resp.sendRedirect(ctx + "/users?action=list");
        	        return;
        	 }
        	 
        	 //入力したユーザーがいない時
        	 User target = userDAO.findOne(username);
        	 if (target == null) {
        		 session.setAttribute("errorMessage", "指定のユーザーが見つかりません。");
        	        resp.sendRedirect(ctx + "/users?action=list");
        	        return;
        	 }
        	 
        	 //管理者が1人しかいない時に無効化しない
        	 if (!enabled && "admin".equals(target.getRole())
        		        && !userDAO.existsAnotherEnabledAdmin(username)) {
        		 session.setAttribute("errorMessage", "最後の管理者は無効化できません。");
        	        resp.sendRedirect(ctx + "/users?action=list");
        	        return;
        	 }
        	 
        	// 事故防止: 自分自身の無効化は禁止
        	 if (!enabled && currentUser.getUsername().equals(username)) {
        		 session.setAttribute("errorMessage", "自分自身のアカウントは無効化できません。");
        	        resp.sendRedirect(ctx + "/users?action=list");
        	        return;
        	 }
        	 
        	 if (target.isEnabled() == enabled) {
        		    session.setAttribute("successMessage", "変更はありません。");
        		    resp.sendRedirect(ctx + "/users?action=list");
        		    return;
        		}
        	 
        	 boolean ok = userDAO.toggleEnabled(username, enabled);
        	 if (ok) {
        		 session.setAttribute("successMessage",
        				 username + "のアカウントを" + (enabled ? "有効" : "無効") + "にしました。");
        	 }else {
        		 session.setAttribute("errorMessage", "アカウント状態の更新に失敗しました。");
        		 
        	 }
        	 resp.sendRedirect(ctx + "/users?action=list");
        	    return;
        }
        resp.sendRedirect("users?action=list");
	}

}
