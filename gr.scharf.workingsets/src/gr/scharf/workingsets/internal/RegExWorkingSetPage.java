/*******************************************************************************
 * Copyright (c) 2008 Scott Stanchfield
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package gr.scharf.workingsets.internal;

import gr.scharf.workingsets.DynamicWorkingSetPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * A property page for regular-expression-based dynamic working sets.
 * This page displays a field for the regular expression (with
 * 	content assist, woohoo!) to filter projects for inclusion in the working set
 * @author Michael Scharf
 */
public class RegExWorkingSetPage extends DynamicWorkingSetPage {
	private Text fExpressionText = null;
	private IResource[] fResources=new IResource[0];
	private TableViewer fPreviewTableViewer;
	private ResourceComparator fComparator;
	/**
	 * Do not fill the table before the dialog is shown.
	 * Causes strange effects.
	 */
	private boolean fReady;
	public RegExWorkingSetPage() {
		super("gr.scharf.workingsets.RegExWorkingSetPage",
				"gr.scharf.workingsets.regexWorkingSetPage",
				"Enter project name regular expression",
				Activator.getImageDescriptor("icons/logo16.gif"));
	}

	/**
	 * Create the fields for the property page. This includes a list displaying
	 * 	projects that match the regular expression (b/c regexs are a real pain
	 * 	to get right the first time...)
	 */
	@Override protected void createFields(Composite parent) {
		Label label = new Label(parent, SWT.NULL);
		label.setText("Expressions:");
	    SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
	    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fExpressionText = new Text(sashForm, SWT.BORDER | SWT.MULTI);
		fExpressionText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				filter();
			}
		});

		// This is cool -- I'm using the regular expression content assist from the
		//	find/replace dialog. They made it nicely reusable!
		TextContentAdapter contentAdapter= new TextContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer= new FindReplaceDocumentAdapterContentProposalProvider(true);
		ContentAssistCommandAdapter contentAssistCommandAdapter = new ContentAssistCommandAdapter(
				fExpressionText,
				contentAdapter,
				findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[] {'\\', '[', '('},
				true);
		contentAssistCommandAdapter.setEnabled(true);
	    Composite composite = new Composite(sashForm, SWT.NONE);
	    GridLayout layout = new GridLayout();
	    layout.marginWidth = 0; // <-- HERE
	    composite.setLayout(layout);

		label = new Label(composite, SWT.NULL);
		label.setText("Matching files:");
		createTableViewer(composite);
	    sashForm.setWeights(new int[]{1,4});
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				fReady=true;
				filter();
			}
		});
	}

	/**
	 * Filter the matching projects section of the property page
	 */
	private void filter() {
		if(!fReady)
			return;
		RegExResourceFilter filter = new RegExResourceFilter(fExpressionText.getText(), null);
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(filter, 0);
		} catch (PatternSyntaxException e) {
		} catch (CoreException e) {
		}
		Collection<IAdaptable> filtered=filter.getResult();
		fResources=filtered.toArray(new IResource[filtered.size()]);
		Arrays.sort(fResources, fComparator);
		fPreviewTableViewer.setInput(this.fResources);
		fPreviewTableViewer.setItemCount(this.fResources.length);
		
	}
	@Override protected List<IAdaptable> getMatchingItems() {
		RegExResourceFilter filter = new RegExResourceFilter(fExpressionText.getText(), null);
		try {
			ResourcesPlugin.getWorkspace().getRoot().accept(filter, 0);
		} catch (PatternSyntaxException e) {
		} catch (CoreException e) {
		}
		ArrayList<IAdaptable> result = new ArrayList<IAdaptable>();
		result.addAll(filter.getResult());
		return result;
	}
	@Override protected String getWorkingSetName() {
		return "RegEx: " + fExpressionText.getText();
	}
	@Override protected void initFields(IWorkingSet workingSet) {
		fExpressionText.setText(workingSet.getName().substring(7));
	}
	@Override protected boolean validate() {
		String regex = fExpressionText.getText();
		if ("".equals(regex.trim())) {
			System.out.println("missing regex");
			updateStatus("Regular expression must be specified");
			return false;
		}
		try {
			Pattern.compile(fExpressionText.getText());
		} catch (PatternSyntaxException e) {
			String message = e.getMessage().split("[\n\r]")[0];
			updateStatus("Regular expression syntax: " + message);
			return false;
		}
		return true;
	}
    public void createTableViewer(Composite parent) {
		Table table = new Table(parent, SWT.VIRTUAL| SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		TableLayout layout = new TableLayout();
		table.setLayout(layout);
		table.setHeaderVisible(true);
		this.fPreviewTableViewer = new TableViewer(table);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.minimumHeight=300;

		this.fPreviewTableViewer.getTable().setLayoutData(data);
	    fComparator = new ResourceComparator();
	    createTableViewerColumn("Name", 200, 0);
	    createTableViewerColumn("Path", 400, 1);
		this.fPreviewTableViewer.setLabelProvider(new ITableLabelProvider() {
			public Image getColumnImage(Object element, int columnIndex) {
				if(columnIndex==1)
					return null;
				IWorkbenchAdapter adapter = (IWorkbenchAdapter)((IAdaptable)element).getAdapter(IWorkbenchAdapter.class);
				if (adapter == null) {
					return null;
				}
				ImageDescriptor descriptor = adapter.getImageDescriptor(element);
				if (descriptor == null) {
					return null;
				}
				Image image = (Image) descriptor.createResource(Display.getCurrent());
				return image;
			}

			public String getColumnText(Object element, int columnIndex) {
				IResource resource = (IResource)element;
				if(columnIndex==1)
					return resource.getFullPath().toPortableString();
 				return resource.getName();
			}

			public void addListener(ILabelProviderListener listener) {
			}
			public void dispose() {
			}
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			public void removeListener(ILabelProviderListener listener) {
			}
		});
		
		this.fPreviewTableViewer.setContentProvider(new ILazyContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public void updateElement(int index) {
				fPreviewTableViewer.replace(fResources[index], index);
			}
		});
		
		this.fPreviewTableViewer.setInput(this.fResources);
    }
    private TableViewerColumn createTableViewerColumn(String title, int bound,
    		final int colNumber) {
    	final TableViewerColumn viewerColumn = new TableViewerColumn(fPreviewTableViewer,SWT.NONE);
    	final TableColumn column = viewerColumn.getColumn();
    	column.setText(title);
    	column.setWidth(bound);
    	column.setResizable(true);
    	column.setMoveable(true);
    	column.addSelectionListener(getSelectionAdapter(column, colNumber));
    	return viewerColumn;
    }

    private SelectionAdapter getSelectionAdapter(final TableColumn column,
    		final int index) {
    	SelectionAdapter selectionAdapter = new SelectionAdapter() {
    		@Override
    		public void widgetSelected(SelectionEvent e) {
    			fComparator.setColumn(index);
    			final int direction = fComparator.getDirection();
    			fPreviewTableViewer.getTable().setSortDirection(direction);
    			fPreviewTableViewer.getTable().setSortColumn(column);
    			Arrays.sort(fResources, fComparator);
    			fPreviewTableViewer.refresh();
    		}
    	};
    	return selectionAdapter;
    }
    private class ResourceComparator implements Comparator<IResource> {
    	private int propertyIndex;
    	private static final int DESCENDING = 1;
    	private int direction = DESCENDING;

    	public ResourceComparator() {
    		this.propertyIndex = 0;
    		direction = 0;
    	}

    	public int getDirection() {
    		return direction == DESCENDING ? SWT.DOWN : SWT.UP;
    	}

    	public void setColumn(int column) {
    		if (column == this.propertyIndex) {
    			// Same column as last sort; toggle the direction
    			direction = 1 - direction;
    		} else {
    			// New column; do an ascending sort
    			this.propertyIndex = column;
    			direction = DESCENDING;
    		}
    	}
		@Override
		public int compare(IResource r1, IResource r2) {
			int rc;
    		switch (propertyIndex) {
    		case 0:
    			rc = r1.getName().compareTo(r2.getName());
    			break;
    		case 1:
    			rc = r1.getFullPath().toPortableString().compareTo(r2.getFullPath().toPortableString());
    			break;
    		default:
    			rc = 0;
    		}
    		// If descending order, flip the direction
    		if (direction == DESCENDING) {
    			rc = -rc;
    		}
    		return rc;
		}

    } 
}
