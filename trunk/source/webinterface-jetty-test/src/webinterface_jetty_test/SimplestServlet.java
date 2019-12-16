package webinterface_jetty_test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SimplestServlet extends HttpServlet {
	@Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException,
        IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(
            "<h1>Hello World</h1>");
    }

}