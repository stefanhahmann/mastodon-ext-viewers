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
package org.mastodon.mamut;

import bdv.viewer.TransformListener;
import cz.it4i.ulman.transfers.BlenderSendingUtils;
import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import io.grpc.stub.StreamObserver;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.graph.GraphChangeListener;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.spatial.VertexPositionListener;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.ui.coloring.DefaultGraphColorGenerator;
import org.mastodon.ui.coloring.TagSetGraphColorGenerator;

public class BdvToBlenderView {
	final ProjectModel projectModel;
	MamutViewBdv viewBdv = null;
	BlenderSendingUtils.BlenderConnectionHandle conn = null;
	BucketsWithGraphics.BatchOfGraphics.Builder spotsMsgBuilder = null;

	public BdvToBlenderView(final ProjectModel projectModel)
	{
		this.projectModel = projectModel;
	}

	public void openUseAutoCleanBdvToBlenderView(final String urlToBlender,
	                                             final String thisMastodonName,
	                                             final String collectionName)
	{
		//avoid opening another view from this instance
		if (viewBdv != null) {
			System.out.println("It looks like you still have an opened BDV connected to Blender, bailing now.");
			return;
		}

		conn = BlenderSendingUtils.connectToBlender(urlToBlender, thisMastodonName);
		conn.sendInitialIntroHandshake();
		spotsMsgBuilder = BucketsWithGraphics.BatchOfGraphics.newBuilder();
		spotsMsgBuilder
				.setClientID( conn.clientIdObj )
				.setCollectionName( collectionName )
				.setDataID(555);

		//create a BDV window
		viewBdv = projectModel.getWindowManager().createView(MamutViewBdv.class);
		//viewBdv = projectModel.getWindowManager().createBigDataViewer();
		spotsMsgBuilder.setDataName( viewBdv.getFrame().getTitle() );
		//
		//create a listener for it (which will _immediately_ collect updates from BDV)
		final BdvViewUpdateListener bdvUpdateListener = new BdvViewUpdateListener(viewBdv);
		//
		//create a thread that would be watching over the listener and would take only
		//the most recent data if no updates came from BDV for a little while
		//(this is _delayed_ handling of the data, skipping over any intermediate changes)
		final BdvViewUpdateBlenderSenderThread blenderSenderThread
				= new BdvViewUpdateBlenderSenderThread(bdvUpdateListener, 60);

		//register the BDV listener and start the thread
		viewBdv.getViewerPanelMamut().renderTransformListeners().add(bdvUpdateListener);
		projectModel.getModel().getGraph().addVertexPositionListener(bdvUpdateListener);
		projectModel.getModel().getGraph().addGraphChangeListener(bdvUpdateListener);
		blenderSenderThread.start();

		viewBdv.onClose(() -> {
			System.out.println("Cleaning up while BDV to Blender window is closing.");
			viewBdv.getViewerPanelMamut().renderTransformListeners().remove(bdvUpdateListener);
			projectModel.getModel().getGraph().removeGraphChangeListener(bdvUpdateListener);
			projectModel.getModel().getGraph().removeVertexPositionListener(bdvUpdateListener);
			viewBdv = null;
			blenderSenderThread.stopTheWatching();
			conn.closeConnection();
			conn = null;
		});
	}

	class BdvViewUpdateListener implements TransformListener<AffineTransform3D>, GraphChangeListener, VertexPositionListener
	{
		final MamutViewBdv myBdvIamServicing;
		BdvViewUpdateListener(final MamutViewBdv viewBdv) {
			myBdvIamServicing = viewBdv;
		}

		@Override
		public void transformChanged(AffineTransform3D affineTransform3D) { somethingChanged(); }
		@Override
		public void graphChanged() { somethingChanged(); }
		@Override
		public void vertexPositionChanged(Object vertex) { somethingChanged(); }

