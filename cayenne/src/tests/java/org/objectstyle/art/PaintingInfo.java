package org.objectstyle.art;

public class PaintingInfo extends org.objectstyle.cayenne.CayenneDataObject {

    public void setImageBlob(byte[] imageBlob) {
        writeProperty("imageBlob", imageBlob);
    }
    public byte[] getImageBlob() {
        return (byte[])readProperty("imageBlob");
    }
    
    
    public void setTextReview(String textReview) {
        writeProperty("textReview", textReview);
    }
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
    public void setPainting(Painting painting) {
        setToOneTarget("painting", painting, true);
    }
    public Painting getPainting() {
        return (Painting)readProperty("painting");
    } 
    
    
}



