package org.objectstyle.art.auto;

import org.objectstyle.cayenne.CayenneDataObject;

/** 
 * Class _ArtistPaintingCounts was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually, 
 * since it may be overwritten next time code is regenerated. 
 * If you need to make any customizations, please use subclass. 
 */
public class _ArtistPaintingCounts extends CayenneDataObject {

    public static final String PAINTINGS_COUNT_PROPERTY = "paintingsCount";

    public static final String ARTIST_ID_PK_COLUMN = "ARTIST_ID";

    public void setPaintingsCount(Integer paintingsCount) {
        writeProperty("paintingsCount", paintingsCount);
    }
    public Integer getPaintingsCount() {
        return (Integer)readProperty("paintingsCount");
    }
    
    
}
