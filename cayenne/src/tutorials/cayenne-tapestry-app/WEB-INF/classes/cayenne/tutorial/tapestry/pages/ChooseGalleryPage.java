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

		SelectQuery query = new SelectQuery("Gallery");
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
