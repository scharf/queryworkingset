/*******************************************************************************
 * Copyright (c) 2008 Scott Stanchfield
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package gr.scharf.workingsets;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;

/**
 * Superclass for common functionality on both workingset property pages.
 * @author Scott Stanchfield
 */
public abstract class DynamicWorkingSetPage extends WizardPage implements IWorkingSetPage {
	private IWorkingSet fWorkingSet;
	private Text fWorkingSetLabelText;
	private String fWorkingSetId;

	public DynamicWorkingSetPage(String workingSetId, String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		fWorkingSetId = workingSetId;
	}

	public IWorkingSet getSelection() { return fWorkingSet; }

	// hook methods for subclasses to override
	/**
	 * Hook method (overridden by subclasses) that allows specific field initialization
	 * 	for each work set page.
	 * @param workingSet the working set containing existing data to populate fields
	 */
	protected abstract void initFields(IWorkingSet workingSet);

	/**
	 * Get a list of all items in the workspace that match the filters specified
	 * 	to define the current working set
	 * @return item list
	 */
	protected abstract List<IAdaptable> getMatchingItems();

	/**
	 * Get the display name of this working set
	 * @return the display name
	 */
	protected abstract String getWorkingSetName();

	/**
	 * Validate the current field values
	 * @return true if all fields are valid; false otherwise
	 */
	protected abstract boolean validate();

	/**
	 * Create the controls to display on the property page
	 * @param parent the composite into which the controls go
	 */
	protected abstract void createFields(Composite parent);

	public void setSelection(IWorkingSet workingSet) {
		Assert.isNotNull(workingSet, "Working set must not be null");
		fWorkingSet = workingSet;
		// if the UI has already been set up, populate the fields
		if (getContainer() != null && getShell() != null && fWorkingSetLabelText != null) {
			fWorkingSetLabelText.setText(workingSet.getLabel());
			initFields(workingSet);
		}
	}

	/**
	 * The user hit ok; update the working set with the new settings
	 */
	public void finish() {
		List<IAdaptable> items = getMatchingItems();

		// if this is a new working set, create it and fill in the details
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet= workingSetManager.createWorkingSet(getWorkingSetName(), items.toArray(new IAdaptable[items.size()]));
			fWorkingSet.setId(fWorkingSetId);

		// if this is a working set update, create it and fill in the details
		} else {
			fWorkingSet.setName(getWorkingSetName());
			fWorkingSet.setElements(items.toArray(new IAdaptable[items.size()]));
		}

		// update the display text
		fWorkingSet.setLabel(fWorkingSetLabelText.getText());
	}

	/**
	 * Called whenever the data on the property page is changed
	 */
	protected void dialogChanged() {
		if ("".equals(fWorkingSetLabelText.getText().trim())) {
			updateStatus("Label must be specified");
			return;
		}
		if (!validate()) {
			return;
		}

		updateStatus(null);
	}


	/**
	 * Set the error status message
	 * @param message the error to display, or null for no error
	 */
	protected void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/**
	 * Create the controls on the page
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		Label label = new Label(container, SWT.NULL);
		label.setText("Working Set Label:");

		fWorkingSetLabelText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		fWorkingSetLabelText.setLayoutData(gd);

		// call the subclass' createFields method to customize
		createFields(container);
		if (fWorkingSet != null) {
			fWorkingSetLabelText.setText(fWorkingSet.getLabel());
			// if we're updating an existing working set,
			//   initialize the fields from the existing values
			initFields(fWorkingSet);
		}

		fWorkingSetLabelText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		setControl(container);

		// prime the changes
		dialogChanged();
	}
}
