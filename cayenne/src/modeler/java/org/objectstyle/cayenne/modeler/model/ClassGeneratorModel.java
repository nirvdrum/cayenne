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
	protected String superClassPackage;

	public ClassGeneratorModel(
		DataMap map,
		ObjEntity selectedEntity,
		List validationInfo) {
		this.map = map;
		prepareEntities(selectedEntity, validationInfo);
	}
	
	/**
	 * Initializes superClassPackage to a default reasonable 
	 * superclass package value.
	 */
	public void updateDefaultSuperClassPackage() {
		Iterator it = entities.iterator();
		while(it.hasNext()) {
			ClassGeneratorEntityWrapper entityWrapper = (ClassGeneratorEntityWrapper)it.next();
			String className = entityWrapper.getEntity().getClassName();
			if(className == null) {
				continue;
			}
			
			int dot = className.lastIndexOf('.');
			if(dot > 0) {
				superClassPackage = className.substring(0, dot + 1) + "auto";
				break;
			}
		}
		
		if(superClassPackage == null) {
			superClassPackage = "auto"; 
		}
	}

	protected void prepareEntities(
		ObjEntity selectedEntity,
		List validationInfo) {
		Map failedEntities = new HashMap();

		if (validationInfo != null) {
			Iterator vit = validationInfo.iterator();
			while (vit.hasNext()) {
				ValidationInfo info = (ValidationInfo) vit.next();
				ObjEntity ent =
					(ObjEntity) info.getPath().firstInstanceOf(ObjEntity.class);
				if (ent != null) {
					failedEntities.put(ent.getName(), info.getMessage());
				}
			}
		}

		List tmp = new ArrayList();
		Iterator it = map.getObjEntities().iterator();
		while (it.hasNext()) {
			ObjEntity ent = (ObjEntity) it.next();

			// check if entity didn't pass the validation
			ClassGeneratorEntityWrapper wrapper = null;
			String errorMessage = (String) failedEntities.get(ent.getName());
			if (errorMessage != null) {
				wrapper =
					new ClassGeneratorEntityWrapper(ent, false, errorMessage);
			} else {
				boolean enabled =
					(selectedEntity != null) ? selectedEntity == ent : true;
				wrapper = new ClassGeneratorEntityWrapper(ent, enabled);
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
		fireModelChange(VALUE_CHANGED, Selector.fromString("pairs"));
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

	public String getSuperClassPackage() {
		return superClassPackage;
	}

	public void setSuperClassPackage(String superClassPackage) {
		this.superClassPackage = superClassPackage;
		fireModelChange(
			VALUE_CHANGED,
			Selector.fromString("superClassPackage"));
	}
}
