package action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.objectstyle.cayenne.access.DataContext;
import java.util.logging.Level;
import formbean.GalleryForm;
import test.Gallery;

public final class SaveGalleryAction extends Action {

        public ActionForward execute(ActionMapping mapping,
				 ActionForm form,
				 HttpServletRequest request,
				 HttpServletResponse response)
	throws Exception {
            
        DataContext ctxt = (DataContext)request.getSession().getAttribute("context");
            
        GalleryForm galleryForm = (GalleryForm) form;
	
        Gallery aGallery = (Gallery)ctxt.createAndRegisterNewObject("Gallery");
        aGallery.setGalleryName(galleryForm.getGalleryName());
       
        // commit to the database
        ctxt.commitChanges(Level.SEVERE); 

        
	return (mapping.findForward("success"));
    }
    
}