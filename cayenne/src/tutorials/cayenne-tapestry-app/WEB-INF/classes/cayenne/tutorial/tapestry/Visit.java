package cayenne.tutorial.tapestry;

import java.io.*;

import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.conf.*;

/**
 * The artist application session object.  Each user
 * visit has its own cayenne data context.
 * 
 * @author Eric Schneider
 */

public class Visit implements Serializable {

	private DataContext dataContext;

	public Visit() {
		super();

		dataContext =
			Configuration.getSharedConfig().getDomain().createDataContext();
	}

	public DataContext getDataContext() {
		return dataContext;
	}
}
