package cayenne.tutorial.tapestry.pages;

import java.util.List;

import cayenne.tutorial.tapestry.Visit;
import cayenne.tutorial.tapestry.domain.Gallery;
import cayenne.tutorial.tapestry.domain.Painting;

import net.sf.tapestry.IRequestCycle;
import net.sf.tapestry.RequestCycleException;
import net.sf.tapestry.html.BasePage;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

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

		SelectQuery query = new SelectQuery(Gallery.class);
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
