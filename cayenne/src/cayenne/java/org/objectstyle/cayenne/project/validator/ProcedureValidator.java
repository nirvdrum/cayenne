package org.objectstyle.cayenne.project.validator;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for stored procedures.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureValidator extends TreeNodeValidator {
	private static Logger logObj = Logger.getLogger(ProcedureValidator.class);
	
    public void validateObject(ProjectPath treeNodePath, Validator validator) {
		Procedure procedure = (Procedure) treeNodePath.getObject();
        validateName(procedure, treeNodePath, validator);
    }

    protected void validateName(Procedure procedure, ProjectPath path, Validator validator) {
        String name = procedure.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Procedure.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        Iterator it = map.getProcedures().iterator();
        while (it.hasNext()) {
            Procedure otherProcedure = (Procedure) it.next();
            if (otherProcedure == procedure) {
                continue;
            }

            if (name.equals(otherProcedure.getName())) {
                validator.registerError("Duplicate Procedure name: " + name + ".", path);
                break;
            }
        }
    }

}
