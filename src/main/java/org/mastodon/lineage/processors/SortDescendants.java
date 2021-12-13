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
package org.mastodon.lineage.processors;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.ui.util.SortersChooserDlg;

import org.mastodon.collection.RefList;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.lineage.processors.util.SpotsIterator;
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
			choices = { SortersChooserDlg.M_ALPHANUM,
			            SortersChooserDlg.M_POLES, SortersChooserDlg.M_POLES_IC,
			            SortersChooserDlg.M_SLICES, SortersChooserDlg.M_TRIANGLE })
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

			//first: resolve/harvest the sorter method...
			sorterOfDaughters = SortersChooserDlg.resolveSorterOfDaughters(sortMode,commandService,appModel,projectID);
			if (sorterOfDaughters == null) {
				logServiceRef.info("Dialog canceled or some dialog error, exporting nothing.");
				return;
			}

			//...and prepare the traversing
			final SpotsIterator si = new SpotsIterator(appModel,ownLogger);

			final ModelGraph graph = appModel.getModel().getGraph();
			final RefList<Spot> daughterList = new RefArrayList<>(graph.vertices().getRefPool(),3);
			final Link lRef = graph.edgeRef();
			final Spot sRef = graph.vertices().createRef();

			//second: define spot handler that sorts its descendants
			//NB: this is a single thread sweeping so we can afford re-usable refVariables sRef and lRef
			final Consumer<Spot> handler = spot -> {
				//get direct descendants
				daughterList.clear();
				si.enlistDescendants(spot,daughterList);

				//ownLogger.info("hello from "+spot.getLabel()+" which has "+daughterList.size()+" daughters"); //TODO REMOVE
				if (daughterList.size() < 2) return;

				//get new order
				if (doDebugMessages)
					sorterOfDaughters.sort(daughterList,ownLogger);
				else
					sorterOfDaughters.sort(daughterList);

				//_remove_ and re-insert in the new order,
				//find the links to point on them, to record them and only afterwards to remove them
				final RefList<Link> links = new RefArrayList<>(graph.edges().getRefPool(),daughterList.size()+1);
				for (int n=0; n < spot.incomingEdges().size(); ++n)
				{
					spot.incomingEdges().get(n, lRef).getSource( sRef );
					if (daughterList.contains(sRef)) links.add(lRef);
				}
				for (int n=0; n < spot.outgoingEdges().size(); ++n)
				{
					spot.outgoingEdges().get(n, lRef).getTarget( sRef );
					if (daughterList.contains(sRef)) links.add(lRef);
				}
				for (Link l : links) graph.remove(l);
				//
				//remove and _re-insert_ in the new order
				for (Spot daughter : daughterList) graph.addEdge(spot,daughter).init();
			};

			//third: start the lineage travesal
			si.visitSpots(handler);

			graph.vertices().releaseRef(sRef);
			graph.releaseRef(lRef);

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
