package cz.it4i.ulman.transfers.graphexport;

import cz.it4i.ulman.transfers.graphics.protocol.PointsAndLinesGrpc;
import cz.it4i.ulman.transfers.graphics.protocol.PointsAndLinesOuterClass;
import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
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

	private StreamObserver<PointsAndLinesOuterClass.PointAsBall> nodes;
	private StreamObserver<PointsAndLinesOuterClass.LineWithIDs> lines;
	private StreamObserver<PointsAndLinesOuterClass.LineWithPositions> linePs;
	private PointsAndLinesGrpc.PointsAndLinesStub comm;
	private ManagedChannel channel;
	private final String url;
	final LogService logger;

	public BlenderWriter(final String hostAndPort)
	{
		this(hostAndPort, new StderrLogService());
	}

	public BlenderWriter(final String hostAndPort, LogService logService)
	{
		url = hostAndPort;
		logger = logService;

		try {
			channel = ManagedChannelBuilder.forTarget(url).usePlaintext().build();
			comm = PointsAndLinesGrpc.newStub(channel);

			nodes = comm.sendBall(getNoResponseExpectedObj());
			lines = comm.sendLineWithIDs(getNoResponseExpectedObj());
			linePs = comm.sendLineWithPos(getNoResponseExpectedObj());
			isValid = true;
		} catch (StatusRuntimeException e) {
			logger.warn("RPC client-side failed while accessing "+url
				+", details follow:\n"+e.getMessage());
		}
	}

	int nodesCnt;
	int linesCnt;
	int linePsCnt;

	boolean isValid = false;
	boolean isClosed = false;

	@Override
	public void close() {
		// ManagedChannels use resources like threads and TCP connections. To prevent leaking these
		// resources the channel should be shut down when it will no longer be used. If it may be used
		// again leave it running.
		try {
			logger.info("nodes: "+nodesCnt+" , lines: "+linesCnt+", linePs: "+linePsCnt);
			if (linesCnt > 0) lines.onCompleted();
			if (linePsCnt > 0) linePs.onCompleted();
			if (nodesCnt > 0) nodes.onCompleted();

			//first, make sure the channel describe itself as "READY"
			//logger.info("state: "+channel.getState(false).name());
			int wantStillWaitTime = 20;
			while (channel.getState(false) != ConnectivityState.READY && wantStillWaitTime > 0) {
				int waitingTime = 2;
				wantStillWaitTime -= waitingTime;
				Thread.sleep(waitingTime * 1000); //seconds -> milis
			}
			//but even when it claims "READY", it still needs some grace time to finish any commencing transfers
			Thread.sleep( 5000);
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			/* don't care that waiting was interrupted */
		} catch (StatusRuntimeException e) {
			logger.warn("RPC client-side failed for "+url
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

	public void sendMessage(final String message)
	{
		comm.sendTick(PointsAndLinesOuterClass.TickMessage.newBuilder()
			.setMessage(message).build(), getNoResponseExpectedObj());
	}
	// -----------------------------------------------------------------------------

	private StreamObserver<PointsAndLinesOuterClass.Empty> getNoResponseExpectedObj()
	{
		return new EmptyIgnoringStreamObservers();
	}
	// -----------------------------------------------------------------------------

	float[] rgb = new float[3];
	void unbakeRGB(int colorRGB) {
		rgb[0] = (float)((colorRGB & 0xFF0000) >> 16) / 256.f;
		rgb[1] = (float)((colorRGB & 0x00FF00) >>  8) / 256.f;
		rgb[2] = (float)(colorRGB & 0x0000FF)         / 256.f;
	}

	HashMap<String,Integer> ids = new HashMap<>(10000);
	int nextAvailId = 1;
	int translateID(final String string_id) {
		int int_id = ids.getOrDefault(string_id,-1);
		if (int_id == -1) {
			int_id = nextAvailId;
			nextAvailId++;
			ids.put(string_id,int_id);
		}
		return int_id;
	}

	HashMap<Integer,Float> xs = new HashMap<>(10000);
	HashMap<Integer,Float> ys = new HashMap<>(10000);
	float memorizeAndReturn(int id, float value, final HashMap<Integer,Float> memory) {
		memory.put(id,value);
		return value;
	}
	// -----------------------------------------------------------------------------

	@Override
	public void addNode(String id, String label, int colorRGB, int x, int y) {
		addNode(id,label,colorRGB,x,y,defaultNodeWidth,defaultNodeHeight);
	}

	@Override
	public void addNode(String id, String label, int colorRGB, int x, int y, int width, int height) {
		if (!isValid) return;
		unbakeRGB(colorRGB);
		int iid = translateID(id);
		PointsAndLinesOuterClass.PointAsBall p = PointsAndLinesOuterClass.PointAsBall.newBuilder()
				.setID(iid)
				.setX(memorizeAndReturn(iid, x,xs))
				.setZ(memorizeAndReturn(iid,-y,ys))
				.setY(z_coord)
				.setT(0)
				.setLabel(label)
				.setColorR(rgb[0])
				.setColorG(rgb[1])
				.setColorB(rgb[2])
				.setRadius(width)
				.build();
		nodes.onNext(p);
		nodesCnt++;
	}

	@Override
	public void addStraightLine(String fromId, String toId) {
		if (!isValid) return;
		PointsAndLinesOuterClass.LineWithIDs l = PointsAndLinesOuterClass.LineWithIDs.newBuilder()
				.setID(translateID(toId))
				.setFromPointID(translateID(fromId))
				.setToPointID(translateID(toId))
				.setLabel(fromId+" -> "+toId)
				.setColorR(rgb[0])
				.setColorG(rgb[1])
				.setColorB(rgb[2])
				.setRadius(lineRadius)
				.build();
		lines.onNext(l);
		linesCnt++;
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

		PointsAndLinesOuterClass.LineWithPositions l = PointsAndLinesOuterClass.LineWithPositions.newBuilder()
				.setID(tid)
				.setFromX(xs.get(fid))
				.setFromZ(ys.get(fid))
				.setFromY(z_coord)
				.setToX(toX)
				.setToZ(-toY-bendingOffsetY)
				.setToY(z_coord)
				.setLabel(fromId+" -> "+toId+" first")
				.setColorR(rgb[0])
				.setColorG(rgb[1])
				.setColorB(rgb[2])
				.setRadius(lineRadius)
				.build();
		linePs.onNext(l);
		l = PointsAndLinesOuterClass.LineWithPositions.newBuilder()
				.setID(tid+30000)
				.setFromX(toX)
				.setFromZ(-toY-bendingOffsetY)
				.setFromY(z_coord)
				.setToX(toX)
				.setToZ(-toY)
				.setToY(z_coord)
				.setLabel(fromId+" -> "+toId+" second")
				.setColorR(rgb[0])
				.setColorG(rgb[1])
				.setColorB(rgb[2])
				.setRadius(lineRadius)
				.build();
		linePs.onNext(l);
		linePsCnt++;
	}

	@Override
	public void addBendedLineConnectedVertex(String parentNodeID, String newNodeID, String label, int colorRGB, int x, int y) {
		addNode(newNodeID, label,colorRGB, x,y);
		addBendedLine(parentNodeID, newNodeID, x,y);
	}
}
