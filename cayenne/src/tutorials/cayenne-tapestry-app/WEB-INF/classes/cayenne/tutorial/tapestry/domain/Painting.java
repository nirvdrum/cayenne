package cayenne.tutorial.tapestry.domain;

import org.objectstyle.cayenne.*;

/**
 * @author Eric Schneider
 *
 */

public class Painting extends CayenneDataObject {

	public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
		writeProperty("estimatedPrice", estimatedPrice);
	}
	public java.math.BigDecimal getEstimatedPrice() {
		return (java.math.BigDecimal) readProperty("estimatedPrice");
	}

	public void setPaintingTitle(java.lang.String paintingTitle) {
		writeProperty("paintingTitle", paintingTitle);
	}
	public java.lang.String getPaintingTitle() {
		return (java.lang.String) readProperty("paintingTitle");
	}

	public void setToGallery(Gallery toGallery) {
		setToOneTarget("toGallery", toGallery, true);
	}

	public Gallery getToGallery() {
		return (Gallery) readProperty("toGallery");
	}

	public void setToArtist(Artist toArtist) {
		setToOneTarget("toArtist", toArtist, true);
	}

	public Artist getToArtist() {
		return (Artist) readProperty("toArtist");
	}

}



