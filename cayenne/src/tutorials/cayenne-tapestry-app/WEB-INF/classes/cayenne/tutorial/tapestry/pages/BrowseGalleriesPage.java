package cayenne.tutorial.tapestry.pages;

import java.util.*;

import cayenne.tutorial.tapestry.*;
import cayenne.tutorial.tapestry.domain.*;

import net.sf.tapestry.*;
import net.sf.tapestry.html.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.query.*;

/**
 * @author Eric Schneider
 *
 */
public class BrowseGalleriesPage extends BasePage {

	private DataContext ctxt;

	private List galleryList;
	public Gallery gallery;
	public Painting painting;

	public void setGalleryList(List value) {
		galleryList = value;
	}

	public List getGalleryList() {
		return galleryList;
	}

	public void removePaintingAction(IRequestCycle cycle) {

		gallery.removeFromPaintingArray(painting);

		// commit to the database
		ctxt.commitChanges();

	}

	protected void prepareForRender(IRequestCycle cycle)
		throws RequestCycleException {

		super.prepareForRender(cycle);

		Visit visit = (Visit) this.getPage().getVisit();
		ctxt = visit.getDataContext();

		SelectQuery query = new SelectQuery("Gallery");
		Ordering ordering = new Ordering("galleryName", Ordering.ASC);
		query.addOrdering(ordering);

		galleryList = ctxt.performQuery(query);
	}

	public void detach() {
		super.detach();

		galleryList = null;
		gallery = null;
		painting = null;
	}

}
