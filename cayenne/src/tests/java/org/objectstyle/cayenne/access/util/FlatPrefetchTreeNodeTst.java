package org.objectstyle.cayenne.access.util;

import java.util.Arrays;
import java.util.Collection;

import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class FlatPrefetchTreeNodeTst extends CayenneTestCase {

    public void testBuildPrefix1() {
        Collection prefetches = Arrays.asList(new Object[] {
            "toArtist"
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches);
        assertEquals("", paintNode.buildPrefix(new StringBuffer()).toString());

        FlatPrefetchTreeNode artistNode = (FlatPrefetchTreeNode) paintNode
                .getChildren()
                .iterator()
                .next();

        assertEquals("toArtist.", artistNode.buildPrefix(new StringBuffer()).toString());
    }

    public void testBuildPrefix2() {
        Collection prefetches = Arrays.asList(new Object[] {
                "toArtist", "toArtist.groupArray"
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches);
        assertEquals("", paintNode.buildPrefix(new StringBuffer()).toString());

        FlatPrefetchTreeNode artistNode = (FlatPrefetchTreeNode) paintNode
                .getChildren()
                .iterator()
                .next();

        assertEquals("toArtist.", artistNode.buildPrefix(new StringBuffer()).toString());

        // more complicated case - flattened relationship and names of db relationships do
        // not match names of obj relationships
        FlatPrefetchTreeNode groupNode = (FlatPrefetchTreeNode) artistNode
                .getChildren()
                .iterator()
                .next();

        assertEquals("toArtist.artistGroupArray.toGroup.", groupNode
                .buildPrefix(new StringBuffer())
                .toString());
    }

    public void testSourceForTarget() {
        Collection prefetches = Arrays.asList(new Object[] {
            "toArtist"
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches);
        assertEquals("PAINTING_TITLE", paintNode.sourceForTarget("PAINTING_TITLE"));
        assertEquals("ARTIST_ID", paintNode.sourceForTarget("ARTIST_ID"));

        FlatPrefetchTreeNode artistNode = (FlatPrefetchTreeNode) paintNode
                .getChildren()
                .iterator()
                .next();

        assertEquals("ARTIST_ID", artistNode.sourceForTarget("ARTIST_ID"));
        assertEquals("toArtist.ARTIST_NAME", artistNode.sourceForTarget("ARTIST_NAME"));
    }

    public void testTreeAssembly() {
        Collection prefetches = Arrays.asList(new Object[] {
            "toArtist"
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode node = new FlatPrefetchTreeNode(paint, prefetches);

        assertNotNull(node.getChildren());
        assertEquals(1, node.getChildren().size());
        FlatPrefetchTreeNode child = (FlatPrefetchTreeNode) node
                .getChildren()
                .iterator()
                .next();

        assertSame(node, child.getParent());
    }
}