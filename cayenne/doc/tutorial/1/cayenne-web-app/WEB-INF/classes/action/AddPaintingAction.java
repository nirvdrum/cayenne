package action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import formbean.PaintingForm;

public final class AddPaintingAction extends Action {
    
        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
        
        form = new PaintingForm();
        ((PaintingForm)form).setArtistName(request.getParameter("name"));
       
        request.setAttribute(mapping.getAttribute(), form);
        
        saveToken(request);
        
	return (mapping.findForward("success"));

    }
    
}
