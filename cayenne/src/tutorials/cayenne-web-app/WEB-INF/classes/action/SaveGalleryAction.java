package action;

import javax.servlet.http.*;

import org.apache.log4j.*;
import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.*;
import webtest.*;

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