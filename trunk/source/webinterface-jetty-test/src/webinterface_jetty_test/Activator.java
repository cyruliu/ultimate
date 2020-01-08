package webinterface_jetty_test;

import java.util.Hashtable;

import javax.servlet.FilterRegistration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
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

        // Declare server handler collection
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        // Configure context "/" (root) for servlets
        ServletContextHandler root = new ServletContextHandler(contexts, "/",
            ServletContextHandler.SESSIONS);

        // Enable CORS to allow ultimate backend running on a seperate port and domain.
        FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET, POST");
        root.addFilter(filterHolder, "/*", null);

        // Add servlets to root context
        root.addServlet(new ServletHolder(new UltimateAPIServlet()), "/");
        root.addServlet(new ServletHolder(new UltimateHttpServlet()), "/old_api");


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
