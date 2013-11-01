/*******************************************************************************
 * Copyright (c) 2013 Michael Scharf
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/package gr.scharf.workingsets;

import gr.scharf.workingsets.internal.RegExResourceFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetUpdater;

/**
 * This class observes the workspace and updates the working sets when the workspace changes.
 * @author Michael Scharf
 *
 */
public class QueryWorkingSetUpdater implements IWorkingSetUpdater {
	/**
	 * Working sets maintained by the updater
	 */
	Map<String, IWorkingSet> workingSets = new HashMap<String, IWorkingSet>();
	/**
	 * Currently active filters. 
	 */
	Collection<RegExResourceFilter> filters = new ArrayList<RegExResourceFilter>();
	
	public QueryWorkingSetUpdater() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					if (event.getDelta() == null) {
						return;
					}
					// make sure do not update the working set for every file that has changed
					beginUpdateWorkingSets();
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta) throws CoreException {
							IResource resource = null;
							switch (delta.getKind()) {
								case IResourceDelta.ADDED:
									// resources are added one by one
									addToWorkingSets(delta.getResource());
									break;
								case IResourceDelta.CHANGED: 
									// handle project opened/closed
									if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
										resource = delta.getResource();
									} else if (".project".equals(delta.getResource().getName())) {
										// properties of the project might have changed
										resource = delta.getResource();
									}
									if (resource != null) {
										updateResourceInWorkingSets(delta.getResource());
										// no need to recurse down
										return false;
									}
									break;
								case IResourceDelta.REMOVED:
									removeFromWorkingSets(delta.getResource());
									break;
							}
							return true;
						}

					});
				} catch (CoreException e) {
					// TODO log the error
					e.printStackTrace();
				} finally {
					endUpdateWorkingSets();
				}
			}
		});

	}
	public void add(IWorkingSet workingSet) {
		workingSets.put(workingSet.getName(), workingSet);
	}

	public boolean contains(IWorkingSet workingSet) {
		return workingSets.values().contains(workingSet);
	}

	public void dispose() {
	}

	public boolean remove(IWorkingSet workingSet) {
		return workingSets.remove(workingSet.getName()) != null;
	}

	/**
	 * @param resource
	 * @throws CoreException
	 */
	protected void updateResourceInWorkingSets(IResource resource) throws CoreException {
		for (RegExResourceFilter filter : filters) {
			filter.updateResource(resource.createProxy());
		}
	}

	protected void removeFromWorkingSets(IResource resource) throws CoreException {
		for (RegExResourceFilter filter : filters) {
			filter.removeResource(resource);
		}
	}

	protected void addToWorkingSets(IResource resource) throws CoreException {
		IResourceProxy proxy = resource.createProxy();
		for (RegExResourceFilter filter : filters) {
			filter.addResource(proxy);
		}
	}

	protected void endUpdateWorkingSets() {
		for (RegExResourceFilter filter : filters) {
			filter.endUpdate();
		}
		// filters are not needed anymore
		filters.clear();
	}

	protected void beginUpdateWorkingSets() {
		filters.clear();
		for (IWorkingSet workingSet : workingSets.values()) {
			RegExResourceFilter filter = new RegExResourceFilter(workingSet);
			filters.add(filter);
		}	
	}

}
