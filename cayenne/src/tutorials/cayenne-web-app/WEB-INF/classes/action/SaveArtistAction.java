package action;

import javax.servlet.http.*;

import org.apache.log4j.*;
import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.*;
import webtest.*;

import formbean.*;

public final class SaveArtistAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
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

        DataContext ctxt = (DataContext) request.getSession().getAttribute("context");

        Artist anArtist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        anArtist.setArtistName(artistForm.getArtistName());
        anArtist.setDateOfBirth(new java.sql.Date(System.currentTimeMillis()));

        // commit to the database
        // using log level of WARN to show the query execution
        ctxt.commitChanges(Level.WARN);

        return (mapping.findForward("success"));

    }

}
