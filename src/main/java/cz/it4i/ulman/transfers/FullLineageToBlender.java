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
package cz.it4i.ulman.transfers;

import cz.it4i.ulman.transfers.graphics.EmptyIgnoringStreamObservers;
import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.tomancak.util.SpotsIterator;

import org.mastodon.model.tag.TagSetStructure;
import org.mastodon.ui.coloring.FixedColorGenerator;
import org.mastodon.ui.coloring.GraphColorGenerator;
import org.mastodon.ui.coloring.TagSetGraphColorGenerator;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


@Plugin( type = Command.class, name = "Display full time-lapse of the lineage in Blender" )
public class FullLineageToBlender extends DynamicCommand {
	@Parameter(persist = false)
	private ProjectModel projectModel;

	@Parameter
	private String connectURL = "localhost:9083";

	@Parameter(label = "Nickname of this experiment data:")
	private String clientName = "E1";

	@Parameter(label = "Nickname of this displayed (sub)tree:")
	private String dataName = "view1";

	@Parameter(label = "Choose color scheme:", initializer = "readAvailColors", choices = {})
	private String colorScheme = "All white";

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

	@Parameter(label = "Spheres size:", choices = {"scaled by factor value below", "set to the absolute size below"})
	private String scaleMode = "scaled";
	@Parameter(label = "Spheres... value:")
	private float scaleSize = 1.5f;

	//TODO: make a drop-down choice box: full data as one node, tree as one node, track as one node
	private static final String GRP_LEVEL_FULL = "The whole lineage as one Blender node";
	private static final String GRP_LEVEL_TREE = "One lineage tree as one Blender node";
	private static final String GRP_LEVEL_TRACK = "One track as one Blender node";
	@Parameter(label = "Grouping level:", choices = { GRP_LEVEL_FULL, GRP_LEVEL_TREE, GRP_LEVEL_TRACK })
	private String chunkingLevel = GRP_LEVEL_TRACK;

	@Parameter(label = "Draw also tracks (as line segments):")
	private boolean doLines = false;
	@Parameter(label = "Line segments width:")
	private float lineWidth = 0.4f;

	@Parameter(label = "Displace lineages eccentrically by this amount:")
	private float eccentricOffsetSize = 0.f;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		//init the communication side
		final boolean areSphereSizesScaled = scaleMode.startsWith("scaled");
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

			final SpotsIterator visitor = new SpotsIterator(projectModel,
					logService.subLogger("export of " + dataName));

			//NB: this is a hack to be able to share the nodeBuilder among lambdas
			final BucketsWithGraphics.BatchOfGraphics.Builder[] nodeBuilder = { null };
			final Spot motherSpotRef = projectModel.getModel().getGraph().vertexRef();
			final boolean dontEverChangeBuilderNode = chunkingLevel.equals(GRP_LEVEL_FULL);
			final boolean doIndividualTracks = chunkingLevel.equals(GRP_LEVEL_TRACK);
			logService.info("Uploading plan: dontEverChangeBuilderNode = " + dontEverChangeBuilderNode
					+ ", doIndividualTracks = " + doIndividualTracks);

			//<eccentricity>
			final int minTP = projectModel.getMinTimepoint();
			//tp -> geom. centre over all spots in that tp
			final Map<Integer, float[]> globalCentre = new HashMap<>(projectModel.getMaxTimepoint()-minTP+1);
			final float[] currPos = new float[3]; //aux variable
			if (eccentricOffsetSize > 0) {
				//determine cell centres for each time point
				for (int tp = minTP; tp <= projectModel.getMaxTimepoint(); ++tp) {
					globalCentre.put(tp, new float[] {0,0,0});
					float[] cs = globalCentre.get(tp);
					long cnt = 0;
					for (Spot s : projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(tp)) {
						s.localize(currPos);
						cs[0] += currPos[0];
						cs[1] += currPos[1];
						cs[2] += currPos[2];
						++cnt;
					}
					if (cnt > 0) {
						cs[0] /= cnt;
						cs[1] /= cnt;
						cs[2] /= cnt;
					}
				}
			}

			final float[] defaultZeroPos = new float[] {0,0,0};
			final Function<Integer,float[]> commonZeroPosInitializer = (key) -> new float[]{0,0,0, 0}; //x,y,z, cnt
			//</eccentricity>

