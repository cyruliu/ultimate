package de.uni_freiburg.informatik.ultimate.web.backend;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.PathResource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
 

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}
	
	/**
	 * Create jetty front and backend server.
	 * @param config.PORT Note that if you set this to port 0 then a randomly available port
	 * @return
	 */
	public static Server createServer()
    {
        Server server = new Server(Config.PORT);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);
        
        // Serve the website (frontend) as static content.
        addStaticPathToContext(contexts,
        		Paths.get(System.getProperty("user.home"), Config.FRONTEND_PATH), 
        		Config.FRONTEND_ROUTE);
        
        // Serve the API.
        // Prepare Handler for API servlets.
        ServletContextHandler servlets = new ServletContextHandler(contexts, "/",
            ServletContextHandler.SESSIONS);
        // Enable CORS to allow ultimate backend/frontand running on a seperate port and domain.
        enableCorsOnServletContextHandler(servlets);
        // Add API servlets.
        servlets.addServlet(new ServletHolder(new UltimateAPIServlet()), "/api");

        return server;
    }
	
	/**
	 * Serve files staticly at the routePath
	 * @param contextCollection
	 * @param folderPath Path to the static files to be served.
	 * @param routePath The route the files should be served at (e.g. "/media").
	 */
	private static void addStaticPathToContext(
			HandlerCollection contextCollection, Path folderPath, String routePath) {
        ResourceHandler frontendResourceHandler = new ResourceHandler();
        frontendResourceHandler.setDirectoriesListed(true);
        
        ContextHandler frontendContextHandler = new ContextHandler();
        frontendContextHandler.setContextPath(routePath);
        frontendContextHandler.setBaseResource(new PathResource(folderPath));
        frontendContextHandler.setHandler(frontendResourceHandler);
        
        contextCollection.addHandler(frontendContextHandler);
	}

	/**
	 * Add CORS headers to the servlets in the servlet handler.
	 * So the servlets can be called from outside their served domain. 
	 * @param servlets ServletContextHandler
	 */
	private static void enableCorsOnServletContextHandler(ServletContextHandler servlets) {
		FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
        filterHolder.setInitParameter("allowedOrigins", "*");
        filterHolder.setInitParameter("allowedMethods", "GET, POST");
        servlets.addFilter(filterHolder, "/*", null);
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
        
		// Load config
		Config.load();
		
        System.out.println("Start server.");
        Server server = createServer();
        
        server.start();
        server.join();
 
        System.out.println("Registration Complete");
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
