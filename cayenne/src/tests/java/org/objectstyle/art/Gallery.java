package org.objectstyle.art;

import java.util.List;

public class Gallery extends org.objectstyle.cayenne.CayenneDataObject {

    public void setGalleryName(String galleryName) {
        writeProperty("galleryName", galleryName);
    }
    public String getGalleryName() {
        return (String)readProperty("galleryName");
    }
    
    
    public void addToExhibitArray(Exhibit obj) {
        addToManyTarget("exhibitArray", obj, true);
    }
    public void removeFromExhibitArray(Exhibit obj) {
        removeToManyTarget("exhibitArray", obj, true);
    }
    public List getExhibitArray() {
        return (List)readProperty("exhibitArray");
    }
    
    
    public void addToPaintingArray(Painting obj) {
        addToManyTarget("paintingArray", obj, true);
    }
    public void removeFromPaintingArray(Painting obj) {
        removeToManyTarget("paintingArray", obj, true);
    }
    public List getPaintingArray() {
        return (List)readProperty("paintingArray");
    }
    
    
}



