package action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory; 
import org.objectstyle.cayenne.query.SelectQuery;
import java.util.logging.Level;
import java.util.List;
import test.Gallery;
import test.Painting;

public final class SubmitPaintingToGalleryAction extends Action {
    
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
        
        // using log level of SEVERE to show the query execution
        List paintings = ctxt.performQuery(query, Level.SEVERE);
                
        Painting painting = (Painting)paintings.get(0);
        System.err.println("painting: " + painting);
        
        qual = ExpressionFactory.binaryPathExp(
                                        Expression.EQUAL_TO,
                                        "galleryName",
                                        galleryName);
        
        query = new SelectQuery("Gallery", qual);
        List galleries = ctxt.performQuery(query, Level.SEVERE);
        Gallery gallery = (Gallery)galleries.get(0);
        
        gallery.addToPaintingArray(painting);
        
        // commit to the database
        // using log level of SEVERE to show the query execution
        ctxt.commitChanges(Level.SEVERE); 
        
	return (mapping.findForward("success"));

    }
    
}
