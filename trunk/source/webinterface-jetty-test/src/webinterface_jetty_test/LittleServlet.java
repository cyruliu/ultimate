package webinterface_jetty_test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;

@SuppressWarnings("serial")
public class LittleServlet extends HttpServlet {
	
	final String greeting;

    public LittleServlet()
    {
        this("Hello");
    }

    public LittleServlet(String greeting)
    {
        this.greeting = greeting;
        System.out.println("Set greeting");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException,
        IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(
            "<h1>" + greeting + " from LittleServlet</h1>");
    }
//	
//	@Override
//    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        resp.getWriter().println("Hello JCG, Hello OSGi");
//    }
//     
//    public LittleServlet() {
//        super();
//    }
}
