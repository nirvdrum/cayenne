package org.objectstyle.art;


public class BlobTest extends org.objectstyle.cayenne.CayenneDataObject {

    public void setBlobCol(byte[] blobCol) {
        writeProperty("blobCol", blobCol);
    }
    public byte[] getBlobCol() {
        return (byte[])readProperty("blobCol");
    }
    
    
}



