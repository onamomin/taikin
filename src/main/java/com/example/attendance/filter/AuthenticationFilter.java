package com.example.attendance.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebFilter("/*")
public class AuthenticationFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			
			String cp  = httpRequest.getContextPath();                 // 例: /attendance
		    String uri = httpRequest.getRequestURI().substring(cp.length()); // 例: /login.jsp
			
			boolean allow =
			        uri.equals("login.jsp") ||
			        uri.equals("login") ||
			        uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") ||
			        uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png")
			        // 必要に応じて拡張
			        ;
			
			HttpSession session = httpRequest.getSession(false);
			boolean loggedIn = session != null && session.getAttribute("user") != null;			
			
			if (loggedIn) {
	            chain.doFilter(request, response);
			} else {
			httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp");
		}
	}
}