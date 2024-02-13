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
package cz.it4i.ulman.transfers;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.ProjectModel;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.AbstractContextual;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;
import org.mastodon.mamut.launcher.RecentProjectsPanel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

@Plugin( type = MamutPlugin.class )
public class DlBlenderInitProjectMenuItem extends AbstractContextual implements MamutPlugin {
	public static final String KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT = "not mapped";

	// DON'T CHANGE ANYTHING ABOVE
	// -----------------------------------------------------------------------
	// CHANGE THESE (IF YOU WANT):

	//this is how it will appear in the Key Config window; it should be ideally a unique
	//identifier of this plugin; this is not what is visible in the GUI menu
	private static final String PLUGIN_SHORT_NAME = "[displays] download Blender project";
	private static final String PLUGIN_DESCRIPTION = "Opens URL pointing to a reference Blender project file, which is required for some Blender stuff here to work.";

	//menu path, item, and shortcut key
	private static final ViewMenuBuilder.MenuItem PLUGIN_MENU_PATH
			= menu( "Plugins", menu( "Auxiliary Displays", item( PLUGIN_SHORT_NAME ) ) );
	private static final String PLUGIN_MENU_ITEM_NAME = "Download Blender project";

	//provide activation shortcut as a single space separated list of keystrokes,
	//keys are type in uppercase (e.g. 'T'), modifiers are spelled in full name (e.g. 'CTRL')
	//if no shortcut should be assigned, provide KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT
	private static final String[] PLUGIN_ACTIVATION_KEYS = { KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT };

	//provide context where the above activation should be available
	private static final String[] PLUGIN_ACTIVATION_CONTEXTS = { KeyConfigContexts.MASTODON,
			KeyConfigContexts.BIGDATAVIEWER, KeyConfigContexts.TRACKSCHEME };
			//more options: KeyConfigContexts.TABLE, KeyConfigContexts.GRAPHER

	public void run() {
		System.out.println("Downloading two files: display_server_addon.zip and display_server_project.blend");
		System.out.println("Install the former into your Blender, then open the later therein.");
		RecentProjectsPanel.openUrl("https://github.com/xulman/graphics-net-transfers/raw/master/display_server_initial_Blender_project/display_server_addon.zip");
		RecentProjectsPanel.openUrl("https://github.com/xulman/graphics-net-transfers/raw/master/display_server_initial_Blender_project/display_server_project.blend");
	}
	// -----------------------------------------------------------------------
	// DON'T CHANGE ANYTHING BELOW

	// ------------- menu stuff -------------
	private static final Map< String, String > menuText = new HashMap<>();
	static {
		menuText.put( PLUGIN_SHORT_NAME, PLUGIN_MENU_ITEM_NAME );
	}
	@Override
	public Map< String, String > getMenuTexts()
	{ return menuText; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{ return Collections.singletonList( PLUGIN_MENU_PATH ); }

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.MAMUT, PLUGIN_ACTIVATION_CONTEXTS );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions ) {
			descriptions.add( PLUGIN_SHORT_NAME, PLUGIN_ACTIVATION_KEYS, PLUGIN_DESCRIPTION );
		}
	}

	// ------------- action stuff -------------
	private final AbstractNamedAction actionOfThisPlugin = new RunnableAction( PLUGIN_SHORT_NAME, this::run );

	@Override
	public void setAppPluginModel( final ProjectModel projectModel ) {
	}

	@Override
	public void installGlobalActions( final Actions actions ) {
		actions.namedAction( actionOfThisPlugin, PLUGIN_ACTIVATION_KEYS );
	}
}
