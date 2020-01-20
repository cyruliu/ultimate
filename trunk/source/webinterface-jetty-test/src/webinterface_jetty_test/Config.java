package webinterface_jetty_test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
* Class that loads settings from config.properties
* @author Brant Unger
*
*/
public class Config 
{
	public static boolean DEBUG = false;
	public static int PORT = 8080;
	public static String FRONTEND_PATH = "website_static";
	public static String FRONTEND_ROUTE = "/website";

	/**
	 * Load settings from config.properties
	 */
	public static void load()
	{
		try {
			Properties appSettings = new Properties();
			FileInputStream fileInputStream = new FileInputStream("web.config.properties");
			appSettings.load(fileInputStream);

			DEBUG = Boolean.parseBoolean((String)appSettings.get("DEBUG"));
		    PORT = Integer.parseInt((String)appSettings.get("PORT"));
		    FRONTEND_PATH = (String)appSettings.get("FRONTEND_PATH");
		    FRONTEND_ROUTE = (String)appSettings.get("FRONTEND_ROUTE");

		    fileInputStream.close();
   
		   if(DEBUG) System.out.println("Settings file successfuly loaded");
	
	    }
	    catch(IOException e) {
	        System.out.println("Could not load settings file.");
	        System.out.println(e.getMessage());
	    }
	}
}
