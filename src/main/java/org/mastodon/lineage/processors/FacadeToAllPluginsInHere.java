/*-
 * #%L
 * Online Mastodon Exports
 * %%
 * Copyright (C) 2021 - 2024 Vladimír Ulman
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.lineage.processors;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.scijava.AbstractContextual;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.mastodon.mamut.plugin.MamutPlugin;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.mastodon.app.ui.ViewMenuBuilder;

import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;

@Plugin( type = MamutPlugin.class )
public class FacadeToAllPluginsInHere extends AbstractContextual implements MamutPlugin
{
	private static final String SORT_DSCNDNTS = "[tomancak] sort descendants";

	private static final String[] SORT_DSCNDNTS_KEYS = { "not mapped" };
	//------------------------------------------------------------------------

	private final static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put(SORT_DSCNDNTS, "Sort Descendants");
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList( menu( "Plugins",
			menu( "Trees Management",
				item(SORT_DSCNDNTS)
			)
		) );
	}

	/** Command descriptions for all provided commands */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.MAMUT, KeyConfigContexts.TRACKSCHEME, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(SORT_DSCNDNTS, SORT_DSCNDNTS_KEYS, "");
		}
	}
	//------------------------------------------------------------------------

	private final AbstractNamedAction actionSorting = new RunnableAction( SORT_DSCNDNTS, this::sortDescendants );

	private ProjectModel projectModel;

	@Override
	public void setAppPluginModel( final ProjectModel model )
	{
		this.projectModel = model;
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction(actionSorting, SORT_DSCNDNTS_KEYS );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void sortDescendants()
	{
		this.getContext().getService(CommandService.class).run(
				SortDescendants.class, true,
				"projectModel", projectModel,
				"projectID", projectModel.getProjectName());
	}
}
