package cayenne.tutorial.tapestry.pages;

import cayenne.tutorial.tapestry.Visit;
import cayenne.tutorial.tapestry.domain.Gallery;
import cayenne.tutorial.tapestry.domain.Painting;

import java.util.List;

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
public class ChooseGalleryPage extends BasePage {

	private DataContext ctxt;

	public List galleryList;
	private Painting painting;
	public Gallery gallery;

	public Painting getPainting() {
		return painting;
	}

	public void setPainting(Painting value) {
		painting = value;
	}

	public void savePaintingToGalleryAction(IRequestCycle cycle) {

		gallery.addToPaintingArray(painting);

		// commit to the database
		ctxt.commitChanges();

		BrowseArtistsPage nextPage =
			(BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");

		cycle.setPage(nextPage);

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

		ctxt = null;
		galleryList = null;
		gallery = null;
	}

}
