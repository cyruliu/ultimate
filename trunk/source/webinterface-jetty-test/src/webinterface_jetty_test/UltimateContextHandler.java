package webinterface_jetty_test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class UltimateContextHandler extends AbstractHandler {
	final String greeting;
	private UltimateWebController ultimateWebController;
	
    public UltimateContextHandler()
    {
        this("I am here to process Ultimate results..");
    }

    public UltimateContextHandler(String greeting)
    {
        this.greeting = greeting;
        this.ultimateWebController = new UltimateWebController();
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
        ServletException
    {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();

        out.println("<h1>" + greeting + "</h1>");

        baseRequest.setHandled(true);
    }
}
