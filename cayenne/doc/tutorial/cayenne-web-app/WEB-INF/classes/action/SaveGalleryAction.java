package action;

import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import test.Gallery;
import org.objectstyle.cayenne.access.DataContext;
import formbean.*;

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
        ctxt.commitChanges(Level.WARN); 

        
	return (mapping.findForward("success"));
    }
    
}