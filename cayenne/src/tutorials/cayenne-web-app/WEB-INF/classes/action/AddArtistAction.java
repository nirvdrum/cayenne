package action;

import javax.servlet.http.*;

import org.apache.struts.action.*;

import formbean.*;

public final class AddArtistAction extends Action {

    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws Exception {

        form = new ArtistForm();

        request.setAttribute(mapping.getAttribute(), form);

        saveToken(request);

        return mapping.findForward("success");
    }
}
