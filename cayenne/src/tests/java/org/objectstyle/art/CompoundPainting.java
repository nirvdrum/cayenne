package org.objectstyle.art;

public class CompoundPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public String getArtistName() {
        return (String)readProperty("artistName");
    }
    
    
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public String getGalleryName() {
        return (String)readProperty("galleryName");
    }
    
    
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
    public Gallery getToGallery() {
        return (Gallery)readProperty("toGallery");
    } 
    
    
    public PaintingInfo getToPaintingInfo() {
        return (PaintingInfo)readProperty("toPaintingInfo");
    } 
    
    
}



