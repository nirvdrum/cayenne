package cayenne.tutorial.tapestry.pages;

import cayenne.tutorial.tapestry.*;
import cayenne.tutorial.tapestry.domain.*;

import net.sf.tapestry.*;
import org.objectstyle.cayenne.access.*;

/**
 * @author Eric Schneider
 *
 */
public class AddArtistPage extends EditorPage {
	
	private Artist artist;
	
	public AddArtistPage() {
		super();	
	}
	
	public void setArtist(Artist value) {
		artist = value;
	}

	public Artist getArtist() {
		return artist;
	}
	
	public void saveArtistAction(IRequestCycle cycle) {
		
		Visit visit = (Visit) this.getPage().getVisit();
		DataContext ctxt = visit.getDataContext();
		
		if (!assertNotNull(artist.getArtistName()) ) {
			appendHtmlToErrorMessage("You must provide a name.");	
		}
		
		if (!assertNotNull(artist.getDateOfBirth()) ) {
			appendHtmlToErrorMessage("You must provide a DOB.");	
		}
		
		if (getHasErrorMessage())
			return;
		
		ctxt.registerNewObject(artist);

		// commit to the database
		ctxt.commitChanges();
		
		BrowseArtistsPage nextPage =
			(BrowseArtistsPage) cycle.getPage("BrowseArtistsPage");
		
		cycle.setPage(nextPage);
	}
	
	public void initialize() {
		super.initialize();
		
		artist = new Artist();
	}

}
