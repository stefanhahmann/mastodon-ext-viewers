package org.mastodon.lineage.processors;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.ui.util.SortersChooserDlg;

import org.mastodon.collection.RefList;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.lineage.processors.util.SpotsIterator;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;

import org.scijava.log.Logger;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;


@Plugin( type = Command.class, name = "Sort daughters in the lineage" )
public class SortDescendants implements Command
{
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...also of only selected sub-trees.";

	@Parameter(persist = false)
	private MamutAppModel appModel;
	//
	//optional ID to distinguish among Mastodon projects
	@Parameter(persist = false, required = false)
	private String projectID = "default";

	@Parameter(label = "How to sort the lineage:",
			choices = { SortersChooserDlg.M_TRACKSCHEME, SortersChooserDlg.M_ALPHANUM,
			            SortersChooserDlg.M_POLES, SortersChooserDlg.M_POLES_IC,
			            SortersChooserDlg.M_TRIANGLE })
	public String sortMode;

	@Parameter
	private boolean doDebugMessages = false;

	@Parameter
	private LogService logServiceRef;
	private Logger ownLogger;

	@Parameter
	private CommandService commandService;

	private DescendantsSorter sorterOfDaughters;


	@Override
	public void run()
	{
		try {
			ownLogger = logServiceRef.subLogger("Lineage exports in "+projectID);

			//first: do we have some extra dialogs to take care of?
			sorterOfDaughters = SortersChooserDlg.resolveSorterOfDaughters(sortMode,commandService,appModel,projectID);
			if (sorterOfDaughters == null) {
				logServiceRef.info("Dialog canceled or some dialog error, exporting nothing.");
				return;
			}
			//
			//also prepare the traversing code
			final SpotsIterator si = new SpotsIterator(appModel,ownLogger);

			final RefList<Spot> daughterList = new RefArrayList<>(appModel.getModel().getGraph().vertices().getRefPool(),3);

			//second: define spot handler that sorts its descendants
			final Consumer<Spot> handler = spot -> {
				ownLogger.info("hello from "+spot.getLabel()); //TODO REMOVE

				//get direct descendants
				daughterList.clear();
				si.enlistDescendants(spot,daughterList);

				//get new order
				if (doDebugMessages)
					sorterOfDaughters.sort(daughterList,ownLogger);
				else
					sorterOfDaughters.sort(daughterList);

				//remove and re-insert in the new order
			};

			//third: start the lineage travesal
			si.visitSpots(handler);

			ownLogger.info("processing "
					+ (si.isSelectionEmpty ? "full" : "selected")
					+ " graph done");
			appModel.getModel().getGraph().notifyGraphChanged();

		} catch (InterruptedException e) {
			logServiceRef.info("Dialog interrupted, doing nothing.");
		} catch (ExecutionException e) {
			logServiceRef.error("Some error executing the additional dialog: "+e.getMessage());
		}
	}
}
