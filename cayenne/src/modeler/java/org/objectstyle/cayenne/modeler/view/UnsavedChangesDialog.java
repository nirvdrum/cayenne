/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.modeler.view;

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;

/**
 * @author Andrei Adamchik
 */
public class UnsavedChangesDialog {
	private static Logger logObj = Logger.getLogger(UnsavedChangesDialog.class);
	
    private static final String SAVE_AND_CLOSE = "Save Changes";
    private static final String CLOSE_WITHOUT_SAVE = "Ignore Changes";
    private static final String CANCEL = "Cancel";
    
    private static final String[] OPTIONS =
        new String[] { SAVE_AND_CLOSE, CLOSE_WITHOUT_SAVE, CANCEL };

    protected Component parent;
    protected String result = CANCEL;

    public UnsavedChangesDialog(Component parent) {
        this.parent = parent;
    }

    public void show() {
        JOptionPane pane =
            new JOptionPane(
                "You have unsaved changes. Do you want to save them?",
                JOptionPane.QUESTION_MESSAGE);
        pane.setOptions(OPTIONS);

        JDialog dialog = pane.createDialog(parent, ModelerUtil.buildTitle("Unsaved Changes"));
        dialog.show();

        Object selectedValue = pane.getValue();
        // need to do an if..else chain, since
        // sometimes values are unexpected
        if(SAVE_AND_CLOSE.equals(selectedValue)) {
        	result = SAVE_AND_CLOSE;
        }
        else if(CLOSE_WITHOUT_SAVE.equals(selectedValue)){
        	result = CLOSE_WITHOUT_SAVE;
        }
        else {
        	result = CANCEL;
        }
    }

    public boolean shouldSave() {
        return SAVE_AND_CLOSE.equals(result);
    }

    public boolean shouldNotSave() {
        return CLOSE_WITHOUT_SAVE.equals(result);
    }

    public boolean shouldCancel() {
        return result == null || CANCEL.equals(result);
    }
}

