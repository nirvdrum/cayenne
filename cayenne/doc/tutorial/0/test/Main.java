package test;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import java.util.List;

public class Main {

    private DataContext ctxt;

    /** 
     * Runs tutorial.
     * Usage:
     *     java test.Main galleryPattern
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage:");
            System.err.println("    java test.Main galleryPattern");
            System.exit(1);
        }

        Main tutorialObj = new Main();
        tutorialObj.runTutorial(args[0]);
    }

    public Main() {
        this.ctxt = createContext();
    }

    public void runTutorial(String galleryPattern) {        
        Gallery gallery = findGallery(galleryPattern);
        if (gallery != null) {
            addArtist(gallery);
        }
    }

    /** Creates and returns DataContext object. */
    private DataContext createContext() {
        Configuration.bootstrapSharedConfig(this.getClass());
        DataDomain sharedDomain = Configuration.getSharedConfig().getDomain();
        return sharedDomain.createDataContext();
    }

    /** 
     * Searches for matching galleries in the database. 
     * If one and only one matching gallery is found, it is returned, 
     * otherwise null is returned.
     */
    private Gallery findGallery(String galleryPattern) {
        String likePattern = "%" + galleryPattern + "%";
        Expression qual =
            ExpressionFactory.binaryPathExp(
                Expression.LIKE_IGNORE_CASE,
                "galleryName",
                likePattern);

        SelectQuery query = new SelectQuery("Gallery", qual);
        List galleries = ctxt.performQuery(query);
        if (galleries.size() == 1) {
            Gallery gallery = (Gallery) galleries.get(0);
            System.out.println("Found gallery '" + gallery.getGalleryName() + "'.");
            return gallery;
        }
        else if (galleries.size() == 0) {
            System.out.println("No matching galleries found.");
			return null;            
        }
        else {
            System.out.println("Found more than one matching gallery. Be more specific.");
            return null;
        }
    }

    /** Adds new artist and his paintings to the gallery. */
    private void addArtist(Gallery gallery) {
       // create new Artist object
       Artist dali = (Artist)ctxt.createAndRegisterNewObject("Artist");
       dali.setArtistName("Salvador Dali");
       
       // create new Painting object
       Painting paint = (Painting)ctxt.createAndRegisterNewObject("Painting");
       paint.setPaintingTitle("Sleep");
       
       // establish relationship between artist and painting
       dali.addToPaintingArray(paint);
       
       // commit to the database
       ctxt.commitChanges(); 
    }
}