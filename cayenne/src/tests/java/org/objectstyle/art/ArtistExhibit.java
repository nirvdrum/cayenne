package org.objectstyle.art;

public class ArtistExhibit extends org.objectstyle.cayenne.CayenneDataObject {

    public void setToArtist(Artist toArtist) {
        setToOneTarget("toArtist", toArtist, true);
    }
    public Artist getToArtist() {
        return (Artist)readProperty("toArtist");
    } 
    
    
    public void setToExhibit(Exhibit toExhibit) {
        setToOneTarget("toExhibit", toExhibit, true);
    }
    public Exhibit getToExhibit() {
        return (Exhibit)readProperty("toExhibit");
    } 
    
    
}



