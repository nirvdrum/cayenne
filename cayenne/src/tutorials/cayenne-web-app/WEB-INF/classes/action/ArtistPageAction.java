package action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;

import webtest.Artist;

public final class ArtistPageAction extends Action {

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws Exception {

		DataContext ctxt =
			(DataContext) request.getSession().getAttribute("context");

		SelectQuery query = new SelectQuery(Artist.class);
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