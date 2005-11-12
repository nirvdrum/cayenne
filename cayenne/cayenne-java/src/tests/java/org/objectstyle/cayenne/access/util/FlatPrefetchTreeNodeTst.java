package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.collections.Closure;
import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.Prefetch;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class FlatPrefetchTreeNodeTst extends CayenneTestCase {

    public void testBuildPrefix1() {
        Collection prefetches = Arrays.asList(new Object[] {
            new Prefetch("toArtist")
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches, null);
        assertEquals("", paintNode.buildPrefix(new StringBuffer()).toString());

        FlatPrefetchTreeNode artistNode = (FlatPrefetchTreeNode) paintNode
                .getChildren()
                .iterator()
                .next();

        assertEquals("toArtist.", artistNode.buildPrefix(new StringBuffer()).toString());
    }

    public void testBuildPrefix2() {
        Collection prefetches = Arrays.asList(new Object[] {
                new Prefetch("toArtist"), new Prefetch("toArtist.groupArray")
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches, null);
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

        assertEquals("toArtist.artistGroupArray.toGroup.", groupNode.buildPrefix(
                new StringBuffer()).toString());
    }

    public void testSourceForTarget() {
        Collection prefetches = Arrays.asList(new Object[] {
            new Prefetch("toArtist")
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode paintNode = new FlatPrefetchTreeNode(paint, prefetches, null);
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
            new Prefetch("toArtist")
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        FlatPrefetchTreeNode node = new FlatPrefetchTreeNode(paint, prefetches, null);

        assertNotNull(node.getChildren());
        assertEquals(1, node.getChildren().size());
        FlatPrefetchTreeNode child = (FlatPrefetchTreeNode) node
                .getChildren()
                .iterator()
                .next();

        assertSame(node, child.getParent());
    }

    public void testPrefetchBlocking1() {
        Collection prefetches = Arrays.asList(new Object[] {
                new Prefetch("toArtist"), new Prefetch("toArtist.groupArray")
        });

        DataContext context = createDataContext();
        ObjEntity paint = context.getEntityResolver().lookupObjEntity(Painting.class);

        Expression q1 = Expression.fromString("toArtist.artistName = 'aaa'");
        FlatPrefetchTreeNode n1 = new FlatPrefetchTreeNode(paint, prefetches, q1);
        Collection nonPhantom1 = new NonPhanotomFilter().filterNonPhantom(n1);
        assertEquals(2, nonPhantom1.size());

        Expression q2 = Expression.fromString("toArtist.groupArray = 'aaa'");
        FlatPrefetchTreeNode n2 = new FlatPrefetchTreeNode(paint, prefetches, q2);
        Collection nonPhantom2 = new NonPhanotomFilter().filterNonPhantom(n2);
        assertEquals(1, nonPhantom2.size());
    }

    public void testPrefetchBlocking2() {
        Collection prefetches = Arrays.asList(new Object[] {
                new Prefetch("paintingArray"), new Prefetch("paintingArray.toPaintingInfo")
        });

        DataContext context = createDataContext();
        ObjEntity artist = context.getEntityResolver().lookupObjEntity(Artist.class);

        Expression q1 = Expression.fromString("groupArray = 'aaa'");
        FlatPrefetchTreeNode n1 = new FlatPrefetchTreeNode(artist, prefetches, q1);
        Collection nonPhantom1 = new NonPhanotomFilter().filterNonPhantom(n1);
        assertEquals(2, nonPhantom1.size());

        Expression q2 = Expression.fromString("paintingArray.paintingTitle = 'aaa'");
        FlatPrefetchTreeNode n2 = new FlatPrefetchTreeNode(artist, prefetches, q2);
        Collection nonPhantom2 = new NonPhanotomFilter().filterNonPhantom(n2);
        assertEquals(1, nonPhantom2.size());
    }

    static class NonPhanotomFilter implements Closure {

        Collection nonPhantom;

        public void execute(Object object) {
            FlatPrefetchTreeNode node = (FlatPrefetchTreeNode) object;
            if (!node.isPhantom()) {
                nonPhantom.add(node);
            }
        }

        Collection filterNonPhantom(FlatPrefetchTreeNode root) {
            nonPhantom = new ArrayList();
            root.executeDepthFirst(this);
            nonPhantom.remove(root);
            return nonPhantom;
        }
    }
}