package action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.Ordering;
import java.util.logging.Level;
import java.util.List;


public final class ArtistPageAction extends Action {

        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {

        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
            
        SelectQuery query = new SelectQuery("Artist");
        Ordering ordering = new Ordering("artistName", Ordering.ASC); 
        query.addOrdering(ordering);
        
        // using log level of SEVERE to show the query execution
        List artists = ctxt.performQuery(query, Level.SEVERE);
        
        System.out.println("artists: " + artists);
        
        request.setAttribute("artists", artists);
        
	return (mapping.findForward("success"));

    }
    
}