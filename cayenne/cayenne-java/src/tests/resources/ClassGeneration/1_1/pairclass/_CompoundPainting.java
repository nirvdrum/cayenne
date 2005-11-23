package org.objectstyle.art.auto;

/** Class _CompoundPainting was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _CompoundPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ARTIST_NAME_PROPERTY = "artistName";
    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String GALLERY_NAME_PROPERTY = "galleryName";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";
    public static final String TO_ARTIST_PROPERTY = "toArtist";
    public static final String TO_GALLERY_PROPERTY = "toGallery";
    public static final String TO_PAINTING_INFO_PROPERTY = "toPaintingInfo";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

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
    
    

    public org.objectstyle.art.Artist getToArtist() {
        return (org.objectstyle.art.Artist)readProperty("toArtist");
    } 
    
    

    public org.objectstyle.art.Gallery getToGallery() {
        return (org.objectstyle.art.Gallery)readProperty("toGallery");
    } 
    
    

    public org.objectstyle.art.PaintingInfo getToPaintingInfo() {
        return (org.objectstyle.art.PaintingInfo)readProperty("toPaintingInfo");
    } 
    
    
}