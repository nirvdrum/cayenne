package test;

import java.util.List;
import org.objectstyle.cayenne.*;

public class Artist extends CayenneDataObject {

    public void setDateOfBirth(java.sql.Date dateOfBirth) {
        writeProperty("dateOfBirth", dateOfBirth);
    }
    public java.sql.Date getDateOfBirth() {
        return (java.sql.Date)readProperty("dateOfBirth");
    }
    
    
    public void setArtistName(java.lang.String artistName) {
        writeProperty("artistName", artistName);
    }
    public java.lang.String getArtistName() {
        return (java.lang.String)readProperty("artistName");
    }
    
    
    public void addToPaintingArray(test.Painting obj) {
        addToManyTarget("paintingArray", obj, true);
    }
    public void removeFromPaintingArray(test.Painting obj) {
        removeToManyTarget("paintingArray", obj, true);
    }
    public List getPaintingArray() {
        return (List)readProperty("paintingArray");
    }
    
    
}



