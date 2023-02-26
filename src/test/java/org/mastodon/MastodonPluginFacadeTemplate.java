package org.mastodon;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;
import org.scijava.AbstractContextual;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

@Plugin( type = MastodonPluginFacadeTemplate.class )
public class MastodonPluginFacadeTemplate extends AbstractContextual implements MamutPlugin {
	public static final String KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT = "not mapped";

	// DON'T CHANGE ANYTHING ABOVE
	// -----------------------------------------------------------------------
	// CHANGE THESE (IF YOU WANT):

	//this is how it will appear in the Key Config window
	private static final String PLUGIN_SHORT_NAME = "[group] test short name";
	private static final String PLUGIN_DESCRIPTION = "some description";

	//menu path, item, and shortcut key
	private static final ViewMenuBuilder.MenuItem PLUGIN_MENU_PATH
			= menu( "Plugins", /* menu( "submenu", */ item( PLUGIN_SHORT_NAME ) /* ) */ );
	private static final String PLUGIN_MENU_ITEM_NAME = "Test plugin";

	//provide activation shortcut as a single space separated list of keystrokes,
	//keys are type in uppercase (e.g. 'T'), modifiers are spelled in full name (e.g. 'ctrl')
	//if no shortcut should be assigned, provide KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT
	private static final String[] PLUGIN_ACTIVATION_KEYS = { KEYWORD_FOR_NO_SHORTCUT_ASSIGNMENT };

	public void run() {
		System.out.println("this code will happen when this plugin is triggered");
		//example on how to reach the Mastodon's internal data, e.g. project path here
		System.out.println("project folder: "
				+ pluginAppModel.getWindowManager().getProjectManager().getProject().getProjectRoot());
	}
	// -----------------------------------------------------------------------
	// DON'T CHANGE ANYTHING BELOW
	private MamutPluginAppModel pluginAppModel = null;

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

	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.MASTODON );
			//super( KeyConfigContexts.MASTODON, KeyConfigContexts.TRACKSCHEME,
			//       KeyConfigContexts.BIGDATAVIEWER, KeyConfigContexts.TABLE );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( PLUGIN_SHORT_NAME, PLUGIN_ACTIVATION_KEYS, PLUGIN_DESCRIPTION );
		}
	}

	// ------------- action stuff -------------
	private final AbstractNamedAction actionOfThisPlugin = new RunnableAction( PLUGIN_SHORT_NAME, this::run);
	{ actionOfThisPlugin.setEnabled( false ); }

	@Override
	public void setAppPluginModel( final MamutPluginAppModel model ) {
		this.pluginAppModel = model;
		actionOfThisPlugin.setEnabled( model != null && model.getAppModel() != null );
	}

	@Override
	public void installGlobalActions( final Actions actions ) {
		actions.namedAction( actionOfThisPlugin, PLUGIN_ACTIVATION_KEYS );
	}
}
