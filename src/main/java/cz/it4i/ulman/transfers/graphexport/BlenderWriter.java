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
package cz.it4i.ulman.transfers.graphexport;

import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.ClientToServerGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class BlenderWriter extends AbstractGraphExporter implements GraphExportable
{
	// -----------------------------------------------------------------------------
	public float lineRadius = 3.f;
	public float z_coord = 0.f;

	private ClientToServerGrpc.ClientToServerStub commContinuous;
	private ClientToServerGrpc.ClientToServerBlockingStub commBlocking;
	private ManagedChannel channel;
	private String url;
	final LogService logger;

	public BlenderWriter(final String hostAndPort,
	                     final String clientName)
	{
		this(hostAndPort, clientName, new StderrLogService());
	}

	public BlenderWriter(final String hostAndPort,
	                     final String clientName,
	                     LogService logService)
	{
		this(ManagedChannelBuilder.forTarget(hostAndPort).usePlaintext().build(), clientName, logService);
		url = hostAndPort;
	}

	public BlenderWriter(final ManagedChannel someExistingChannel,
	                     final String clientName,
	                     LogService logService)
	{
		logger = logService;

		try {
			channel = someExistingChannel;
			commContinuous = ClientToServerGrpc.newStub(channel);
			commBlocking = ClientToServerGrpc.newBlockingStub(channel);
			isValid = true;

			setClientName(clientName);
			introduceClient();
			mainDataStream = commContinuous.addGraphics(new EmptyIgnoringStreamObservers());
		} catch (StatusRuntimeException e) {
			logger.warn("RPC client-side failed while accessing " + url
					+ ", details follow:\n" + e.getMessage());
		}
	}

	//private String currentSourceName = "Mastodon lineage trees";
	private BucketsWithGraphics.ClientIdentification currentCid;
	public String currentCollectionName = "lineage trees";

	boolean isValid = false;
	boolean isClosed = false;

	@Override
	public void close() {
		// ManagedChannels use resources like threads and TCP connections. To prevent leaking these
		// resources the channel should be shut down when it will no longer be used. If it may be used
		// again leave it running.
		logger.info("connection to Blender is closing...");
		try {
			if (nodeBuilder != null && mainDataStream != null) {
				mainDataStream.onNext( nodeBuilder.build() );
				mainDataStream.onCompleted();
				logger.info("...sent last batch");
			}

			//first, make sure the channel describe itself as "READY"
			//logger.info("state: "+channel.getState(false).name());
			int wantStillWaitTime = 20;
			final int checkingPeriod = 2;
			while (channel.getState(false) != ConnectivityState.READY && wantStillWaitTime > 0) {
				wantStillWaitTime -= checkingPeriod;
				Thread.sleep(checkingPeriod * 1000); //seconds -> milis
			}
			//but even when it claims "READY", it still needs some grace time to finish any commencing transfers;
			//request it to stop whenever it can, then keep asking when it's done
			channel.shutdown();
			wantStillWaitTime = 20;
			while (!channel.isTerminated() && wantStillWaitTime > 0) {
				wantStillWaitTime -= checkingPeriod;
				Thread.sleep(checkingPeriod * 1000); //seconds -> milis
			}
			//last 10secs extra if it is still not closed...
			channel.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			/* don't care that waiting was interrupted */
		} catch (StatusRuntimeException e) {
			logger.error("Mastodon network sender failed for "+url
				+", details follow:\n"+e.getMessage());
		}
		isClosed = true;
	}

	public void finalize()
	{
		// don't forget to close()... and clean up
		if (!isClosed) close();
	}
	// -----------------------------------------------------------------------------

	public void setClientName(final String newClientName)
	{
		currentCid = BucketsWithGraphics.ClientIdentification.newBuilder()
				.setClientName(newClientName)
				.build();
	}

	public void introduceClient()
	{
		final BucketsWithGraphics.ClientHello hi
				= BucketsWithGraphics.ClientHello.newBuilder()
					.setClientID( currentCid )
					.setReturnURL( "no feedback" )
					.build();
		commBlocking.introduceClient(hi);
	}

	StreamObserver<BucketsWithGraphics.BatchOfGraphics> mainDataStream = null;
	BucketsWithGraphics.BatchOfGraphics.Builder nodeBuilder = null;

	public void startSendingGraphics(final String nodeName, final int nodeID)
	{
		if (nodeBuilder != null && mainDataStream != null) {
			mainDataStream.onNext( nodeBuilder.build() );
		}

		//new building
		nodeBuilder = BucketsWithGraphics.BatchOfGraphics.newBuilder()
					.setClientID( currentCid )
					.setCollectionName( currentCollectionName )
					.setDataName( nodeName )
					.setDataID( nodeID );
	}

	public void sendMessage(final String message)
	{
		final BucketsWithGraphics.TextMessage m
				= BucketsWithGraphics.TextMessage.newBuilder()
					.setMsg(message)
					.build();

		BucketsWithGraphics.SignedTextMessage si
				= BucketsWithGraphics.SignedTextMessage.newBuilder()
					.setClientID( currentCid )
					.setClientMessage( m )
					.build();
		commBlocking.showMessage(si);
	}
	// -----------------------------------------------------------------------------

	HashMap<Integer,Float> xs = new HashMap<>(10000);
	HashMap<Integer,Float> ys = new HashMap<>(10000);
	float memorizeAndReturn(int id, float value, final HashMap<Integer,Float> memory) {
		memory.put(id,value);
		return value;
	}

	HashMap<String,Integer> ids = new HashMap<>(10000);
	int nextAvailId = 1;
	int translateID(final String string_id) {
		int int_id = ids.getOrDefault(string_id, -1);
		if (int_id == -1) {
			int_id = nextAvailId;
			nextAvailId++;
			ids.put(string_id, int_id);
		}
		return int_id;
	}
	// -----------------------------------------------------------------------------

	@Override
	public void addNode(String id, String label, int colorRGB, int x, int y) {
		addNode(id,label,colorRGB,x,y,defaultNodeWidth,defaultNodeHeight);
	}

	@Override
	public void addNode(String id, String label, int colorRGB, int x, int y, int width, int height) {
		if (!isValid) return;

		y *= -1;
		final int i = translateID(id);
		memorizeAndReturn(i, x, xs);
		memorizeAndReturn(i, y, ys);

		BucketsWithGraphics.SphereParameters.Builder s = BucketsWithGraphics.SphereParameters.newBuilder();
		s.setCentre( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(x).setY(z_coord).setZ(y).build() );
		s.setTime(0);
		s.setRadius(width);
		s.setColorXRGB(colorRGB);
		//logger.info("adding sphere: "+s);
		nodeBuilder.addSpheres(s);
	}

	@Override
	public void addStraightLine(String fromId, String toId) {
		if (!isValid) return;

		final int fi = translateID(fromId);
		final int ti = translateID(toId);

		BucketsWithGraphics.LineParameters.Builder l = BucketsWithGraphics.LineParameters.newBuilder();
		l.setStartPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(fi)).setY(z_coord).setZ(ys.get(fi)).build() );
		l.setEndPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(ti)).setY(z_coord).setZ(ys.get(ti)).build() );
		l.setTime(0);
		l.setRadius(lineRadius);
		l.setColorIdx(0);
		//logger.info("adding line: "+l);
		nodeBuilder.addLines(l);
	}

	@Override
	public void addStraightLineConnectedVertex(String parentNodeID, String newNodeID, String label, int colorRGB, int x, int y) {
		addNode(newNodeID, label,colorRGB, x,y);
		addStraightLine(parentNodeID, newNodeID);
	}

	@Override
	public void addBendedLine(String fromId, String toId, int toX, int toY) {
		addBendedLine(fromId,toId, toX,toY, defaultBendingPointAbsoluteOffsetY);
	}

	@Override
	public void addBendedLine(String fromId, String toId, int toX, int toY, int bendingOffsetY) {
		if (!isValid) return;

		final int fid = translateID(fromId);
		final int tid = translateID(toId);

		BucketsWithGraphics.LineParameters.Builder l = BucketsWithGraphics.LineParameters.newBuilder();
		l.setStartPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(fid)).setY(z_coord).setZ(ys.get(fid)).build() );
		l.setEndPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(tid)).setY(z_coord).setZ(ys.get(tid)-bendingOffsetY).build() );
		l.setTime(0);
		l.setRadius(lineRadius);
		l.setColorIdx(0);
		nodeBuilder.addLines(l);

		l.setStartPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(tid)).setY(z_coord).setZ(ys.get(tid)-bendingOffsetY).build() );
		l.setEndPos( BucketsWithGraphics.Vector3D.newBuilder()
				.setX(xs.get(tid)).setY(z_coord).setZ(ys.get(tid)).build() );
		nodeBuilder.addLines(l);
	}

	@Override
	public void addBendedLineConnectedVertex(String parentNodeID, String newNodeID, String label, int colorRGB, int x, int y) {
		addNode(newNodeID, label,colorRGB, x,y);
		addBendedLine(parentNodeID, newNodeID, x,y);
	}
}
