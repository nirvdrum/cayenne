package org.objectstyle.art;

import java.util.List;

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
    
    
    public void addToChildGroupsArray(ArtGroup obj) {
        addToManyTarget("childGroupsArray", obj, true);
    }
    public void removeFromChildGroupsArray(ArtGroup obj) {
        removeToManyTarget("childGroupsArray", obj, true);
    }
    public List getChildGroupsArray() {
        return (List)readProperty("childGroupsArray");
    }
    
    
    public void setToParentGroup(ArtGroup toParentGroup) {
        setToOneTarget("toParentGroup", toParentGroup, true);
    }
    public ArtGroup getToParentGroup() {
        return (ArtGroup)readProperty("toParentGroup");
    } 
    
    
}



