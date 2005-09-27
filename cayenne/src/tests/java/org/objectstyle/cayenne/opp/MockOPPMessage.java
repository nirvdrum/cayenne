package org.objectstyle.cayenne.opp;

import org.objectstyle.cayenne.opp.OPPChannel;
import org.objectstyle.cayenne.opp.OPPMessage;

public class MockOPPMessage implements OPPMessage {

    OPPChannel lastChannel;

    public MockOPPMessage() {

    }

    public Object dispatch(OPPChannel channel) {
        this.lastChannel = channel;
        return null;
    }
    
    public OPPChannel getLastChannel() {
        return lastChannel;
    }
} 
