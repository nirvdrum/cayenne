package cayenne.tutorial.tapestry.pages;

import java.util.List;

import cayenne.tutorial.tapestry.Visit;
import cayenne.tutorial.tapestry.domain.Artist;
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

		SelectQuery query = new SelectQuery(Artist.class);
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
