package action;

import java.util.List;
import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import test.Gallery;
import test.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

import formbean.*;

public final class RemovePaintingFromGalleryAction extends Action {
    
        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
        
        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
        
        String paintingTitle = request.getParameter("title");
        String galleryName = request.getParameter("galleryName");
        
        Expression qual = ExpressionFactory.binaryPathExp(
                                        Expression.EQUAL_TO,
                                        "paintingTitle",
                                        paintingTitle);
        
        SelectQuery query = new SelectQuery("Painting", qual);
        
        // using log level of WARN to show the query execution
        query.setLogLevel(Level.WARN);
        List paintings = ctxt.performQuery(query);
                
        Painting painting = (Painting)paintings.get(0);
        System.err.println("painting: " + painting);
        
        qual = ExpressionFactory.binaryPathExp(
                                        Expression.EQUAL_TO,
                                        "galleryName",
                                        galleryName);
        
        query = new SelectQuery("Gallery", qual);
        query.setLogLevel(Level.WARN);
        List galleries = ctxt.performQuery(query);
        Gallery gallery = (Gallery)galleries.get(0);
        
        gallery.removeFromPaintingArray(painting);
        
        // commit to the database
        // using log level of WARN to show the query execution
        ctxt.commitChanges(Level.WARN); 
        
	return (mapping.findForward("success"));

    }
    
}
