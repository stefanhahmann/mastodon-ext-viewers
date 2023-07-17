/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, Vladimír Ulman
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
package cz.it4i.ulman.transfers.embeddings.experimental;

import org.joml.Vector3d;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
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
public class FlatDivisionAnalysis extends DynamicCommand {
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

		pluginAppModel.getAppModel().getModel()
				.getTagSetModel()
				.getTagSetStructure()
				.getTagSets()
				.forEach( ts -> choices.add(ts.getName()) );

		this.getInfo().getMutableInput("colorScheme",String.class).setChoices( choices );
	}

	@Parameter
	private LogService logService;

	@Parameter(persist = false)
	private MamutPluginAppModel pluginAppModel;

	@Override
	public void run() {
		final PoolCollectionWrapper<Spot> vertices = pluginAppModel.getAppModel().getModel().getGraph().vertices();

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

		//sweeping
		final SpotsIterator visitor = new SpotsIterator(pluginAppModel.getAppModel(),
				logService.subLogger("flat export"));

		org.mastodon.collection.RefList<Spot> mothers = new RefArrayList<>(vertices.getRefPool());

		visitor.visitRootsFromEntireGraph( root -> {
			visitor.visitDownstreamSpots(root, spot -> {
				if (visitor.countDescendants(spot) == 2) mothers.add(spot);
			});
		});

		double[] xy = { 0.f, 0.f };
		Spot trackStarts = pluginAppModel.getAppModel().getModel().getGraph().vertexRef();
		for (Spot m : mothers) {
			get2DPos(m, xy);
			visitor.findUpstreamSpot(m,trackStarts,999);
			System.out.println("Mother "+m.getLabel()
				+" at TP="+m.getTimepoint()+" @ xy="+xy[0]
				+","+xy[1]+":");
			System.out.println("  started with "+trackStarts.getLabel()
				+" at TP="+trackStarts.getTimepoint());
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
