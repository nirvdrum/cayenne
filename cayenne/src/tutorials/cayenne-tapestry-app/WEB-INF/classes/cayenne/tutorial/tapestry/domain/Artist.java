package cayenne.tutorial.tapestry.domain;

import java.util.List;

import org.objectstyle.cayenne.CayenneDataObject;

/**
 * @author Eric Schneider
 *
 */
public class Artist extends CayenneDataObject {

	public void setDateOfBirth(java.sql.Date dateOfBirth) {
		writeProperty("dateOfBirth", dateOfBirth);
	}
	public java.sql.Date getDateOfBirth() {
		return (java.sql.Date) readProperty("dateOfBirth");
	}

	public void setArtistName(java.lang.String artistName) {
		writeProperty("artistName", artistName);
	}
	public java.lang.String getArtistName() {
		return (java.lang.String) readProperty("artistName");
	}

	public void addToPaintingArray(Painting obj) {
		addToManyTarget("paintingArray", obj, true);
	}
	public void removeFromPaintingArray(Painting obj) {
		removeToManyTarget("paintingArray", obj, true);
	}
	public List getPaintingArray() {
		return (List) readProperty("paintingArray");
	}

}
