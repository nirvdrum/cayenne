package org.objectstyle.art;

import java.util.List;

public class ROArtist extends org.objectstyle.cayenne.CayenneDataObject {

    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public java.sql.Date getDateOfBirth() {
        return (java.sql.Date)readProperty("dateOfBirth");
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



