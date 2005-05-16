package org.objectstyle.art.auto;

import java.sql.Date;
import java.util.List;

import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.CayenneDataObject;

/** Class _ROArtist was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _ROArtist extends CayenneDataObject {

    public static final String ARTIST_NAME_PROPERTY = "artistName";
    public static final String DATE_OF_BIRTH_PROPERTY = "dateOfBirth";
    public static final String PAINTING_ARRAY_PROPERTY = "paintingArray";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public Date getDateOfBirth() {
        return (Date)readProperty("dateOfBirth");
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