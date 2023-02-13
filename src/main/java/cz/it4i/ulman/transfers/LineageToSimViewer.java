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
package cz.it4i.ulman.transfers;

import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import cz.it4i.ulman.transfers.graphics.protocol.ClientToServerGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.tomancak.util.SpotsIterator;

import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Plugin( type = Command.class, name = "Display lineage in SimViewer" )
public class LineageToSimViewer extends DynamicCommand {
	@Parameter(persist = false)
	private MamutPluginAppModel pluginAppModel;

	@Parameter
	private String connectURL = "localhost:9083";

	@Parameter(label = "Nickname of this experiment data:")
	private String clientName = "E1";

	@Parameter(label = "Nickname of this displayed (sub)tree:")
	private String dataName = "view1";

	@Parameter(label = "Spheres scale:")
	private float scaleFactor = 0.4f;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		//init the communication side
		try {
			final ManagedChannel channel = ManagedChannelBuilder.forTarget(connectURL).usePlaintext().build();
			final ClientToServerGrpc.ClientToServerStub commContinuous = ClientToServerGrpc.newStub(channel);
			final ClientToServerGrpc.ClientToServerBlockingStub commBlocking = ClientToServerGrpc.newBlockingStub(channel);

			final BucketsWithGraphics.ClientIdentification currentCid = BucketsWithGraphics.ClientIdentification.newBuilder()
					.setClientName(clientName)
					.build();

			//introduction first
			final BucketsWithGraphics.ClientHello hi = BucketsWithGraphics.ClientHello.newBuilder()
					.setClientID( currentCid )
					.setReturnURL( "no feedback" )
					.build();
			commBlocking.introduceClient(hi);

			//now keep pushing data away to the channel
			final StreamObserver<BucketsWithGraphics.BatchOfGraphics> dataSender
					= commContinuous.replaceGraphics(new EmptyIgnoringStreamObservers());

			final SpotsIterator visitor = new SpotsIterator(pluginAppModel.getAppModel(),
					logService.subLogger("export of " + dataName));
			AtomicInteger currentColorID = new AtomicInteger(1);

			visitor.visitRootsFromEntireGraph( root -> {
				final BucketsWithGraphics.BatchOfGraphics.Builder nodeBuilder = BucketsWithGraphics.BatchOfGraphics.newBuilder()
						.setClientID(currentCid)
						.setCollectionName(dataName)
						.setDataName(root.getLabel())
						.setDataID(root.getInternalPoolIndex());

				visitor.visitDownstreamSpots(root, spot -> {
					BucketsWithGraphics.SphereParameters.Builder s = BucketsWithGraphics.SphereParameters.newBuilder();
					s.setCentre( BucketsWithGraphics.Vector3D.newBuilder()
							.setX(spot.getFloatPosition(0))
							.setY(spot.getFloatPosition(1))
							.setZ(spot.getFloatPosition(2))
							.build() );
					s.setTime(spot.getTimepoint());
					s.setRadius(scaleFactor * (float)spot.getBoundingSphereRadiusSquared());
					s.setColorIdx(currentColorID.intValue() % 64);
					//logService.info("adding sphere at: "+s.getTime());
					nodeBuilder.addSpheres(s);
				});
				dataSender.onNext( nodeBuilder.build() );

				currentColorID.addAndGet(5);
			});
			dataSender.onCompleted();

			//first, make sure the channel describe itself as "READY"
			//logger.info("state: "+channel.getState(false).name());
			int wantStillWaitTime = 20;
			final int checkingPeriod = 2;
			while (channel.getState(false) != ConnectivityState.READY && wantStillWaitTime > 0) {
				wantStillWaitTime -= checkingPeriod;
				Thread.sleep(checkingPeriod * 1000); //seconds -> milis
			}
			//but even when it claims "READY", it still needs some grace time to finish any commencing transfers
			Thread.sleep( 5000);
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (StatusRuntimeException e) {
			logService.error("Mastodon network sender: GRPC: " + e.getMessage());
		} catch (Exception e) {
			logService.error("Mastodon network sender: Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}