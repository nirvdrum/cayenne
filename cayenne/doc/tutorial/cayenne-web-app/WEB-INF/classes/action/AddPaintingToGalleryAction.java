package action;

import java.util.List;
import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import test.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

import formbean.*;

public final class AddPaintingToGalleryAction extends Action {
    
        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
        
        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
        
        String paintingTitle = request.getParameter("title");
        
        Expression qual = ExpressionFactory.binaryPathExp(
                                        Expression.EQUAL_TO,
                                        "paintingTitle",
                                        paintingTitle);
        
        SelectQuery query = new SelectQuery("Painting", qual);
        query.setLogLevel(Level.SEVERE);
        // using log level of SEVERE to show the query execution
        List paintings = ctxt.performQuery(query);
                
        Painting painting = (Painting)paintings.get(0);
        System.err.println("painting: " + painting);
        
        query = new SelectQuery("Gallery");
        query.setLogLevel(Level.SEVERE);
        List galleries = ctxt.performQuery(query);
        
        request.setAttribute("painting", painting);
        request.setAttribute("galleries", galleries);
        
	return (mapping.findForward("success"));

    }
    
}

