package org.mastodon.lineage.processors.util;

import org.mastodon.collection.RefList;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.mastodon.model.SelectionModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.Link;
import org.scijava.log.Logger;

import java.util.function.Consumer;

/** this class is designed to visit and calculate
 *  user-provided spot handlers in a __single-thread__ fashion
 *  (because it stores and shares some aux variables) */
public class SpotsIterator
{
	// ========= init =========
	final Logger ownLogger;
	final MamutAppModel appModel;
	final ModelGraph modelGraph;
	final SelectionModel<Spot, Link> selectionModel;

	//cache...
	public boolean isSelectionEmpty;

	public SpotsIterator(final MamutAppModel appModel,
	                     final Logger reporter)
	{
		this.ownLogger = reporter;
		this.appModel = appModel;
		this.modelGraph = appModel.getModel().getGraph();
		this.selectionModel = appModel.getSelectionModel();
	}


	// ========= official API =========
	/** finds roots in the current selection if there is one,
	 *  or in the entire lineage otherwise, and, in fact,
	 *  calls visitDownstreamSpots() on the roots */
	public
	void visitSpots(final Consumer<Spot> spotHandler)
	{
		if (appModel.getSelectionModel().isEmpty())
			visitAllSpots(spotHandler);
		else
			visitSelectedSpots(spotHandler);
	}


	/** finds roots in the current selection and
	 *  calls visitDownstreamSpots() on them */
	public
	void visitSelectedSpots(final Consumer<Spot> spotHandler)
	{
		isSelectionEmpty = selectionModel.isEmpty();

		for (Spot spot : selectionModel.getSelectedVertices())
		{
			if (countAncestors(spot) == 0)
			{
				ownLogger.info("Discovered root "+spot.getLabel());
				visitDownstreamSpots(spot,spotHandler);
			}
		}
	}


	/** finds roots in the current whole lineage and
	 *  calls visitDownstreamSpots() on them */
	public
	void visitAllSpots(final Consumer<Spot> spotHandler)
	{
		isSelectionEmpty = true; //pretend there is nothing selected

		final int timeFrom = appModel.getMinTimepoint();
		final int timeTill = appModel.getMaxTimepoint();
		final SpatioTemporalIndex<Spot> spots = appModel.getModel().getSpatioTemporalIndex();

		//over all time points and all spots within each time point
		for (int time = timeFrom; time <= timeTill; ++time)
		for (final Spot spot : spots.getSpatialIndex(time))
		{
			if (countAncestors(spot) == 0)
			{
				ownLogger.info("Discovered root "+spot.getLabel());
				visitDownstreamSpots(spot,spotHandler);
			}
		}
	}


	// ========= internal (but accessible) API =========
	/** traverses future (higher time point) direct descendants
	 *  of the given root spot */
	public
	void visitDownstreamSpots(final Spot spot,
	                          final Consumer<Spot> spotHandler)
	{
		final Link lRef = modelGraph.edgeRef();
		final Spot sRef = modelGraph.vertices().createRef();

		spotHandler.accept(spot);

		final int time = spot.getTimepoint();
		for (int n=0; n < spot.incomingEdges().size(); ++n)
		{
			spot.incomingEdges().get(n, lRef).getSource( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				visitDownstreamSpots(sRef,spotHandler);
			}
		}

		for (int n=0; n < spot.outgoingEdges().size(); ++n)
		{
			spot.outgoingEdges().get(n, lRef).getTarget( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				visitDownstreamSpots(sRef,spotHandler);
			}
		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);
	}


	// ========= internal (but accessible) helpers =========
	public
	int countAncestors(final Spot spot)
	{
		final Link lRef = modelGraph.edgeRef();
		final Spot sRef = modelGraph.vertices().createRef();

		final int time = spot.getTimepoint();
		int cnt = 0;

		for (int n=0; n < spot.incomingEdges().size(); ++n)
		{
			spot.incomingEdges().get(n, lRef).getSource( sRef );
			if (sRef.getTimepoint() < time && isEligible(sRef))
			{
				++cnt;
			}
		}

		for (int n=0; n < spot.outgoingEdges().size(); ++n)
		{
			spot.outgoingEdges().get(n, lRef).getTarget( sRef );
			if (sRef.getTimepoint() < time && isEligible(sRef))
			{
				++cnt;
			}
		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);

		return cnt;
	}


	public
	int countDescendants(final Spot spot)
	{
		final Link lRef = modelGraph.edgeRef();
		final Spot sRef = modelGraph.vertices().createRef();

		final int time = spot.getTimepoint();
		int cnt = 0;

		for (int n=0; n < spot.incomingEdges().size(); ++n)
		{
			spot.incomingEdges().get(n, lRef).getSource( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				++cnt;
			}
		}

		for (int n=0; n < spot.outgoingEdges().size(); ++n)
		{
			spot.outgoingEdges().get(n, lRef).getTarget( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				++cnt;
			}
		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);

		return cnt;
	}


	public
	void enlistDescendants(final Spot spot,                  //input
	                       final RefList<Spot> daughterList) //output
	{
		final Link lRef = modelGraph.edgeRef();
		final Spot sRef = modelGraph.vertices().createRef();

		final int time = spot.getTimepoint();

		for (int n=0; n < spot.incomingEdges().size(); ++n)
		{
			spot.incomingEdges().get(n, lRef).getSource( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				daughterList.add(sRef);
			}
		}

		for (int n=0; n < spot.outgoingEdges().size(); ++n)
		{
			spot.outgoingEdges().get(n, lRef).getTarget( sRef );
			if (sRef.getTimepoint() > time && isEligible(sRef))
			{
				daughterList.add(sRef);
			}
		}

		modelGraph.vertices().releaseRef(sRef);
		modelGraph.releaseRef(lRef);
	}


	public
	boolean isEligible(final Spot s)
	{
		return isSelectionEmpty || selectionModel.isSelected(s);
	}
}
