package cayenne.tutorial.tapestry;

import java.io.Serializable;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.Configuration;

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
