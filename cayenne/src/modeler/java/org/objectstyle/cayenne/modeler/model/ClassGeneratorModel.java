package org.objectstyle.cayenne.modeler.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.project.validator.ValidationInfo;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorModel extends BasicModel {
    protected DataMap map;
    protected String outputDir;
    protected boolean pairs;
    protected List entities;

    public ClassGeneratorModel(DataMap map, List validationInfo) {
        this.map = map;
        prepareEntities(validationInfo);
    }

    protected void prepareEntities(List validationInfo) {
        Map failedEntities = new HashMap();
        
        if(validationInfo != null) {
        	Iterator vit = validationInfo.iterator();
        	while(vit.hasNext()) {
        		ValidationInfo info = (ValidationInfo)vit.next();
        		ObjEntity ent = (ObjEntity)info.getPath().firstInstanceOf(ObjEntity.class);
        		if(ent != null) {
        		    failedEntities.put(ent.getName(), info.getMessage());
        		}
        	}
        }

        List tmp = new ArrayList();
        Iterator it = map.getObjEntitiesAsList().iterator();
        while (it.hasNext()) {
            ObjEntity ent = (ObjEntity) it.next();

            // check if entity didn't pass the validation
            ClassGeneratorEntityWrapper wrapper = null;
            String errorMessage = (String)failedEntities.get(ent.getName());
            if (errorMessage != null) {
                wrapper = new ClassGeneratorEntityWrapper(ent, false, errorMessage);
            } else {
                wrapper = new ClassGeneratorEntityWrapper(ent, true);
            }

            tmp.add(wrapper);
        }
        entities = tmp;
    }

    public List getSelectedEntities() {
        Iterator it = entities.iterator();
        List selected = new ArrayList();
        while (it.hasNext()) {
            ClassGeneratorEntityWrapper wrapper =
                (ClassGeneratorEntityWrapper) it.next();
            if (wrapper.isSelected()) {
                selected.add(wrapper.getEntity());
            }
        }

        return selected;
    }

    /**
     * Returns the map.
     * @return DataMap
     */
    public DataMap getMap() {
        return map;
    }
    
    /**
     * Returns the outputDir.
     * @return File
     */
    public File getOutputDirectory() {
        return (outputDir != null) ? new File(outputDir) : null;
    }

    /**
     * Returns the pairs.
     * @return boolean
     */
    public boolean isPairs() {
        return pairs;
    }

    /**
     * Sets the pairs.
     * @param pairs The pairs to set
     */
    public void setPairs(boolean pairs) {
        this.pairs = pairs;
    }

    /**
     * Returns the entities.
     * @return List
     */
    public List getEntities() {
        return entities;
    }
    /**
     * Returns the outputDir.
     * @return String
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Sets the outputDir.
     * @param outputDir The outputDir to set
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
        fireModelChange(VALUE_CHANGED, Selector.fromString("outputDir"));
    }
}
