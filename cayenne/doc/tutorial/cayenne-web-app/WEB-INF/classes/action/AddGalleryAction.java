package action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;

import formbean.*;

public final class AddGalleryAction extends Action {
    
        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
        
        form = new GalleryForm();
       
        request.setAttribute(mapping.getAttribute(), form);
        
        saveToken(request);
        
	return (mapping.findForward("success"));

    }
    
}
