package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;

import org.apache.log4j.Level;
import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextStoredProcTst extends CayenneTestCase {
    public static final String UPDATE_STORED_PROCEDURE = "cayenne_tst_upd_proc";
    public static final String SELECT_STORED_PROCEDURE =
        "cayenne_tst_select_proc";

    protected DataContext ctxt;
    
    /**
     * Constructor for DataContextStoredProcTst.
     * @param name
     */
    public DataContextStoredProcTst(String name) {
        super(name);
    }

    public void testUpdate() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }
        
        
        // create an artist with painting in the database  
        createArtist(1000.0);
        
        // create and run stored procedure
        Procedure proc = new Procedure(UPDATE_STORED_PROCEDURE);
        proc.addParam("paintingPrice", Types.INTEGER);
        
        ProcedureQuery q = new ProcedureQuery(Artist.class, proc);
        q.addParam("paintingPrice", new Integer(3000));
        DefaultOperationObserver observer = new DefaultOperationObserver();
        observer.setLoggingLevel(Level.WARN);
        ctxt.performQuery(q, observer);
        
        // check that price have doubled
        SelectQuery select = new SelectQuery(Artist.class);
        select.addPrefetch("paintingArray");
        select.setLoggingLevel(Level.WARN);
        List artists = ctxt.performQuery(select);
        assertEquals(1, artists.size());
        
        Artist a = (Artist)artists.get(0);
        Painting p = (Painting)a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }
    
    
    protected void createArtist(double paintingPrice) {
    	Artist a = (Artist)ctxt.createAndRegisterNewObject("Artist");
    	a.setArtistName("An Artist");
    	
    	Painting p = (Painting)ctxt.createAndRegisterNewObject("Painting");
    	p.setPaintingTitle("A Painting");
    	p.setEstimatedPrice(new BigDecimal(paintingPrice));
    	a.addToPaintingArray(p);
    	
    	ctxt.commitChanges();
    }
    
    
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // Don't run this on MySQL
        if (!getDatabaseSetupDelegate().supportsStoredProcedures()) {
            return;
        }
        
        getDatabaseSetup().cleanTableData();
        ctxt = createDataContext();
    }

}
