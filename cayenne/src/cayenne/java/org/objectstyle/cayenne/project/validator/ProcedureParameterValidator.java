package org.objectstyle.cayenne.project.validator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.ProcedureParameter;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * Validator for stored procedure parameters.
 * 
 * @author Andrei Adamchik
 */
public class ProcedureParameterValidator extends TreeNodeValidator {
    private static Logger logObj = Logger.getLogger(ProcedureParameterValidator.class);

    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        ProcedureParameter parameter = (ProcedureParameter) treeNodePath.getObject();

        // Must have name
        if (Util.isEmptyString(parameter.getName())) {
            validator.registerError("Unnamed ProcedureParameter.", treeNodePath);
        }

        // all attributes must have type
        if (parameter.getType() == TypesMapping.NOT_DEFINED) {
            validator.registerWarning("ProcedureParameter has no type.", treeNodePath);
        }

        // VARCHAR and CHAR attributes must have max length
        if (parameter.getMaxLength() < 0
            && (parameter.getType() == java.sql.Types.VARCHAR
                || parameter.getType() == java.sql.Types.CHAR)) {

            validator.registerWarning(
                "Character procedure parameter doesn't have max length.",
                treeNodePath);
        }

        // all attributes must have type
        if (parameter.getDirection() <= 0) {
            validator.registerWarning(
                "ProcedureParameter has no direction.",
                treeNodePath);
        }

    }
}