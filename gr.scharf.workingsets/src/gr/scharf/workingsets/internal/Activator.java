/*******************************************************************************
 * Copyright (c) 2013 Michael Scharf
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package gr.scharf.workingsets.internal;

import gr.scharf.workingsets.QueryWorkingSetUpdater;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "gr.scharf.workingsets";

	// The shared instance
	private static Activator fgPlugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		Activator.fgPlugin = this;
		// make sure we load the updater when the bundle is loaded
		Class.forName(QueryWorkingSetUpdater.class.getName());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		Activator.fgPlugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return Activator.fgPlugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
