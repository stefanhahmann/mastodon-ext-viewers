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
package cz.it4i.ulman.transfers.embeddings;

import cz.it4i.ulman.transfers.BlenderSendingUtils;
import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.joml.Vector3d;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.tomancak.util.SpotsIterator;
import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.pool.PoolCollectionWrapper;
import org.mastodon.ui.coloring.FixedColorGenerator;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.ui.coloring.TagSetGraphColorGenerator;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cz.it4i.ulman.transfers.graphexport.Utils.createVector3d;

@Plugin(type = Command.class, name = "Flat View Parameters")
public class FlatView extends DynamicCommand {
	@Parameter(label = "Label of north-pole spot:")
	public String spotNorthPoleName;

	@Parameter(label = "Label of south-pole spot:")
	public String spotSouthPoleName;

	@Parameter(visibility = ItemVisibility.MESSAGE, required = false)
	private final String sewInfoMsg1 = "Considering a look from the outside, having tails-to-head orientation parallel with";
	@Parameter(visibility = ItemVisibility.MESSAGE, required = false)
	private final String sewInfoMsg2 = "the south-to-north orientation, choose surface spot that'd be in the flat view centre.";
	@Parameter(label = "Label of the centre spot:")
	public String spotViewCentreName;

	@Parameter(label = "South-north direction stretch factor:")
	public float elevSpacing = 1.0f;
	@Parameter(label = "Perpendicular direction stretch factor:")
	public float azimuthSpacing = 1.0f;
	@Parameter(label = "Radius used to draw the spheres:")
	public float spheresRadius = 1.0f;

	@Parameter
	public String connectURL = "localhost:9083";

	@Parameter(label = "Nickname of this experiment data:")
	public String clientName = "E1";

	@Parameter(label = "Nickname of this flat view:")
	public String dataName = "view1";

	@Parameter(label = "Choose color scheme:", initializer = "readAvailColors", choices = {})
	public String colorScheme = "All white";

	void readAvailColors() {
		//there gotta be at least one choice... e.g. when there's not a single tagset available
		List<String> choices = new ArrayList<>(50);
		choices.add("All white");

		projectModel.getModel()
				.getTagSetModel()
				.getTagSetStructure()
				.getTagSets()
				.forEach( ts -> choices.add(ts.getName()) );

		this.getInfo().getMutableInput("colorScheme",String.class).setChoices( choices );
	}

	@Parameter(label = "Show also debug-orientation vectors:")
	public boolean showDebug = false;

	@Parameter(label = "Division-orientation analysis, show mother tracks orientations:")
	public boolean showDO_motherTrackLines = false;
	@Parameter(label = "Division-orientation analysis, up to points back to set mother orientations:")
	public int showDO_motherTrackHistoryTPs = 999;
	@Parameter(label = "Division-orientation analysis, show daughters connection lines:")
	public boolean showDO_daughtersLines = false;
	@Parameter(label = "Division-orientation analysis, hide spots to see lines better:")
	public boolean showDO_hideSpotsForTheSakeOfLines = false;

	@Parameter
	private LogService logService;

	@Parameter(persist = false)
	private ProjectModel projectModel;

