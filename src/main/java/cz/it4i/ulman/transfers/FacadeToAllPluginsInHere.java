package cz.it4i.ulman.transfers;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;
import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.MamutAppModel;

import net.imagej.ImageJ;
import org.scijava.AbstractContextual;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;

import javax.swing.*;
import java.util.*;
import java.util.List;


@Plugin( type = FacadeToAllPluginsInHere.class )
public class FacadeToAllPluginsInHere extends AbstractContextual implements MamutPlugin
{
	//"IDs" of all plug-ins wrapped in this class
	private static final String SVopen = "LoPaT-OpenSimViewer";
	private static final String lineageExports = "LoPaT-LineageExports";
	private static final String lineageTimes = "LoPaT-LineageLengths";
	//------------------------------------------------------------------------

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		//this places the plug-in's menu items into the menu,
		//the titles of the items are defined right below
		return Arrays.asList(
				menu( "Plugins",
						item( SVopen ),
						item(lineageExports),
						item(lineageTimes) ) );
	}

	/** titles of this plug-in's menu items */
	private final static Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put(SVopen, "Connect to SimViewer");
		menuTexts.put(lineageExports, "Exports of the lineage");
		menuTexts.put(lineageTimes,  "Export lineage lengths");
	}

	@Override
	public Map< String, String > getMenuTexts()
	{
		return menuTexts;
	}
	//------------------------------------------------------------------------

	private final AbstractNamedAction actionOpen;
	private final AbstractNamedAction actionLengths;
	private final AbstractNamedAction actionLineageExport;

	/** default c'tor: creates Actions available from this plug-in */
	public FacadeToAllPluginsInHere()
	{
		actionOpen    = new RunnableAction(SVopen, this::simviewerConnection );
		actionLengths = new RunnableAction(lineageTimes, this::exportLengths );
		actionLineageExport = new RunnableAction(lineageExports, this::exportFullLineage );
		updateEnabledActions();
	}

	/** register the actions to the application (with no shortcut keys) */
	@Override
	public void installGlobalActions( final Actions actions )
	{
		final String[] noShortCut = { "not mapped" };
		actions.namedAction( actionOpen, noShortCut );
		actions.namedAction( actionLengths, noShortCut );
		actions.namedAction(actionLineageExport, noShortCut );
	}

	/** reference to the currently available project in Mastodon */
	private MamutPluginAppModel pluginAppModel;

	/** learn about the current project's params */
	@Override
	public void setAppPluginModel( final MamutPluginAppModel model )
	{
		//the application reports back to us if some project is available
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		actionOpen.setEnabled( appModel != null );
		actionLengths.setEnabled( appModel != null );
		actionLineageExport.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------

	private void simviewerConnection()
	{
		this.getContext().getService(CommandService.class).run(
			LineageToSimViewer.class, true,
			"pluginAppModel", pluginAppModel);
	}

	private void exportLengths()
	{
		this.getContext().getService(CommandService.class).run(
			LineageLengthExporter.class, true,
			"appModel", pluginAppModel.getAppModel());
	}

	private void exportFullLineage()
	{
		this.getContext().getService(CommandService.class).run(
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
