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
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
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
import java.util.List;
import java.util.Optional;


@Plugin( type = Command.class, name = "Display lineage in SimViewer" )
public class FullLineageToBlender extends DynamicCommand {
	@Parameter(persist = false)
	private MamutPluginAppModel pluginAppModel;

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

		pluginAppModel.getAppModel().getModel()
				.getTagSetModel()
				.getTagSetStructure()
				.getTagSets()
				.forEach( ts -> choices.add(ts.getName()) );

		this.getInfo().getMutableInput("colorScheme",String.class).setChoices( choices );
	}

	@Parameter(label = "Spheres scale:")
	private float scaleFactor = 0.4f;

	//TODO: make a drop-down choice box: full data as one node, tree as one node, track as one node
	@Parameter(label = "ala Matthias:")
	private boolean doIndividualTracks = false;

	@Parameter(label = "draw tracks:")
	private boolean doLines = false;
	@Parameter(label = "Lines width:")
	private float lineWidth = 0.4f;

	@Parameter
	private LogService logService;

	@Override
	public void run() {
		//init the communication side
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
			Optional<TagSetStructure.TagSet> ts = pluginAppModel.getAppModel().getModel()
					.getTagSetModel()
					.getTagSetStructure()
					.getTagSets()
					.stream()
					.filter(_ts -> _ts.getName().equals(colorScheme))
					.findFirst();
			final GraphColorGenerator<Spot, Link> colorizer
					= ts.isPresent() ? new TagSetGraphColorGenerator<>(
						pluginAppModel.getAppModel().getModel().getTagSetModel(), ts.get())
					: new FixedColorGenerator(255,255,255);
			//</colors>

			final SpotsIterator visitor = new SpotsIterator(pluginAppModel.getAppModel(),
					logService.subLogger("export of " + dataName));

			final BucketsWithGraphics.BatchOfGraphics.Builder[] nodeBuilder = { null };
			final Spot motherSpotRef = pluginAppModel.getAppModel().getModel().getGraph().vertexRef();

			visitor.visitRootsFromEntireGraph( root -> {
				nodeBuilder[0] = BucketsWithGraphics.BatchOfGraphics.newBuilder()
						.setClientID(conn.clientIdObj)
						.setCollectionName(dataName)
						.setDataName(root.getLabel())
						.setDataID(root.getInternalPoolIndex());

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
						//logService.info("adding sphere at: "+sBuilder.getTime());
						nodeBuilder[0].addLines(lBuilder);
					}
					if (doIndividualTracks) {
						if (motherSpotRef.getInternalPoolIndex() != spot.getInternalPoolIndex()
							&& visitor.countDescendants(motherSpotRef) > 1)
						{
							//beginning of a new track, yay!
							System.out.println("Found new beginning: "+spot.getLabel());
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

					sBuilder.setCentre( vBuilder
							//updates the builder content and builds inside setCentre()
							.setX(spot.getFloatPosition(0))
							.setY(spot.getFloatPosition(1))
							.setZ(spot.getFloatPosition(2)) );
					sBuilder.setTime(spot.getTimepoint());
					sBuilder.setRadius(scaleFactor * (float)Math.sqrt(spot.getBoundingSphereRadiusSquared()));
					sBuilder.setColorXRGB( colorizer.color(spot) );
					//logService.info("adding sphere at: "+sBuilder.getTime());
					nodeBuilder[0].addSpheres(sBuilder);
				});
				dataSender.onNext( nodeBuilder[0].build() );
			});
			dataSender.onCompleted();

			pluginAppModel.getAppModel().getModel().getGraph().releaseRef( motherSpotRef );

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
