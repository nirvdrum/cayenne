package cayenne.tutorial.tapestry.pages;

import java.math.BigDecimal;

import net.sf.tapestry.IRequestCycle;
import org.objectstyle.cayenne.access.DataContext;

import cayenne.tutorial.tapestry.Visit;
import cayenne.tutorial.tapestry.domain.Artist;
import cayenne.tutorial.tapestry.domain.Painting;

/**
 * @author Eric Schneider
 *
 */
public class AddPaintingPage extends EditorPage {
	
	public Painting painting;
	private Artist artist;
	
	public Artist getArtist() {
		return artist;	
	}
	
	public void setArtist(Artist value) {
		artist = value;	
	}
	
	public void savePaintingAction(IRequestCycle cycle) {
		
		if (!assertNotNull(painting.getPaintingTitle()) ) {
			appendHtmlToErrorMessage("You must provide a painting title.");	
			return; 
		}
					
		Visit visit = (Visit) this.getPage().getVisit();
		DataContext ctxt = visit.getDataContext();
		
		ctxt.registerNewObject(painting);

		artist.addToPaintingArray(painting);

		// commit to the database
		ctxt.commitChanges();

		BrowseArtistsPage nextPage =
			(BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");

		cycle.setPage(nextPage);
	}
	
	public void initialize() {
		super.initialize();
		
		painting = new Painting();
		painting.setEstimatedPrice(new BigDecimal(0));
	}	

}
