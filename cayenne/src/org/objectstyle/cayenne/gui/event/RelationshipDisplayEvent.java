package org.objectstyle.cayenne.gui.event;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.map.*;

public class RelationshipDisplayEvent extends EntityDisplayEvent
{
	Relationship rel;
	/** True if different from current relationship */
	boolean relationshipChanged = true;

	public RelationshipDisplayEvent (Object src, Relationship temp_rel
	, Entity temp_entity, DataMap data_map, DataDomain temp_domain) 
	{
		super(src, temp_entity, data_map, temp_domain);
		rel = temp_rel;
	}

	public Relationship getRelationship() {
		return rel;
	}
	
	public boolean isRelationshipChanged() {return relationshipChanged;}
	public void setRelationshipChanged(boolean temp) {
		relationshipChanged = temp;
	}
}