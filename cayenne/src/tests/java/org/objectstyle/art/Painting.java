package org.objectstyle.art;

public class Painting extends org.objectstyle.art.ArtDataObject {

    public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public void setPaintingTitle(String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public void setToArtist(Artist toArtist) {
        setToOneTarget("toArtist", toArtist, true);
    }
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
    public void setToGallery(Gallery toGallery) {
        setToOneTarget("toGallery", toGallery, true);
    }
    public Gallery getToGallery() {
        return (Gallery)readProperty("toGallery");
    } 
    
    
    public void setToPaintingInfo(PaintingInfo toPaintingInfo) {
        setToOneTarget("toPaintingInfo", toPaintingInfo, true);
    }
    public PaintingInfo getToPaintingInfo() {
        return (PaintingInfo)readProperty("toPaintingInfo");
    } 
    
    
}



