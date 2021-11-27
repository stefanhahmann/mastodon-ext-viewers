package org.mastodon.lineage.processors;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.ui.util.SortersChooserDlg;

import org.mastodon.collection.RefList;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.model.SelectionModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.Link;

import org.scijava.log.Logger;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import java.util.concurrent.ExecutionException;


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

			//go!
			selectionModel = appModel.getSelectionModel();
			isSelectionEmpty = selectionModel.isEmpty();
			if (isSelectionEmpty) processAllDaughters();
			else processSelectedDaughters();
		} catch (InterruptedException e) {
			logServiceRef.info("Dialog interrupted, doing nothing.");
		} catch (ExecutionException e) {
			logServiceRef.error("Some error executing the additional dialog: "+e.getMessage());
		}
	}

	boolean isSelectionEmpty;
	SelectionModel<Spot, Link> selectionModel;

	private void processSelectedDaughters()
	{
		final ModelGraph modelGraph = appModel.getModel().getGraph();
		final Link lRef = modelGraph.edgeRef();              //link reference
		final Spot sRef = modelGraph.vertices().createRef(); //aux spot reference

		int xLeftBound = 0;
		final int[] xIgnoreCoords = new int[1];

		for (Spot spot : selectionModel.getSelectedVertices())
		{
			//find how many _selected_ backward-references (time-wise) this spot has
			int countBackwardLinks = 0;

			final int time = spot.getTimepoint();
			for (int n=0; n < spot.incomingEdges().size(); ++n)
			{
				spot.incomingEdges().get(n, lRef).getSource( sRef );
				if (sRef.getTimepoint() < time && selectionModel.isSelected(sRef))
				{
					++countBackwardLinks;
				}
			}
			for (int n=0; n < spot.outgoingEdges().size(); ++n)
			{
				spot.outgoingEdges().get(n, lRef).getTarget( sRef );
				if (sRef.getTimepoint() < time && selectionModel.isSelected(sRef))
				{
					++countBackwardLinks;
				}
			}

			//can this spot be root?
			if (countBackwardLinks == 0)
			{
				ownLogger.info("Discovered root "+spot.getLabel());
			}

		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);

		ownLogger.info("generation SELECTED graph rendered");
		modelGraph.notifyGraphChanged();
	}

	/** implements the "LineageExporter" functionality */
	private void processAllDaughters()
	{
		//NB: this method could be in a class of its own... later...

		//shortcuts to the data
		final int timeFrom = appModel.getMinTimepoint();
		final int timeTill = appModel.getMaxTimepoint();
		final ModelGraph modelGraph = appModel.getModel().getGraph();

		//aux Mastodon data: shortcuts and caches/proxies
		final SpatioTemporalIndex< Spot > spots = appModel.getModel().getSpatioTemporalIndex();
		final Link lRef = modelGraph.edgeRef();              //link reference
		final Spot sRef = modelGraph.vertices().createRef(); //aux spot reference

		int xLeftBound = 0;
		final int[] xIgnoreCoords = new int[1];

		//over all time points
		for (int time = timeFrom; time <= timeTill; ++time)
		{
			//over all spots in the current time point
			for ( final Spot spot : spots.getSpatialIndex( time ) )
			{
				//find how many backward-references (time-wise) this spot has
				int countBackwardLinks = 0;

				for (int n=0; n < spot.incomingEdges().size(); ++n)
				{
					spot.incomingEdges().get(n, lRef).getSource( sRef );
					if (sRef.getTimepoint() < time)
					{
						++countBackwardLinks;
					}
				}
				for (int n=0; n < spot.outgoingEdges().size(); ++n)
				{
					spot.outgoingEdges().get(n, lRef).getTarget( sRef );
					if (sRef.getTimepoint() < time)
					{
						++countBackwardLinks;
					}
				}

				//can this spot be root?
				if (countBackwardLinks == 0)
				{
					ownLogger.info("Discovered root "+spot.getLabel());
				}
			}
		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);

		ownLogger.info("generation graph rendered");
		modelGraph.notifyGraphChanged();
	}

	private boolean isEligible(Spot s)
	{
		return isSelectionEmpty || selectionModel.isSelected(s);
	}

	private DescendantsSorter sorterOfDaughters;

	/** returns width of the tree induced with the given 'root' */
	private int discoverEdge(final GraphExportable ge, final ModelGraph modelGraph,
	                         final Spot root,
	                         final int generation,
	                         final int xLeftBound,
	                         final int[] xCoords, final int xCoordsPos)
	{
		final Spot spot = modelGraph.vertices().createRef(); //aux spot reference
		final Spot fRef = modelGraph.vertices().createRef(); //spot's ancestor buddy (forward)
		final Link lRef = modelGraph.edgeRef();              //link reference
		final Spot tRef = modelGraph.vertices().createRef(); //tmp reference on spot
		final RefList<Spot> daughterList = new RefArrayList<>(modelGraph.vertices().getRefPool(),3);

		spot.refTo( root );
		while (true)
		{
			//shortcut to the time of the current node/spot
			final int time = spot.getTimepoint();

			//find how many forward-references (time-wise) this spot has
			int countForwardLinks = 0;

			for (int n=0; n < spot.incomingEdges().size(); ++n)
			{
				spot.incomingEdges().get(n, lRef).getSource( fRef );
				if (fRef.getTimepoint() > time && isEligible(fRef))
				{
					++countForwardLinks;
					tRef.refTo(fRef); //keep the last used valid reference
				}
			}
			for (int n=0; n < spot.outgoingEdges().size(); ++n)
			{
				spot.outgoingEdges().get(n, lRef).getTarget( fRef );
				if (fRef.getTimepoint() > time && isEligible(fRef))
				{
					++countForwardLinks;
					tRef.refTo(fRef);
				}
			}

			if (countForwardLinks == 1)
			{
				//just a vertex on "a string", move over it
				spot.refTo( tRef );
			}
			else
			{
				int xRightBound = xLeftBound;
				final int[] childrenXcoords = new int[countForwardLinks];

				//we're leaf or branching point
				if (countForwardLinks > 1)
				{
					//branching point -> enumerate all descendants and restart searches from them
					daughterList.clear();
					for (int n=0; n < spot.incomingEdges().size(); ++n)
					{
						spot.incomingEdges().get(n, lRef).getSource( fRef );
						if (fRef.getTimepoint() > time && isEligible(fRef)) daughterList.add(fRef);
					}
					for (int n=0; n < spot.outgoingEdges().size(); ++n)
					{
						spot.outgoingEdges().get(n, lRef).getTarget( fRef );
						if (fRef.getTimepoint() > time && isEligible(fRef)) daughterList.add(fRef);
					}
					if (doDebugMessages) sorterOfDaughters.sort(daughterList,ownLogger);
					else sorterOfDaughters.sort(daughterList);

					//process the daughters in the given order
					int childCnt = 0;
					for (Spot d : daughterList) {
						xRightBound += discoverEdge(ge,modelGraph, d, generation+1,xRightBound, childrenXcoords,childCnt);
						++childCnt;
					}
				}
				else
				{
					//we're a leaf -> pretend a subtree of single column width
					xRightBound += ge.get_xColumnWidth();
				}

				final String rootID = Integer.toString(root.getInternalPoolIndex());
				xCoords[xCoordsPos] = countForwardLinks == 0
				                      ? (xRightBound + xLeftBound)/2
				                      : (childrenXcoords[0] + childrenXcoords[countForwardLinks-1])/2;
				//gsv.graph.addNode(rootID).addAttribute("xyz", new int[] {!,!,0});
				ge.addNode(rootID, root.getLabel(),ge.get_defaultNodeColour(),
				           xCoords[xCoordsPos],ge.get_yLineStep()*generation);

				if (countForwardLinks > 1)
				{
					int childCnt = 0;
					//enumerate all ancestors (children) and connect them (to this parent)
					for (Spot d : daughterList) {
						//edge
						final String toID = Integer.toString(d.getInternalPoolIndex());
						ownLogger.info("generation: "+generation+"   "+rootID+" -> "+toID);
						ge.addBendedLine( rootID, toID,
								childrenXcoords[childCnt++],ge.get_yLineStep()*(generation+1) );
					}
				}
				else
				{
					//leaf is just a vertex node (there's no one to connect to)
					ownLogger.info("Discovered \"leaf\" "+root.getLabel());
				}

				//clean up first before exiting
				modelGraph.vertices().releaseRef(spot);
				modelGraph.vertices().releaseRef(fRef);
				modelGraph.vertices().releaseRef(tRef);
				modelGraph.releaseRef(lRef);

				return (xRightBound - xLeftBound);
			}
		}
	}
}
