package test;

import java.util.List;
import org.objectstyle.cayenne.*;

public class Painting extends CayenneDataObject {

    public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public void setPaintingTitle(java.lang.String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public java.lang.String getPaintingTitle() {
        return (java.lang.String)readProperty("paintingTitle");
    }
    
    
    public void setToGallery(test.Gallery toGallery) {
        setToOneTarget("toGallery", toGallery, true);
    }
    
    public test.Gallery getToGallery() {
        return (test.Gallery)readProperty("toGallery");
    } 
    
    
    public void setToArtist(test.Artist toArtist) {
        setToOneTarget("toArtist", toArtist, true);
    }
    
    public test.Artist getToArtist() {
        return (test.Artist)readProperty("toArtist");
    } 
    
    
}



