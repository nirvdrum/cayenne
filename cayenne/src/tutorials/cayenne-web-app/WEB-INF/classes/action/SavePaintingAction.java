package action;

import java.util.*;

import javax.servlet.http.*;

import org.apache.log4j.*;
import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.exp.*;
import org.objectstyle.cayenne.query.*;
import webtest.*;

import formbean.*;

public final class SavePaintingAction extends Action {

        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
            
        PaintingForm paintingForm = (PaintingForm) form;

        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
        
        String anArtistName = paintingForm.getArtistName();
        
        Expression qual = ExpressionFactory.binaryPathExp(
                                        Expression.EQUAL_TO,
                                        "artistName",
                                        anArtistName);
        
        SelectQuery query = new SelectQuery("Artist", qual);
        
        // using log level of WARN to show the query execution
        query.setLoggingLevel(Level.WARN);
        
      
        List artists = ctxt.performQuery(query);
        System.err.println("artists: " + artists);
        Artist artist = (Artist)artists.get(0);
        
	
        Painting aPainting = (Painting)ctxt.createAndRegisterNewObject("Painting");
        aPainting.setPaintingTitle(paintingForm.getPaintingTitle());
        aPainting.setEstimatedPrice(paintingForm.getEstimatedPrice());
        
        artist.addToPaintingArray(aPainting);
       
        // commit to the database
        // using log level of WARN to show the query execution
        ctxt.commitChanges(Level.WARN); 

        
	return (mapping.findForward("success"));

    }
    
}
