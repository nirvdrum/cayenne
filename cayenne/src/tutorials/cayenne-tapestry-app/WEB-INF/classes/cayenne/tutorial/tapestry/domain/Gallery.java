package cayenne.tutorial.tapestry.domain;

import java.util.List;
import org.objectstyle.cayenne.*;

/**
 * @author Eric Schneider
 *
 */

public class Gallery extends CayenneDataObject {

	public void setGalleryName(java.lang.String galleryName) {
		writeProperty("galleryName", galleryName);
	}
	public java.lang.String getGalleryName() {
		return (java.lang.String) readProperty("galleryName");
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
