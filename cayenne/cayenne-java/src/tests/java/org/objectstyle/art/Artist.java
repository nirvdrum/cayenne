package org.objectstyle.art;

import org.objectstyle.art.auto._Artist;
import org.objectstyle.cayenne.validation.ValidationResult;

public class Artist extends _Artist {

    protected boolean validateForSaveCalled;

    public boolean isValidateForSaveCalled() {
        return validateForSaveCalled;
    }

    public void resetValidationFlags() {
        validateForSaveCalled = false;
    }

    public void validateForSave(ValidationResult validationResult) {
        validateForSaveCalled = true;
        super.validateForSave(validationResult);
    }
}
