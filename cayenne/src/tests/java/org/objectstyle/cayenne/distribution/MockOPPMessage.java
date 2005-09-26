package org.objectstyle.cayenne.distribution;

public class MockOPPMessage implements OPPMessage {

    OPPChannel lastChannel;

    public MockOPPMessage() {

    }

    public Object onReceive(OPPChannel channel) {
        this.lastChannel = channel;
        return null;
    }
    
    public OPPChannel getLastChannel() {
        return lastChannel;
    }
} 