	@Override
	public void run() {
		final PoolCollectionWrapper<Spot> vertices = projectModel.getModel().getGraph().vertices();

		Optional<Spot> searchSpot = vertices.stream().filter(s -> s.getLabel().equals(spotNorthPoleName)).findFirst();
		if (!searchSpot.isPresent()) {
			logService.error("Couldn't find (north pole) spot with label "+spotNorthPoleName);
			return;
		}
		final Vector3d posN = createVector3d(searchSpot.get());
		final int debugTime = searchSpot.get().getTimepoint();
		//
		searchSpot = vertices.stream().filter(s -> s.getLabel().equals(spotSouthPoleName)).findFirst();
		if (!searchSpot.isPresent()) {
			logService.error("Couldn't find (south pole) spot with label "+spotSouthPoleName);
			return;
		}
		centre = createVector3d(searchSpot.get());
		centre.add(posN).div(2.0);
		//
		upVec = new Vector3d(posN).sub(centre).normalize();

		searchSpot = vertices.stream().filter(s -> s.getLabel().equals(spotViewCentreName)).findFirst();
		if (!searchSpot.isPresent()) {
			logService.error("Couldn't find (view centre pole) spot with label "+ spotViewCentreName);
			return;
		}
		frontVec = createVector3d(searchSpot.get());
		frontVec.sub(centre);
		//
		//move along upVec-given orientation to find latCentre such that
		//the angle originalNorthPole-latCentre-theFrontPole is perpendicular,
		//that said, we subtract the upVec contribution from the frontVec
		latCentre = new Vector3d(upVec);
		latCentre.mul( upVec.dot(frontVec) ).add(centre);
		frontVec.add(centre)    //moves back to where the "view centre pole" is
				.sub(latCentre);  //a vector again; finally, from latCentre to the "view centre pole"
		final float debugViewCentreLength = (float)frontVec.length();
		frontVec.normalize();
		//
		//upVec is a normal vector to a plane that includes latCentre and frontVec (and "view centre pole"),
		//this creates the 3rd "coordinate axis" -- used to define fully the azimuth angle
		sideVec = frontVec.cross(upVec, new Vector3d());

		//<colors>
		Optional<TagSetStructure.TagSet> ts = projectModel.getModel()
				.getTagSetModel()
				.getTagSetStructure()
				.getTagSets()
				.stream()
				.filter(_ts -> _ts.getName().equals(colorScheme))
				.findFirst();
		final GraphColorGenerator<Spot, Link> colorizer
				= ts.isPresent() ? new TagSetGraphColorGenerator<>(
				projectModel.getModel().getTagSetModel(), ts.get())
				: new FixedColorGenerator(255,255,255);
		//</colors>

		//sweeping
		final Vector3d runner = new Vector3d();
		final Vector3d runnerProjectedToLateralPlane = new Vector3d();
		final SpotsIterator visitor = new SpotsIterator(projectModel,
				logService.subLogger("flat export"));

		try {
			final BlenderSendingUtils.BlenderConnectionHandle conn
					= BlenderSendingUtils.connectToBlender(connectURL, clientName);
			conn.sendInitialIntroHandshake();

			//now keep pushing data away to the channel
			final StreamObserver<BucketsWithGraphics.BatchOfGraphics> dataSender
					= conn.commContinuous.replaceGraphics(new EmptyIgnoringStreamObservers());

			final BucketsWithGraphics.Vector3D.Builder vBuilder
					= BucketsWithGraphics.Vector3D.newBuilder();
			final BucketsWithGraphics.SphereParameters.Builder sBuilder
					= BucketsWithGraphics.SphereParameters.newBuilder();
			final BucketsWithGraphics.LineParameters.Builder lBuilder
					= BucketsWithGraphics.LineParameters.newBuilder();
			final BucketsWithGraphics.VectorParameters.Builder aBuilder // a = arrow
					= BucketsWithGraphics.VectorParameters.newBuilder();

			//send debug data
			if (showDebug) {
				final BucketsWithGraphics.BatchOfGraphics.Builder debugNodeBuilder = BucketsWithGraphics.BatchOfGraphics.newBuilder()
						.setClientID(conn.clientIdObj)
						.setCollectionName(dataName)
						.setDataName("orientation outline")
						.setDataID(0);
				final BucketsWithGraphics.VectorParameters.Builder dBuilder
						= BucketsWithGraphics.VectorParameters.newBuilder();
				dBuilder.setStartPos(vBuilder.setX((float) centre.x).setY((float) centre.y).setZ((float) centre.z));
				dBuilder.setEndPos(vBuilder.setX((float) posN.x).setY((float) posN.y).setZ((float) posN.z));
				dBuilder.setTime(debugTime);
				dBuilder.setRadius(10f);
				dBuilder.setColorXRGB(0xFF0000);
				debugNodeBuilder.addVectors(dBuilder);
				//
				//"front" axis towards the view centre spot
				dBuilder.setStartPos(vBuilder.setX((float) latCentre.x).setY((float) latCentre.y).setZ((float) latCentre.z));
				runner.set(frontVec).mul(debugViewCentreLength).add(latCentre);
				dBuilder.setEndPos(vBuilder.setX((float) runner.x).setY((float) runner.y).setZ((float) runner.z));
				dBuilder.setColorXRGB(0x00FF00);
				debugNodeBuilder.addVectors(dBuilder);
				//
				//"side" aux vector (again, exists here to help with deciding the full azimuth)
				dBuilder.setStartPos(vBuilder.setX((float) centre.x).setY((float) centre.y).setZ((float) centre.z));
				runner.set(sideVec).mul(0.3f * debugViewCentreLength).add(centre);
				dBuilder.setEndPos(vBuilder.setX((float) runner.x).setY((float) runner.y).setZ((float) runner.z));
				dBuilder.setColorXRGB(0x0000FF);
				debugNodeBuilder.addVectors(dBuilder);
				dataSender.onNext(debugNodeBuilder.build());
			}
			//end of: send debug data

			Spot trackEnds = projectModel.getModel().getGraph().vertexRef();
			Spot trackStarts = projectModel.getModel().getGraph().vertexRef();
			double[] xyS = { 0.f, 0.f };
			double[] xyE = { 0.f, 0.f };
			double[] xyD0 = { 0.f, 0.f };
			double[] xyD1 = { 0.f, 0.f };
			org.mastodon.collection.RefList<Spot> daughterList = new RefArrayList<>(vertices.getRefPool());

			visitor.visitRootsFromEntireGraph( root -> {
				final BucketsWithGraphics.BatchOfGraphics.Builder nodeBuilder = BucketsWithGraphics.BatchOfGraphics.newBuilder()
						.setClientID(conn.clientIdObj)
						.setCollectionName(dataName)
						.setDataName(root.getLabel())
						.setDataID(root.getInternalPoolIndex());

				visitor.visitDownstreamSpots(root, spot -> {
					if (!showDO_hideSpotsForTheSakeOfLines) {
						get2DPos(spot, xyS);

						//updates the builder content and builds inside setCentre()
						sBuilder.setCentre(vBuilder.setX((float) xyS[0]).setY((float) xyS[1]).setZ(0.f));
						sBuilder.setTime(spot.getTimepoint());
						sBuilder.setRadius(spheresRadius);
						sBuilder.setColorXRGB(colorizer.color(spot));
						//logService.info("adding sphere at: "+sBuilder.getTime());
						nodeBuilder.addSpheres(sBuilder);
					}

					if (visitor.countDescendants(spot) == 2) {
						//reached a division point
						if (showDO_motherTrackLines) {
							trackEnds.refTo(spot);
							visitor.findUpstreamSpot(trackEnds,trackStarts,showDO_motherTrackHistoryTPs);

							get2DPos(trackStarts, xyS);
							get2DPos(trackEnds, xyE);
							aBuilder.setStartPos( vBuilder.setX( (float)xyS[0] ).setY( (float)xyS[1] ).setZ(0.f) );
							aBuilder.setEndPos( vBuilder.setX( (float)xyE[0] ).setY( (float)xyE[1] ).setZ(0.f) );
							aBuilder.setTime(spot.getTimepoint()+1);
							aBuilder.setRadius(4.f);
							aBuilder.setColorXRGB( colorizer.color(spot) );
							nodeBuilder.addVectors( aBuilder );
						}

						if (showDO_daughtersLines) {
							daughterList.clear();
							visitor.enlistDescendants(spot, daughterList);
							if (daughterList.size() == 2) { //should always be true...
								get2DPos(daughterList.get(0), xyD0);
								get2DPos(daughterList.get(1), xyD1);
								lBuilder.setStartPos(vBuilder.setX((float) xyD0[0]).setY((float) xyD0[1]).setZ(0.f));
								lBuilder.setEndPos(vBuilder.setX((float) xyD1[0]).setY((float) xyD1[1]).setZ(0.f));
								lBuilder.setTime(spot.getTimepoint() + 1);
								lBuilder.setRadius(2.f);
								lBuilder.setColorXRGB(colorizer.color(spot));
								nodeBuilder.addLines(lBuilder);
							}
						}
					}
				});
				dataSender.onNext( nodeBuilder.build() );
			});
			dataSender.onCompleted();

			projectModel.getModel().getGraph().releaseRef(trackStarts);
			projectModel.getModel().getGraph().releaseRef(trackEnds);

			conn.closeConnection();
		}
		catch (StatusRuntimeException e) {
			logService.error("Mastodon network sender: GRPC: " + e.getMessage());
		} catch (Exception e) {
			logService.error("Mastodon network sender: Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	final Vector3d runner = new Vector3d();
	final Vector3d runnerProjectedToLateralPlane = new Vector3d();

	Vector3d centre, upVec, latCentre, sideVec, frontVec;

	void get2DPos(Spot spot, double[] xy) {
		runner.set(spot.getFloatPosition(0),spot.getFloatPosition(1),spot.getFloatPosition(2));
		runnerProjectedToLateralPlane.set(runner); //a copy

		runner.sub(centre).normalize();
		double elevAngle = Math.acos( runner.dot(upVec) );

		runnerProjectedToLateralPlane.sub(latCentre);
		runner.set(upVec);
		runner.mul( -1.0 * upVec.dot(runnerProjectedToLateralPlane) );
		runnerProjectedToLateralPlane.add( runner );

		double azimuthAng = Math.acos( runnerProjectedToLateralPlane.normalize().dot(frontVec) );
		azimuthAng *= runnerProjectedToLateralPlane.dot(sideVec) < 0 ? +1 : -1;

		xy[0] = azimuthSpacing*azimuthAng;
		xy[1] = elevSpacing*elevAngle;
	}
}
