package webinterface_jetty_test;

import java.util.Hashtable;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
 

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}
	
	public static Server createServer(int port)
    {
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(port);
        
        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        // ServletHandler handler = new ServletHandler();
        // server.setHandler(handler);
        
        // Set the ultimate context handler.
        server.setHandler(new UltimateContextHandler());

        // Passing in the class for the Servlet allows jetty to instantiate an
        // instance of that Servlet and mount it on a given context path.

        // IMPORTANT:
        // This is a raw Servlet, not a Servlet that has been configured
        // through a web.xml @WebServlet annotation, or anything similar.
        // handler.addServletWithMapping(SimplestServlet.class, "/*");

        return server;
    }

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
        
        int port = 8080;
        System.out.println("Start server.");
        Server server = createServer(port);
        
        server.start();
        server.join();
 
        System.out.println("Registration Complete");
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
