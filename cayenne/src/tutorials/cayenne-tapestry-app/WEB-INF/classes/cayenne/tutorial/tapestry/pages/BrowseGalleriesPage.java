package cayenne.tutorial.tapestry.pages;

import java.util.List;

import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.event.PageEvent;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

import cayenne.tutorial.tapestry.domain.Gallery;
import cayenne.tutorial.tapestry.domain.Painting;

/**
 * Page displaying galleries with their paintings.
 * 
 * @author Eric Schneider
 */
public abstract class BrowseGalleriesPage extends ApplicationPage {

    // properties are defined as abstract setters and getters
    // and are declared in BrowseGalleriesPage.page file
    public abstract void setGallery(Gallery value);
    public abstract Gallery getGallery();

    public abstract void setPainting(Painting value);
    public abstract Painting getPainting();

    public abstract void setGalleryList(List value);
    public abstract List getGalleryList();

    public void removePaintingAction(IRequestCycle cycle) {
        getGallery().removeFromPaintingArray(getPainting());

        // commit to the database
        getVisitDataContext().commitChanges();
    }

    public void pageBeginRender(PageEvent event) {
        // fetch the galleries if we do not have them cached
        if (getGalleryList() == null) {
            SelectQuery query = new SelectQuery(Gallery.class);
            Ordering ordering = new Ordering("galleryName", Ordering.ASC);
            query.addOrdering(ordering);

            setGalleryList(getVisitDataContext().performQuery(query));
        }
    }
}
