package action;

import java.util.List;
import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import test.Artist;
import test.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
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
        
        // using log level of SEVERE to show the query execution
        query.setLogLevel(Level.SEVERE);
        
      
        List artists = ctxt.performQuery(query);
        System.err.println("artists: " + artists);
        Artist artist = (Artist)artists.get(0);
        
	
        Painting aPainting = (Painting)ctxt.createAndRegisterNewObject("Painting");
        aPainting.setPaintingTitle(paintingForm.getPaintingTitle());
        aPainting.setEstimatedPrice(paintingForm.getEstimatedPrice());
        
        artist.addToPaintingArray(aPainting);
       
        // commit to the database
        // using log level of SEVERE to show the query execution
        ctxt.commitChanges(Level.SEVERE); 

        
	return (mapping.findForward("success"));

    }
    
}
