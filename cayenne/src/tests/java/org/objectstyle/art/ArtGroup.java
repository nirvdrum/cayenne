package org.objectstyle.art;

import java.util.List;
import org.objectstyle.cayenne.*;

public class ArtGroup extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    
    public void addToArtistArray(Artist obj) {
        addToManyTarget("artistArray", obj, true);
    }
    public void removeFromArtistArray(Artist obj) {
        removeToManyTarget("artistArray", obj, true);
    }
    public List getArtistArray() {
        return (List)readProperty("artistArray");
    }
    
    
}