		void somethingChanged() {
			timeStampOfLastRequest = System.currentTimeMillis();
			isLastRequestDataValid = true;
			//System.out.println("detected new tp and some new transform");
		}

		boolean isLastRequestDataValid = false;
		long timeStampOfLastRequest = 0;
	}

	class BdvViewUpdateBlenderSenderThread extends Thread
	{
		final BdvViewUpdateListener dataSource;
		final long updateInterval;
		boolean keepWatching = true;
		BdvViewUpdateBlenderSenderThread(final BdvViewUpdateListener dataSupplier,
		                                 final long updateIntervalInMilis) {
			super("Mastodon BDV updater to Blender");
			dataSource = dataSupplier;
			updateInterval = updateIntervalInMilis;
		}

		void stopTheWatching() {
			keepWatching = false;
		}

		@Override
		public void run() {
			System.out.println("Blender sender service started");
			try {
				while (keepWatching)
				{
					if (dataSource.isLastRequestDataValid
							&& (System.currentTimeMillis() - dataSource.timeStampOfLastRequest > updateInterval))
					{
						System.out.println("silence detected, going to send the current data");
						dataSource.isLastRequestDataValid = false;
						sendBdvSpotsToBlender();
					} else sleep(updateInterval/2);
				}
			}
			catch (InterruptedException e)
			{ /* do nothing, silently stop */ }
			System.out.println("Blender sender service stopped");
		}
	}

	final AffineTransform3D lastSentTransform = new AffineTransform3D();
	int lastSentTimepoint = 0;

	final BucketsWithGraphics.Vector3D.Builder vBuilder
			= BucketsWithGraphics.Vector3D.newBuilder();
	final BucketsWithGraphics.SphereParameters.Builder sBuilder
			= BucketsWithGraphics.SphereParameters.newBuilder();
	final RealPoint spotNewPos = new RealPoint(3);

	private float spotScalingForBlender = 1.0f;
	public BdvToBlenderView setSpheresScalingFactor(final float factor) {
		spotScalingForBlender = factor;
		return this;
	}

	synchronized
	void sendBdvSpotsToBlender()
	{
		viewBdv.getViewerPanelMamut().state().getViewerTransform(lastSentTransform);
		lastSentTimepoint = viewBdv.getViewerPanelMamut().state().getCurrentTimepoint();
		//System.out.println("new tp: "+lastSentTimepoint+", and new transform: "+lastSentTransform);

		sBuilder.setTime(0);
		spotsMsgBuilder.clearSpheres();

		final TagSetStructure.TagSet ts = viewBdv.getColoringModel().getTagSet();
		final GraphColorGenerator<Spot, Link> colorizer
				= ts != null ? new TagSetGraphColorGenerator<>(
						projectModel.getModel().getTagSetModel(), ts)
				: new DefaultGraphColorGenerator<>();

		final SpatialIndex<Spot> spots
				= projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(lastSentTimepoint);
		spots.forEach(s -> {
			lastSentTransform.apply(s, spotNewPos);
			sBuilder.setCentre( vBuilder
					.setX( spotNewPos.getFloatPosition(0) )
					.setY( spotNewPos.getFloatPosition(1) )
					.setZ( spotNewPos.getFloatPosition(2) ) );
			sBuilder.setRadius(spotScalingForBlender * (float)Math.sqrt(s.getBoundingSphereRadiusSquared()));
			sBuilder.setColorXRGB( colorizer.color(s) & 0x00FFFFFF );
			spotsMsgBuilder.addSpheres( sBuilder );
		});

		final StreamObserver<BucketsWithGraphics.BatchOfGraphics> connMsg
				= conn.commContinuous.replaceGraphics(new EmptyIgnoringStreamObservers());
		connMsg.onNext( spotsMsgBuilder.build() );
		connMsg.onCompleted();
		System.out.println("sent "+spotsMsgBuilder.getSpheresCount()+" spots");
	}
}
