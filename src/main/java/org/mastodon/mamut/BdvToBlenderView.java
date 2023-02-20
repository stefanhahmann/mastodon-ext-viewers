package org.mastodon.mamut;

import bdv.viewer.TransformListener;
import cz.it4i.ulman.transfers.BlenderSendingUtils;
import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import io.grpc.stub.StreamObserver;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.spatial.SpatialIndex;

public class BdvToBlenderView {
	final MamutPluginAppModel appModel;
	MamutViewBdv viewBdv = null;
	BlenderSendingUtils.BlenderConnectionHandle conn = null;
	BucketsWithGraphics.BatchOfGraphics.Builder spotsMsgBuilder = null;

	public BdvToBlenderView(final MamutPluginAppModel pluginAppModel)
	{
		appModel = pluginAppModel;
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
		viewBdv = appModel.getWindowManager().createBigDataViewer();
		spotsMsgBuilder.setDataName( viewBdv.getFrame().getTitle() );
		//
		//create a listener for it (which will _immediately_ collect updates from BDV)
		final BdvViewUpdateListener bdvUpdateListener = new BdvViewUpdateListener(viewBdv);
		//
		//create a thread that would be watching over the listener and would take only
		//the most recent data if no updates came from BDV for a little while
		//(this is _delayed_ handling of the data, skipping over any intermediate changes)
		final BdvViewUpdateBlenderSenderThread blenderSenderThread
				= new BdvViewUpdateBlenderSenderThread(bdvUpdateListener, 100);

		//register the BDV listener and start the thread
		viewBdv.getViewerPanelMamut().renderTransformListeners().add(bdvUpdateListener);
		blenderSenderThread.start();

		viewBdv.onClose(() -> {
			System.out.println("Cleaning up while BDV to Blender window is closing.");
			viewBdv.getViewerPanelMamut().renderTransformListeners().remove(bdvUpdateListener);
			viewBdv = null;
			blenderSenderThread.stopTheWatching();
			conn.closeConnection();
			conn = null;
		});
	}

	class BdvViewUpdateListener implements TransformListener<AffineTransform3D>
	{
		final MamutViewBdv myBdvIamServicing;
		BdvViewUpdateListener(final MamutViewBdv viewBdv) {
			myBdvIamServicing = viewBdv;
		}

		@Override
		public void transformChanged(AffineTransform3D affineTransform3D) { somethingChanged(); }

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

	synchronized
	void sendBdvSpotsToBlender()
	{
		viewBdv.getViewerPanelMamut().state().getViewerTransform(lastSentTransform);
		lastSentTimepoint = viewBdv.getViewerPanelMamut().state().getCurrentTimepoint();
		//System.out.println("new tp: "+lastSentTimepoint+", and new transform: "+lastSentTransform);

		sBuilder.setTime(0);
		sBuilder.setColorIdx(1);
		spotsMsgBuilder.clearSpheres();

		final SpatialIndex<Spot> spots
				= appModel.getAppModel().getModel().getSpatioTemporalIndex().getSpatialIndex(lastSentTimepoint);
		spots.forEach(s -> {
			lastSentTransform.apply(s, spotNewPos);
			sBuilder.setCentre( vBuilder
					.setX( spotNewPos.getFloatPosition(0) )
					.setY( spotNewPos.getFloatPosition(1) )
					.setZ( spotNewPos.getFloatPosition(2) ) );
			sBuilder.setRadius(0.3f * (float)s.getBoundingSphereRadiusSquared());
			spotsMsgBuilder.addSpheres( sBuilder );
		});

		final StreamObserver<BucketsWithGraphics.BatchOfGraphics> connMsg
				= conn.commContinuous.replaceGraphics(new EmptyIgnoringStreamObservers());
		connMsg.onNext( spotsMsgBuilder.build() );
		connMsg.onCompleted();
		System.out.println("sent "+spotsMsgBuilder.getSpheresCount()+" spots");
	}
}
