package org.objectstyle.cayenne.opp;

import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.opp.OPPMessage;

public class MockOPPMessage implements OPPMessage {

    DataChannel lastChannel;

    public MockOPPMessage() {

    }

    public Object dispatch(DataChannel channel) {
        this.lastChannel = channel;
        return null;
    }
    
    public DataChannel getLastChannel() {
        return lastChannel;
    }
} 
