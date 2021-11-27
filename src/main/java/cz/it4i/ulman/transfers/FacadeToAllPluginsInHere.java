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
import net.imagej.ImageJ;

@Plugin( type = FacadeToAllPluginsInHere.class )
public class FacadeToAllPluginsInHere extends AbstractContextual implements MamutPlugin
{
	//"IDs" of all plug-ins wrapped in this class
	private static final String SV_OPEN = "[displays] sciview and SimViewer";
	private static final String LINEAGE_EXPORTS = "[displays] lineage exports";
	private static final String LINEAGE_EXPORTS_NQ = "[displays] lineage exports w/o dialog";

	private static final String[] SV_OPEN_KEYS = { "not mapped" };
	private static final String[] LINEAGE_EXPORTS_KEYS = { "not mapped" };
	private static final String[] LINEAGE_EXPORTS_NQ_KEYS = { "ctrl D" };
	//------------------------------------------------------------------------


	private final static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put(SV_OPEN,            "Connect to SimViewer");
		menuTexts.put(LINEAGE_EXPORTS,    "Lineage Exports");
		menuTexts.put(LINEAGE_EXPORTS_NQ, "Lineage Exports - Quick Repeat");
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList( menu( "Plugins",
			menu( "External Displays",
				item(SV_OPEN),
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
			descriptions.add(SV_OPEN, SV_OPEN_KEYS, "");
			descriptions.add(LINEAGE_EXPORTS, LINEAGE_EXPORTS_KEYS, "");
			descriptions.add(LINEAGE_EXPORTS_NQ, LINEAGE_EXPORTS_NQ_KEYS, "");
		}
	}
	//------------------------------------------------------------------------


	private final AbstractNamedAction actionOpen;
	private final AbstractNamedAction actionLineageExport;
	private final AbstractNamedAction actionLineageExport_NQ;

	private MamutPluginAppModel pluginAppModel;

	public FacadeToAllPluginsInHere()
	{
		actionOpen             = new RunnableAction( SV_OPEN,            this::simviewerConnection );
		actionLineageExport    = new RunnableAction( LINEAGE_EXPORTS,    this::exportFullLineage );
		actionLineageExport_NQ = new RunnableAction( LINEAGE_EXPORTS_NQ, this::exportFullLineageFast );
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
		actions.namedAction(actionOpen,             SV_OPEN_KEYS );
		actions.namedAction(actionLineageExport,    LINEAGE_EXPORTS_KEYS );
		actions.namedAction(actionLineageExport_NQ, LINEAGE_EXPORTS_NQ_KEYS );
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		actionOpen.setEnabled( appModel != null );
		actionLineageExport.setEnabled( appModel != null );
		actionLineageExport_NQ.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void simviewerConnection()
	{
		this.getContext().getService(CommandService.class).run(
			LineageToSimViewer.class, true,
			"pluginAppModel", pluginAppModel);
	}

	private void exportFullLineage()
	{
		this.getContext().getService(CommandService.class).run(
			LineageExporter.class, true,
			"appModel", pluginAppModel.getAppModel(),
				"projectID", pluginAppModel.getWindowManager().getProjectManager()
						.getProject().getProjectRoot().getName());
	}

	private void exportFullLineageFast()
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
	//------------------------------------------------------------------------

	public static void main( final String[] args ) throws Exception
	{
		//only start up our own Fiji/Imagej2
		final ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();
	}
}
