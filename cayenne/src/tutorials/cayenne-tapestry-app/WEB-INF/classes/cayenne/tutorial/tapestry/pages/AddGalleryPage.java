package cayenne.tutorial.tapestry.pages;

import cayenne.tutorial.tapestry.*;
import cayenne.tutorial.tapestry.domain.*;

import net.sf.tapestry.*;
import org.objectstyle.cayenne.access.*;

/**
 * @author Eric Schneider
 *
 */
public class AddGalleryPage extends EditorPage {

	private Gallery gallery;

	public void setGallery(Gallery value) {
		gallery = value;
	}

	public Gallery getGallery() {
		return gallery;
	}

	public void saveGalleryAction(IRequestCycle cycle) {
		
		if (!assertNotNull(gallery.getGalleryName()) ) {
			appendHtmlToErrorMessage("You must provide a gallery name.");	
			return;
		}

		Visit visit = (Visit) this.getPage().getVisit();
		DataContext ctxt = visit.getDataContext();

		ctxt.registerNewObject(gallery);

		// commit to the database
		ctxt.commitChanges();

		BrowseGalleriesPage nextPage =
			(BrowseGalleriesPage) cycle.getPage("BrowseGalleriesPage");

		cycle.setPage(nextPage);
	}

	public void initialize() {
		super.initialize();
		
		gallery = new Gallery();
	}

}
