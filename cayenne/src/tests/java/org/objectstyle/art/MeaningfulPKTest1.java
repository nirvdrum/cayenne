package org.objectstyle.art;

import java.util.List;

public class MeaningfulPKTest1 extends org.objectstyle.cayenne.CayenneDataObject {

    public void setArtistId(Integer artistId) {
        writeProperty("artistId", artistId);
    }
    public Integer getArtistId() {
        return (Integer)readProperty("artistId");
    }
    
    
    public void setArtistName(String artistName) {
        writeProperty("artistName", artistName);
    }
    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public void setDateOfBirth(java.util.Date dateOfBirth) {
        writeProperty("dateOfBirth", dateOfBirth);
    }
    public java.util.Date getDateOfBirth() {
        return (java.util.Date)readProperty("dateOfBirth");
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
    
    
    public void addToPaintingArray(CompoundPainting obj) {
        addToManyTarget("paintingArray", obj, true);
    }
    public void removeFromPaintingArray(CompoundPainting obj) {
        removeToManyTarget("paintingArray", obj, true);
    }
    public List getPaintingArray() {
        return (List)readProperty("paintingArray");
    }
    
    
}



