package action;

import java.util.List;
import org.apache.log4j.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.*;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.SelectQuery;

public final class GalleryPageAction extends Action {

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws Exception {

		DataContext ctxt =
			(DataContext) request.getSession().getAttribute("context");

		SelectQuery query = new SelectQuery("Gallery");

		// using log level of WARN to show the query execution
		query.setLogLevel(Level.WARN);

		List galleries = ctxt.performQuery(query);
		request.setAttribute("galleries", galleries);

		return (mapping.findForward("success"));

	}

}
