package org.objectstyle.cayenne.gui.datamap;

/** Callback for panes of detail views to process existing selections.
  * When tab is selected processExistingSelection() is called
  * to reset the state if the tab has any rows selected.
  * For example, this is useful when we want to reset the state of the 
  * "Remove" button if the tab has attributes (relationships) already 
  * selected. */
interface ExistingSelectionProcessor {
 /** Called when tab is selected. Resets the state there are any rows selected.
  *  For example, it is useful when we want to reset "Remove" button 
  *  if the tab has attributes (relationships) already selected. */
  public void processExistingSelection();	
}
