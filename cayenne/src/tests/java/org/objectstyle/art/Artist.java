package org.objectstyle.art;

import java.util.*;
import org.objectstyle.cayenne.*;

public class Artist extends org.objectstyle.cayenne.CayenneDataObject {

    public void setArtistName(String artistName) {
        writeProperty("artistName", artistName);
    }
    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public void setDateOfBirth(Date dateOfBirth) {
        writeProperty("dateOfBirth", dateOfBirth);
    }
    public Date getDateOfBirth() {
        return (Date)readProperty("dateOfBirth");
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
    
    
    public void addToGroupArray(ArtGroup obj) {
        addToManyTarget("groupArray", obj, true);
    }
    public void removeFromGroupArray(ArtGroup obj) {
        removeToManyTarget("groupArray", obj, true);
    }
    public List getGroupArray() {
        return (List)readProperty("groupArray");
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



