package cayenne.tutorial.tapestry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import net.sf.tapestry.ApplicationServlet;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.conf.BasicServletConfiguration;
import org.objectstyle.cayenne.conf.Configuration;

/**
 * The main servlet class for the application. Links the 
 * servlet container with the artist application.
 * 
 * @author Eric Schneider
 */

public class ArtistAppServlet extends ApplicationServlet {

    protected String getApplicationSpecificationPath() {
        return "/ArtistApp.application";
    }
    /**
     * @see javax.servlet.Servlet#init(ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Configuration.setLoggingLevel(Level.WARN);
        
        // [OPTIONAL STEP] Set up shared Cayenne configuration object
        // to be org.objectstyle.cayenne.conf.BasicServletConfiguration
        // to locate Cayenne config files under WEB-INF instead of CLASSPATH
        BasicServletConfiguration.initializeConfiguration(config.getServletContext());
    }
}