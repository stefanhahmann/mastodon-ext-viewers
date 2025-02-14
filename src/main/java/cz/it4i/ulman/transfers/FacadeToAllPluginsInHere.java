/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2021, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cz.it4i.ulman.transfers;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import io.scif.gui.DefaultGUIService;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.BdvToBlenderView;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;

@Plugin( type = FacadeToAllPluginsInHere.class )
public class FacadeToAllPluginsInHere extends AbstractContextual implements MamutPlugin
{
	//"IDs" of all plug-ins wrapped in this class
	private static final String BDV_SPOTS_EXPORTS = "[displays] spots-exporting BDV";
	private static final String ALL_SPOTS_EXPORTS = "[displays] all spots exports";
	private static final String LINEAGE_EXPORTS = "[displays] lineage exports";
	private static final String LINEAGE_EXPORTS_NQ = "[displays] lineage exports w/o dialog";

	private static final String[] BDV_SPOTS_EXPORTS_KEYS = { "not mapped" };
	private static final String[] ALL_SPOTS_EXPORTS_KEYS = { "not mapped" };
	private static final String[] LINEAGE_EXPORTS_KEYS = { "not mapped" };
	private static final String[] LINEAGE_EXPORTS_NQ_KEYS = { "ctrl D" };
	//------------------------------------------------------------------------


	private final static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put(BDV_SPOTS_EXPORTS,  "BDV Spots To Blender");
		menuTexts.put(ALL_SPOTS_EXPORTS,  "All Spots To Blender");
		menuTexts.put(LINEAGE_EXPORTS,    "Lineage Exports");
		menuTexts.put(LINEAGE_EXPORTS_NQ, "Lineage Exports - Quick Repeat");
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList( menu( "Plugins",
			menu( "Auxiliary Displays",
				item(BDV_SPOTS_EXPORTS),
				item(ALL_SPOTS_EXPORTS),
				item(LINEAGE_EXPORTS),
				item(LINEAGE_EXPORTS_NQ)
			)
		) );
	}

	/** Command descriptions for all provided commands */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.TRACKSCHEME, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(BDV_SPOTS_EXPORTS, BDV_SPOTS_EXPORTS_KEYS, "BDV-SPOTS: does this show up anywhere?");
			descriptions.add(ALL_SPOTS_EXPORTS, ALL_SPOTS_EXPORTS_KEYS, "");
			descriptions.add(LINEAGE_EXPORTS, LINEAGE_EXPORTS_KEYS, "");
			descriptions.add(LINEAGE_EXPORTS_NQ, LINEAGE_EXPORTS_NQ_KEYS, "");
		}
	}
	//------------------------------------------------------------------------


	private final AbstractNamedAction actionBdvExport;
	private final AbstractNamedAction actionAllExport;
	private final AbstractNamedAction actionLineageExport;
	private final AbstractNamedAction actionLineageExport_NQ;

	private MamutPluginAppModel pluginAppModel;

	public FacadeToAllPluginsInHere()
	{
		actionBdvExport        = new RunnableAction( BDV_SPOTS_EXPORTS,  this::openBdvToBlenderWindow);
		actionAllExport        = new RunnableAction( ALL_SPOTS_EXPORTS,  this::sendAllToBlender);
		actionLineageExport    = new RunnableAction( LINEAGE_EXPORTS,    this::sendTreesToBlender);
		actionLineageExport_NQ = new RunnableAction( LINEAGE_EXPORTS_NQ, this::sendTreesToBlender_fastWithoutAsking);
		updateEnabledActions();
	}

	@Override
	public void setAppPluginModel( final MamutPluginAppModel model )
	{
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction(actionBdvExport,        BDV_SPOTS_EXPORTS_KEYS );
		actions.namedAction(actionAllExport,        ALL_SPOTS_EXPORTS_KEYS );
		actions.namedAction(actionLineageExport,    LINEAGE_EXPORTS_KEYS );
		actions.namedAction(actionLineageExport_NQ, LINEAGE_EXPORTS_NQ_KEYS );
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		actionBdvExport.setEnabled( appModel != null );
		actionAllExport.setEnabled( appModel != null );
		actionLineageExport.setEnabled( appModel != null );
		actionLineageExport_NQ.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void openBdvToBlenderWindow()
	{
		this.getContext().getService(CommandService.class).run(
				BdvToBlenderPlugin.class, true,
				"pluginAppModel", pluginAppModel);
	}

	private void sendAllToBlender()
	{
		this.getContext().getService(CommandService.class).run(
			FullLineageToBlender.class, true,
			"pluginAppModel", pluginAppModel);
	}

	private void sendTreesToBlender()
	{
		this.getContext().getService(CommandService.class).run(
			LineageExporter.class, true,
			"appModel", pluginAppModel.getAppModel(),
				"projectID", pluginAppModel.getWindowManager().getProjectManager()
						.getProject().getProjectRoot().getName());
	}

	private void sendTreesToBlender_fastWithoutAsking()
	{
		//let's create a head-less context (which is, however, completely isolated from the current one!)
		final List<Class<? extends Service>> serviceList = this.getContext().getServiceIndex().stream()
				.filter(s -> !(s instanceof DefaultGUIService))
				//NB: turned out that filtering the one above is enough...
				//.filter(s -> !(s instanceof DisplayService))
				//.filter(s -> !(s instanceof DefaultDisplayService))
				//.filter(s -> !(s instanceof DefaultUIService))
				.map(Service::getClass)
				.collect(Collectors.toList());
		final Context headlessCtx = new Context(serviceList);

		headlessCtx.getService(CommandService.class).run(
			LineageExporter.class, true,
			"appModel", pluginAppModel.getAppModel(),
				"projectID", pluginAppModel.getWindowManager().getProjectManager()
						.getProject().getProjectRoot().getName());
	}
}
