package org.objectstyle.art;

public class SubPainting extends org.objectstyle.cayenne.CayenneDataObject {

    public void setPaintingTitle(String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
}