			visitor.visitRootsFromEntireGraph( root -> {
				//shall we init? if not, can we still re-init?
				if (nodeBuilder[0] == null || !dontEverChangeBuilderNode) {
					//System.out.println("changing node at root level");
					nodeBuilder[0] = BucketsWithGraphics.BatchOfGraphics.newBuilder()
							.setClientID(conn.clientIdObj)
							.setCollectionName(dataName)
							.setDataName( dontEverChangeBuilderNode ? "Full lineage" : root.getLabel() )
							.setDataID(root.getInternalPoolIndex());
				}

				//<eccentricity>
				final Map<Integer, float[]> lineageOffset = new HashMap<>(projectModel.getMaxTimepoint()-minTP+1);
				if (eccentricOffsetSize > 0) {
					//before the "normal scanning and drawing of this lineage",
					//first determine this lineage centres for every time point
					visitor.visitDownstreamSpots(root, spot -> {
						float[] cs = lineageOffset.computeIfAbsent(spot.getTimepoint(), commonZeroPosInitializer);
						spot.localize(currPos);
						cs[0] += currPos[0];
						cs[1] += currPos[1];
						cs[2] += currPos[2];
						cs[3] += 1;
					});

					//finish the centre calculation, convert the map to a "shifts provider"
					lineageOffset.forEach((k,pos) -> {
						//finish the calculation of the avg. coord (aka centre)
						pos[0] /= pos[3];
						pos[1] /= pos[3];
						pos[2] /= pos[3];

						//turn it into an "eccentric shift vector"
						float[] c = globalCentre.getOrDefault(k, defaultZeroPos); //"default param" should never happen! ...just in case
						pos[0] -= c[0];
						pos[1] -= c[1];
						pos[2] -= c[2];

						//resize it as desired
						final float scale = eccentricOffsetSize / (float)Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
						pos[0] *= scale;
						pos[1] *= scale;
						pos[2] *= scale;
					});
				}
				//</eccentricity>

				visitor.visitDownstreamSpots(root, spot -> {
					//am I the very first spot of a new track? (and should we care?)
					//which breaks into:
					//  - advance one up unless there's a mother
					//  - if we haven't advanced, we're beginning of some track
					if (doLines || doIndividualTracks) {
						visitor.findUpstreamSpot(spot, motherSpotRef, 1);
					}
					if (doLines && motherSpotRef.getInternalPoolIndex() != spot.getInternalPoolIndex()) {
						//build a line
						lBuilder.setStartPos( vBuilder
								.setX(spot.getFloatPosition(0))
								.setY(spot.getFloatPosition(1))
								.setZ(spot.getFloatPosition(2)) );
						lBuilder.setEndPos( vBuilder
								.setX(motherSpotRef.getFloatPosition(0))
								.setY(motherSpotRef.getFloatPosition(1))
								.setZ(motherSpotRef.getFloatPosition(2)) );
						lBuilder.setTime(spot.getTimepoint());
						lBuilder.setRadius(lineWidth);
						lBuilder.setColorXRGB( colorizer.color(spot) );
						nodeBuilder[0].addLines(lBuilder);
					}
					if (doIndividualTracks) {
						if (motherSpotRef.getInternalPoolIndex() != spot.getInternalPoolIndex()
							&& visitor.countDescendants(motherSpotRef) > 1)
						{
							//beginning of a new track, yay!
							logService.info("Found new beginning: "+spot.getLabel());
							//System.out.println("changing node at track level");
							//finish the current bucket...
							dataSender.onNext( nodeBuilder[0].build() );

							//...and start a new one
							nodeBuilder[0] = BucketsWithGraphics.BatchOfGraphics.newBuilder()
									.setClientID(conn.clientIdObj)
									.setCollectionName(dataName)
									.setDataName(spot.getLabel())
									.setDataID(spot.getInternalPoolIndex());
						}
					}

					spot.localize(currPos);
					float[] offset = lineageOffset.getOrDefault(spot.getTimepoint(), defaultZeroPos);
					currPos[0] += offset[0];
					currPos[1] += offset[1];
					currPos[2] += offset[2];
					sBuilder.setCentre( vBuilder
							//updates the builder content and builds inside setCentre()
							.setX(currPos[0]).setY(currPos[1]).setZ(currPos[2]) );
					sBuilder.setTime(spot.getTimepoint());
					//
					float size = scaleSize;
					if (areSphereSizesScaled) size *= (float)Math.sqrt(spot.getBoundingSphereRadiusSquared());
					sBuilder.setRadius(size);
					//
					sBuilder.setColorXRGB( colorizer.color(spot) );
					//logService.info("adding sphere at: "+sBuilder.getTime());
					nodeBuilder[0].addSpheres(sBuilder);
				});
				dataSender.onNext( nodeBuilder[0].build() );
			});
			dataSender.onCompleted();

			projectModel.getModel().getGraph().releaseRef( motherSpotRef );

			conn.closeConnection();
		}
		catch (StatusRuntimeException e) {
			logService.error("Mastodon network sender: GRPC: " + e.getMessage());
		} catch (Exception e) {
			logService.error("Mastodon network sender: Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
