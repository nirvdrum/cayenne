package action;

import java.util.*;

import javax.servlet.http.*;

import org.apache.log4j.*;
import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.query.*;
import webtest.*;

public final class RemovePaintingFromGalleryAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        DataContext ctxt = (DataContext) request.getSession().getAttribute("context");

        String paintingTitle = request.getParameter("title");
        String galleryName = request.getParameter("galleryName");

        Expression qual =
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "paintingTitle",
                paintingTitle);

        SelectQuery query = new SelectQuery("Painting", qual);

        // set a relatively high logging level, 
        // to show the query execution progress
        query.setLoggingLevel(Level.WARN);
        List paintings = ctxt.performQuery(query);

        Painting painting = (Painting) paintings.get(0);
        System.err.println("painting: " + painting);

        qual =
            ExpressionFactory.binaryPathExp(
                Expression.EQUAL_TO,
                "galleryName",
                galleryName);

        query = new SelectQuery("Gallery", qual);
        query.setLoggingLevel(Level.WARN);
        List galleries = ctxt.performQuery(query);
        Gallery gallery = (Gallery) galleries.get(0);

        gallery.removeFromPaintingArray(painting);

        // commit to the database
		// use a relatively high logging level, 
		// to show the query execution progress
        ctxt.commitChanges(Level.WARN);

        return mapping.findForward("success");
    }
}
