package action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.SelectQuery;

public final class GalleryPageAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        DataContext ctxt = (DataContext) request.getSession().getAttribute("context");

        SelectQuery query = new SelectQuery("Gallery");

        // set a relatively high logging level, 
        // to show the query execution progress
        query.setLoggingLevel(Level.WARN);

        List galleries = ctxt.performQuery(query);
        request.setAttribute("galleries", galleries);

        return mapping.findForward("success");
    }

}
