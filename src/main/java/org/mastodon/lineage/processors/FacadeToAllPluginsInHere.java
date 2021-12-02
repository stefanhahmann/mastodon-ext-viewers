package org.mastodon.lineage.processors;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;

import org.scijava.AbstractContextual;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;

@Plugin( type = FacadeToAllPluginsInHere.class )
public class FacadeToAllPluginsInHere extends AbstractContextual implements MamutPlugin
{
	private static final String LINEAGE_TIMES = "[exports] lineage lengths";
	private static final String SORT_DSCNDNTS = "[tomancak] sort descendants";
	private static final String LINEAGE_FILTER = "[tomancak] remove solists spots";

	private static final String[] LINEAGE_TIMES_KEYS = { "not mapped" };
	private static final String[] SORT_DSCNDNTS_KEYS = { "not mapped" };
	private static final String[] LINEAGE_FILTER_KEYS = { "not mapped" };
	//------------------------------------------------------------------------


	private final static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put(LINEAGE_TIMES, "Export lineage lengths");
		menuTexts.put(SORT_DSCNDNTS, "Sort descendants");
		menuTexts.put(LINEAGE_FILTER, "Remove Spots Solists");
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList( menu( "Plugins",
			menu( "Exports",
				item(LINEAGE_TIMES)
			),
			menu( "Tomancak lab",
				item(SORT_DSCNDNTS),
				item(LINEAGE_FILTER)
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
			descriptions.add(LINEAGE_TIMES, LINEAGE_TIMES_KEYS, "");
			descriptions.add(SORT_DSCNDNTS, SORT_DSCNDNTS_KEYS, "");
			descriptions.add(LINEAGE_FILTER, LINEAGE_FILTER_KEYS, "");
		}
	}
	//------------------------------------------------------------------------


	private final AbstractNamedAction actionLengths;
	private final AbstractNamedAction actionSorting;
	private final AbstractNamedAction actionSolists;

	private MamutPluginAppModel pluginAppModel;

	public FacadeToAllPluginsInHere()
	{
		actionLengths = new RunnableAction( LINEAGE_TIMES, this::exportLengths );
		actionSorting = new RunnableAction( SORT_DSCNDNTS, this::sortDescendants );
		actionSolists = new RunnableAction( LINEAGE_FILTER, this::filterOutSolists );
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
		actions.namedAction(actionLengths, LINEAGE_TIMES_KEYS );
		actions.namedAction(actionSorting, SORT_DSCNDNTS_KEYS );
		actions.namedAction(actionSolists, LINEAGE_FILTER_KEYS );
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		actionLengths.setEnabled( appModel != null );
		actionSorting.setEnabled( appModel != null );
		actionSolists.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void exportLengths()
	{
		this.getContext().getService(CommandService.class).run(
			LineageLengthExporter.class, true,
			"appModel", pluginAppModel.getAppModel());
	}

	private void sortDescendants()
	{
		this.getContext().getService(CommandService.class).run(
				SortDescendants.class, true,
				"appModel", pluginAppModel.getAppModel(),
				"projectID", pluginAppModel.getWindowManager().getProjectManager()
						.getProject().getProjectRoot().getName());
	}

	private void filterOutSolists()
	{
		this.getContext().getService(CommandService.class).run(
				FilterOutSolists.class, true,
				"appModel", pluginAppModel.getAppModel());
	}
}
