package org.objectstyle.art;

public class ArtistPaintingCounts extends org.objectstyle.cayenne.CayenneDataObject {

    public void setPaintingsCount(Integer paintingsCount) {
        writeProperty("paintingsCount", paintingsCount);
    }
    public Integer getPaintingsCount() {
        return (Integer)readProperty("paintingsCount");
    }
    
    
}



