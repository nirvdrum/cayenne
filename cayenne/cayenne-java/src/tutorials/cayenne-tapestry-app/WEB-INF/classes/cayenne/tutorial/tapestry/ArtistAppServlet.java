package cayenne.tutorial.tapestry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.tapestry.ApplicationServlet;
import org.objectstyle.cayenne.conf.ServletUtil;

/**
 * The main servlet class for the application. Links the 
 * servlet container with the artist application.
 * 
 * @author Eric Schneider
 */
public class ArtistAppServlet extends ApplicationServlet {
    /**
     * @see javax.servlet.Servlet#init(ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // [OPTIONAL STEP] Configure shared Cayenne configuration object
        // to be able to locate XML config files under WEB-INF in addition to CLASSPATH
        ServletUtil.initializeSharedConfiguration(config.getServletContext());
    }
}