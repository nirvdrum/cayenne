package action;

import java.util.*;

import javax.servlet.http.*;

import org.apache.log4j.*;
import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.query.*;

public final class ArtistPageAction extends Action {

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws Exception {

		DataContext ctxt =
			(DataContext) request.getSession().getAttribute("context");

		SelectQuery query = new SelectQuery("Artist");
		Ordering ordering = new Ordering("artistName", Ordering.ASC);
		query.addOrdering(ordering);

		// set a relatively high logging level, 
		// to show the query execution progress
		query.setLoggingLevel(Level.WARN);
		
		List artists = ctxt.performQuery(query);

		System.out.println("artists: " + artists);
		request.setAttribute("artists", artists);

		return mapping.findForward("success");
	}
}