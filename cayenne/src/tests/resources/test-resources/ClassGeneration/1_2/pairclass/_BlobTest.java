package org.objectstyle.art.auto;

import org.objectstyle.cayenne.CayenneDataObject;

/** Class _BlobTest was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _BlobTest extends CayenneDataObject {

    public static final String BLOB_COL_PROPERTY = "blobCol";

    public static final String BLOB_TEST_ID_PK_COLUMN = "BLOB_TEST_ID";

    public void setBlobCol(byte[] blobCol) {
        writeProperty("blobCol", blobCol);
    }
    public byte[] getBlobCol() {
        return (byte[])readProperty("blobCol");
    }
    
    
}