package org.objectstyle.art;

import java.util.*;

public class ArtGroup extends org.objectstyle.cayenne.CayenneDataObject {

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }
    
    public void setToParentGroup(ArtGroup parent) {
       setToOneTarget("toParentGroup", parent, true);
    }
    
    public ArtGroup getToParentGroup() {
    	return (ArtGroup)readProperty("toParentGroup");
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
    
    public void addToChildGroupsArray(Artist obj) {
        addToManyTarget("toChildGroups", obj, true);
    }
    public void removeFromChildGroupsArray(Artist obj) {
        removeToManyTarget("toChildGroups", obj, true);
    }
    public List getChildGroupstArray() {
        return (List)readProperty("toChildGroups");
    }

}



