/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectFactory;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchProcessor;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

/**
 * An object that resolves a number of joint and disjoint result sets to an object tree
 * according to a given prefetch tree.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectTreeResolver {

    ObjEntity rootEntity;
    ObjectFactory factory;

    ObjectTreeResolver(ObjEntity rootEntity, ObjectFactory factory) {
        this.factory = factory;
        this.rootEntity = rootEntity;
    }

    List resolveObjectTree(
            PrefetchTreeNode tree,
            List mainResultRows,
            Map extraResultsByPath) {

        // create a copy of the tree using DecoratedPrefetchNodes and then traverse it
        // resolving objects...
        DecoratedPrefetchNode decoratedTree = new TreeBuilder(
                mainResultRows,
                extraResultsByPath).buildTree(tree);

        // do a single path for disjoint prefetches, joint subtrees will be processed at
        // each disjoint node that is a parent of joint prefetches.
        decoratedTree.traverse(new DisjointProcessor());

        // connect related objects
        decoratedTree.traverse(new PostProcessor());

        return decoratedTree.getObjects() != null
                ? decoratedTree.getObjects()
                : new ArrayList(1);
    }

    final class JointPrefetchLookahead implements PrefetchProcessor {

        boolean hasJointChildren;
        PrefetchTreeNode root;

        boolean hasJointChildren(PrefetchTreeNode node) {
            this.hasJointChildren = false;
            this.root = node;

            node.traverse(this);
            return hasJointChildren;
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            return processNode(node);
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            return processNode(node);
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return processNode(node);
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            return processNode(node);
        }

        boolean processNode(PrefetchTreeNode node) {
            if (hasJointChildren) {
                return false;
            }

            if (node == root) {
                return true;
            }

            if (node.isJointPrefetch()) {
                hasJointChildren = true;
                return false;
            }

            return true;
        }

    }

    // A PrefetchProcessor that creates a replica of a PrefetchTree with node
    // subclasses that can carry extra info needed during traversal.
    final class TreeBuilder implements PrefetchProcessor {

        DecoratedPrefetchNode root;
        LinkedList nodeStack;

        List mainResultRows;
        Map extraResultsByPath;
        JointPrefetchLookahead helper;

        TreeBuilder(List mainResultRows, Map extraResultsByPath) {
            this.mainResultRows = mainResultRows;
            this.extraResultsByPath = extraResultsByPath;
            this.helper = new JointPrefetchLookahead();
        }

        DecoratedPrefetchNode buildTree(PrefetchTreeNode tree) {
            // reset state
            this.nodeStack = new LinkedList();
            this.root = null;

            tree.traverse(this);

            if (root == null) {
                throw new CayenneRuntimeException(
                        "Failed to create prefetch processing tree.");
            }

            return root;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {

            // root should be treated as disjoint
            if (getParent() == null) {
                return startDisjointPrefetch(node);
            }
            else {
                DecoratedPrefetchNode decorated = new DecoratedPrefetchNode(
                        getParent(),
                        node.getSegmentPath());

                decorated.setPhantom(true);
                return addNode(decorated);
            }
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {

            // look ahead for joint children as joint children will require a different
            // node type.
            DecoratedPrefetchNode decorated = helper.hasJointChildren(node)
                    ? decorated = new DecoratedJointNode(getParent(), node
                            .getSegmentPath())
                    : new DecoratedPrefetchNode(getParent(), node.getSegmentPath());
            decorated.setPhantom(false);

            // semantics has to be "DISJOINT" even if the node is joint, as semantics
            // defines relationship with parent..
            decorated.setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
            return addNode(decorated);
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            DecoratedJointNode decorated = new DecoratedJointNode(getParent(), node
                    .getSegmentPath());
            decorated.setPhantom(false);
            decorated.setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
            boolean result = addNode(decorated);

            // set "jointChildren" flag on first non-phantom parent
            DecoratedPrefetchNode parent = (DecoratedPrefetchNode) decorated.getParent();
            while (parent != null) {
                parent.setJointChildren(true);
                
                if (!parent.isPhantom()) {
                    break;
                }

                parent = (DecoratedPrefetchNode) parent.getParent();
            }

            return result;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            // handle unknown as disjoint...
            return startDisjointPrefetch(node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // pop stack...
            nodeStack.removeLast();
        }

        boolean addNode(DecoratedPrefetchNode node) {

            List rows;
            ObjRelationship relationship;
            ObjEntity entity;

            DecoratedPrefetchNode currentNode = getParent();

            if (currentNode != null) {
                rows = (List) extraResultsByPath.get(node.getPath());
                relationship = (ObjRelationship) currentNode.getEntity().getRelationship(
                        node.getSegmentPath());

                if (relationship == null) {
                    throw new CayenneRuntimeException("No relationship with name '"
                            + node.getSegmentPath()
                            + "' found in entity "
                            + currentNode.getEntity().getName());
                }

                entity = (ObjEntity) relationship.getTargetEntity();
            }
            else {
                relationship = null;
                entity = rootEntity;
                rows = mainResultRows;
            }

            node.setDataRows(rows);
            node.setEntity(entity);
            node.setIncoming(relationship);

            if (currentNode != null) {
                currentNode.addChild(node);
            }

            node.afterInit();

            // push node on stack
            if (nodeStack.isEmpty()) {
                root = node;
            }
            nodeStack.addLast(node);

            return true;
        }

        DecoratedPrefetchNode getParent() {
            return (nodeStack.isEmpty()) ? null : (DecoratedPrefetchNode) nodeStack
                    .getLast();
        }
    }

    final class DisjointProcessor implements PrefetchProcessor {

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {

            DecoratedPrefetchNode decoNode = (DecoratedPrefetchNode) node;

            // this means something bad happened during fetch
            if (decoNode.getDataRows() == null) {
                return false;
            }

            // ... continue with processing even if the objects list is empty to handle
            // multi-step prefetches.
            if (decoNode.getDataRows().isEmpty()) {
                return true;
            }

            List objects;

            // disjoint node that is an instance of DecoratedJointNode is a top
            // of a local joint prefetch "cluster"...
            if (decoNode instanceof DecoratedJointNode) {
                JointProcessor subprocessor = new JointProcessor(
                        (DecoratedJointNode) decoNode);
                Iterator it = decoNode.getDataRows().iterator();
                while (it.hasNext()) {
                    subprocessor.setCurrentFlatRow((DataRow) it.next());
                    decoNode.traverse(subprocessor);
                }

                objects = decoNode.getObjects();
            }
            else {
                objects = factory.objectsFromDataRows(decoNode.getEntity(), decoNode
                        .getDataRows());
                decoNode.setObjects(objects);
            }

            // ... continue with processing even if the objects list is empty to handle
            // multi-step prefetches.
            if (objects.isEmpty()) {
                return true;
            }

            // create temporary relationship mapping if needed...
            if (decoNode.isPartitionedByParent()) {

                Class sourceObjectClass = decoNode.getEntity().getJavaClass();
                ObjRelationship reverseRelationship = decoNode
                        .getIncoming()
                        .getReverseRelationship();

                // Might be used later on... obtain and cast only once
                DbRelationship dbRelationship = (DbRelationship) decoNode
                        .getIncoming()
                        .getDbRelationships()
                        .get(0);

                Iterator it = objects.iterator();
                while (it.hasNext()) {
                    DataObject destinationObject = (DataObject) it.next();
                    DataObject sourceObject = null;
                    if (reverseRelationship != null) {
                        sourceObject = (DataObject) destinationObject
                                .readProperty(reverseRelationship.getName());
                    }
                    else {
                        // Reverse relationship doesn't exist... match objects manually
                        DataContext context = destinationObject.getDataContext();
                        ObjectStore objectStore = context.getObjectStore();

                        Map sourcePk = dbRelationship
                                .srcPkSnapshotWithTargetSnapshot(objectStore.getSnapshot(
                                        destinationObject.getObjectId(),
                                        context));

                        // if object does not exist yet, don't create it
                        // the reason for its absense is likely due to the absent
                        // intermediate prefetch
                        sourceObject = objectStore.getObject(new ObjectId(
                                sourceObjectClass,
                                sourcePk));
                    }

                    // don't attach to hollow objects
                    if (sourceObject != null
                            && sourceObject.getPersistenceState() != PersistenceState.HOLLOW) {
                        decoNode.linkToParent(destinationObject, sourceObject);
                    }
                }
            }

            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            // allow joint prefetch nodes to process their children, but skip their own
            // processing.
            return true;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    // a processor of a single joint result set that walks a subtree of prefetch nodes
    // that use this result set.
    final class JointProcessor implements PrefetchProcessor {

        DataRow currentFlatRow;
        DecoratedPrefetchNode rootNode;

        JointProcessor(DecoratedJointNode rootNode) {
            this.rootNode = rootNode;
        }

        void setCurrentFlatRow(DataRow currentFlatRow) {
            this.currentFlatRow = currentFlatRow;
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            // disjoint prefetch that is not the root terminates the walk...
            return node == rootNode ? startJointPrefetch(node) : false;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            DecoratedJointNode decoNode = (DecoratedJointNode) node;

            DataObject object = null;

            // find existing object, if found skip further processing
            Map id = decoNode.idFromFlatRow(currentFlatRow);
            object = decoNode.getResolved(id);

            if (object == null) {

                // TODO: this should be optimized - DataContext.objectsFromDataRows does
                // some batching that we should do once instead of N * M times (e.g.
                // synchronization blocks, etc.)
                DataRow row = decoNode.rowFromFlatRow(currentFlatRow);
                List objects = factory.objectsFromDataRows(
                        decoNode.getEntity(),
                        Collections.singletonList(row));
                object = (DataObject) objects.get(0);

                decoNode.putResolved(id, object);
                decoNode.addObject(object);
            }

            // categorization by parent needed even if an object is already there
            // (many-to-many case)
            if (decoNode.isPartitionedByParent()) {

                DecoratedPrefetchNode parent = (DecoratedPrefetchNode) decoNode
                        .getParent();
                decoNode.linkToParent(object, parent.getLastResolved());
            }

            decoNode.setLastResolved(object);
            return decoNode.isJointChildren();
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            DecoratedPrefetchNode decoNode = (DecoratedPrefetchNode) node;
            return decoNode.isJointChildren();
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    final class PostProcessor implements PrefetchProcessor {

        public void finishPrefetch(PrefetchTreeNode node) {
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            ((DecoratedPrefetchNode) node).connectToParents();
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            ((DecoratedPrefetchNode) node).connectToParents();
            return true;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }
    }
}
