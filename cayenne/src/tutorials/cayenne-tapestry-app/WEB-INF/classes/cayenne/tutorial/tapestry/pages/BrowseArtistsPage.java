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
public class BrowseArtistsPage extends BasePage {

	private List artistList;
	private Artist artist;
	public Painting painting;

	public void setArtistList(List value) {
		artistList = value;
	}

	public List getArtistList() {
		return artistList;
	}

	public void setArtist(Artist value) {
		artist = value;
	}

	public Artist getArtist() {
		return artist;
	}

	public boolean getIsOnDisplay() {
		return (painting.getToGallery() != null);
	}

	public void addPaintingAction(IRequestCycle cycle) {
		AddPaintingPage nextPage =
			(AddPaintingPage) cycle.getPage("AddPaintingPage");

		nextPage.setArtist(getArtist());

		cycle.setPage(nextPage);
	}

	public void submitPaintingToGalleryAction(IRequestCycle cycle) {
		ChooseGalleryPage nextPage =
			(ChooseGalleryPage) cycle.getPage("ChooseGalleryPage");

		nextPage.setPainting(painting);

		cycle.setPage(nextPage);
	}

	protected void prepareForRender(IRequestCycle cycle)
		throws RequestCycleException {

		super.prepareForRender(cycle);

		Visit visit = (Visit) getVisit();

		DataContext ctxt = visit.getDataContext();

		SelectQuery query = new SelectQuery("Artist");
		Ordering ordering = new Ordering("artistName", Ordering.ASC);
		query.addOrdering(ordering);

		artistList = ctxt.performQuery(query);
	}

	public void detach() {
		super.detach();

		artistList = null;
		artist = null;
		painting = null;
	}

}
