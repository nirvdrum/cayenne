package action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;
import org.apache.struts.action.ActionServlet;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import java.util.logging.Level;
import formbean.ArtistForm;
import test.Artist;

public final class SaveArtistAction extends Action {

        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
        
        System.err.println("****Inside SaveArtistAction.execute()");    
        ArtistForm artistForm = (ArtistForm) form;
        
        // Validate the user form information
	ActionErrors errors = new ActionErrors();
        errors = artistForm.validate(mapping, request);
        
        // Report any errors we have discovered back to the original form
	if (!errors.empty()) {
	    saveErrors(request, errors);
            saveToken(request);
	    return (new ActionForward(mapping.getInput()));
	}

        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
	
        Artist anArtist = (Artist)ctxt.createAndRegisterNewObject("Artist");
        anArtist.setArtistName(artistForm.getArtistName());
        anArtist.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));
       
        // commit to the database
        // using log level of SEVERE to show the query execution
        ctxt.commitChanges(Level.SEVERE); 

        
	return (mapping.findForward("success"));

    }
    
}

