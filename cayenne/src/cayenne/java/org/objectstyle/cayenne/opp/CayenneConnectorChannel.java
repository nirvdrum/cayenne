package org.objectstyle.cayenne.opp;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.client.CayenneClientException;
import org.objectstyle.cayenne.client.ClientEntityResolver;
import org.objectstyle.cayenne.graph.GraphDiff;

/**
 * An OPPChannel adapter that forwards messages via a CayenneConnector.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class CayenneConnectorChannel implements OPPChannel {

    protected OPPConnector connector;

    public CayenneConnectorChannel(OPPConnector connector) {
        this.connector = connector;
    }

    public List onSelectQuery(SelectMessage message) {
        return (List) send(message, List.class);
    }

    public int[] onUpdateQuery(UpdateMessage message) {
        return (int[]) send(message, int[].class);
    }

    public QueryResponse onGenericQuery(GenericQueryMessage message) {
        return (QueryResponse) send(message, QueryResponse.class);
    }

    public GraphDiff onCommit(CommitMessage message) {
        return (GraphDiff) send(message, GraphDiff.class);
    }

    public ClientEntityResolver onBootstrap(BootstrapMessage message) {
        return (ClientEntityResolver) send(message, ClientEntityResolver.class);
    }

    /**
     * Sends a message via connector, getting a result as an instance of a specific class.
     * 
     * @throws org.objectstyle.cayenne.client.CayenneClientException if an underlying
     *             connector exception occured, or a result is not of expected type.
     */
    protected Object send(OPPMessage message, Class resultClass) {
        Object result = connector.sendMessage(message);

        if (result != null && !resultClass.isInstance(result)) {
            String resultString = new ToStringBuilder(result).toString();
            throw new CayenneClientException("Expected result type: "
                    + resultClass.getName()
                    + ", actual: "
                    + resultString);
        }

        return result;
    }
}
