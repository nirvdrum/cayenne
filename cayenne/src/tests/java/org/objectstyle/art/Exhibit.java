package org.objectstyle.art;

import java.util.List;

public class Exhibit extends org.objectstyle.cayenne.CayenneDataObject {

    public void setClosingDate(java.util.Date closingDate) {
        writeProperty("closingDate", closingDate);
    }
    public java.util.Date getClosingDate() {
        return (java.util.Date)readProperty("closingDate");
    }
    
    
    public void setOpeningDate(java.util.Date openingDate) {
        writeProperty("openingDate", openingDate);
    }
    public java.util.Date getOpeningDate() {
        return (java.util.Date)readProperty("openingDate");
    }
    
    
    public void addToArtistExhibitArray(ArtistExhibit obj) {
        addToManyTarget("artistExhibitArray", obj, true);
    }
    public void removeFromArtistExhibitArray(ArtistExhibit obj) {
        removeToManyTarget("artistExhibitArray", obj, true);
    }
    public List getArtistExhibitArray() {
        return (List)readProperty("artistExhibitArray");
    }
    
    
    public void setToGallery(Gallery toGallery) {
        setToOneTarget("toGallery", toGallery, true);
    }
    public Gallery getToGallery() {
        return (Gallery)readProperty("toGallery");
    } 
    
    
}



