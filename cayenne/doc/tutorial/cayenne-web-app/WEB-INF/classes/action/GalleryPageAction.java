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

public final class GalleryPageAction extends Action {

        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {

        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
	
        SelectQuery query = new SelectQuery("Gallery");
        
        // using log level of SEVERE to show the query execution
        List galleries = ctxt.performQuery(query, Level.SEVERE);        
        request.setAttribute("galleries", galleries);
           
	return (mapping.findForward("success"));

    }
    
}
