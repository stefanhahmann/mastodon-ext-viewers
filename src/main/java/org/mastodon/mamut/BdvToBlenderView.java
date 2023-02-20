package org.mastodon.mamut;

import bdv.viewer.TransformListener;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.spatial.SpatialIndex;

public class BdvToBlenderView {
	final MamutPluginAppModel appModel;
	MamutViewBdv viewBdv = null;

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

		//create a BDV window
		viewBdv = appModel.getWindowManager().createBigDataViewer();
		//
		//create a listener for it (which will _immediately_ collect updates from BDV)
		final BdvViewUpdateListener bdvUpdateListener = new BdvViewUpdateListener(viewBdv);
		//
		//create a thread that would be watching over the listener and would take only
		//the most recent data if no updates came from BDV for a little while
		//(this is _delayed_ handling of the data, skipping over any intermediate changes)
		final BdvViewUpdateBlenderSenderThread blenderSenderThread
				= new BdvViewUpdateBlenderSenderThread(bdvUpdateListener, 500);

		//register the BDV listener and start the thread
		viewBdv.getViewerPanelMamut().renderTransformListeners().add(bdvUpdateListener);
		blenderSenderThread.start();

		viewBdv.onClose(() -> {
			System.out.println("Cleaning up while BDV to Blender window is closing.");
			viewBdv.getViewerPanelMamut().renderTransformListeners().remove(bdvUpdateListener);
			blenderSenderThread.stopTheWatching();
			viewBdv = null;
		});
	}

	class BdvViewUpdateListener implements TransformListener<AffineTransform3D>
	{
		final MamutViewBdv myBdvIamServicing;
		BdvViewUpdateListener(final MamutViewBdv viewBdv) {
			myBdvIamServicing = viewBdv;
		}

		@Override
		public void transformChanged(AffineTransform3D affineTransform3D) {
			timeStampOfLastRequest = System.currentTimeMillis();
			isLastRequestDataValid = true;
			System.out.println("detected new tp and some new transform");
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
						sendBdvSpotsToBlender(dataSource.myBdvIamServicing);
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

	synchronized
	void sendBdvSpotsToBlender(final MamutViewBdv currentBDV) {
		currentBDV.getViewerPanelMamut().state().getViewerTransform(lastSentTransform);
		lastSentTimepoint = currentBDV.getViewerPanelMamut().state().getCurrentTimepoint();

		System.out.println("new tp: "+lastSentTimepoint+", and new transform: "+lastSentTransform);
		/*
		final SpatialIndex<Spot> spots = appModel.getAppModel().getModel().getSpatioTemporalIndex().getSpatialIndex(lastSentTimepoint);
		spots.forEach(s -> {
			System.out.println(s);
		});
		*/
	}
}
