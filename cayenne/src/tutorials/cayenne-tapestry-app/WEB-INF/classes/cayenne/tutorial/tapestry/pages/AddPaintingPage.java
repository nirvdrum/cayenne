package cayenne.tutorial.tapestry.pages;

import java.math.*;

import cayenne.tutorial.tapestry.*;
import cayenne.tutorial.tapestry.domain.*;

import net.sf.tapestry.*;
import org.objectstyle.cayenne.access.*;

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
