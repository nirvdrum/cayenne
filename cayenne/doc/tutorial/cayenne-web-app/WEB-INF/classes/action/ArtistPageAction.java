package action;

import java.util.List;
import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

import formbean.*;

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

		// using log level of WARN to show the query execution
		query.setLogLevel(Level.WARN);
		List artists = ctxt.performQuery(query);

		System.out.println("artists: " + artists);
		request.setAttribute("artists", artists);

		return (mapping.findForward("success"));

	}

}