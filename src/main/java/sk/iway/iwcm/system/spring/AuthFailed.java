package sk.iway.iwcm.system.spring;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class AuthFailed implements AuthenticationFailureHandler
{
	@Override
	public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse resp, AuthenticationException arg2) throws IOException, ServletException
	{
		// TODO Auto-generated method stub
		resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}
}
