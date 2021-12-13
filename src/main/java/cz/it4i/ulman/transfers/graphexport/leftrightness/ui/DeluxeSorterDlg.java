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
package cz.it4i.ulman.transfers.graphexport.leftrightness.ui;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.DeluxeSorter;
import cz.it4i.ulman.transfers.graphexport.ui.util.PerProjectPrefsService;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolCollectionWrapper;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import java.util.Optional;
import static cz.it4i.ulman.transfers.graphexport.Utils.createVector3d;

@Plugin(type = Command.class, name = "Deluxe method: Parameters")
public class DeluxeSorterDlg implements Command {
	//NB: persist = false because we read/store ourselves
	@Parameter(label = "Label of north spot (ns):", persist = false, initializer = "loadParams")
	public String spotNorth;

	@Parameter(label = "Label of south spot (ns):", persist = false)
	public String spotSouth;

	@Parameter(label = "Label of east spot (ns):", persist = false)
	public String spotEast;

	@Parameter(label = "Angle (deg) for inner layer:", min = "0", max = "90")
	public int innerLayerCutOff = 20;

	@Parameter(label = "Angle (deg) for outer layer:", min = "90", max = "180")
	public int outerLayerCutOff = 160;

	@Parameter(persist = false)
	private MamutAppModel appModel;

	@Parameter
	private LogService logService;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	final String msg_nsA = "Dialog starts as usually with last entered and thus project-agnostic values,";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	final String msg_nsB = "except for (ns - not shared) values which are memorized per project.";

	/** this param comes from the caller and should identify the project behind this */
	@Parameter(persist = false, required = false)
	String projectID = "default";
	@Parameter
	private PrefService ps;
	//
	void loadParams() {
		spotNorth = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotNorth","--type in spot label--");
		spotSouth = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotSouth","--type in spot label--");
		spotEast = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotEast","--type in spot label--");
	}
	void storeParams() {
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotNorth", spotNorth);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotSouth", spotSouth);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotEast", spotEast);
	}

	//null indicates failure during the resolving of this dialog choices
	public DescendantsSorter sorter = null;

	@Override
	public void run() {
		storeParams();
		final PoolCollectionWrapper<Spot> vertices = appModel.getModel().getGraph().vertices();

		final Optional<Spot> sNorth = vertices.stream().filter(s -> s.getLabel().equals(spotNorth)).findFirst();
		if (!sNorth.isPresent()) {
			logService.error("Couldn't find (north) spot with label "+ spotNorth);
			return;
		}

		final Optional<Spot> sSouth = vertices.stream().filter(s -> s.getLabel().equals(spotSouth)).findFirst();
		if (!sSouth.isPresent()) {
			logService.error("Couldn't find (south) spot with label "+ spotSouth);
			return;
		}

		final Optional<Spot> sEast = vertices.stream().filter(s -> s.getLabel().equals(spotEast)).findFirst();
		if (!sEast.isPresent()) {
			logService.error("Couldn't find (east) spot with label "+ spotEast);
			return;
		}

		logService.info("DeluxeSorter: proceeding with spots "+ spotNorth +", "+ spotSouth
				+" and "+ spotEast +" found ("+innerLayerCutOff+","+outerLayerCutOff+")");

		final DeluxeSorter ds = new DeluxeSorter(createVector3d(sNorth.get()),
				createVector3d(sSouth.get()), createVector3d(sEast.get()));
		ds.layeringLowerCutoffAngleDeg = innerLayerCutOff;
		ds.layeringUpperCutoffAngleDeg = outerLayerCutOff;
		sorter = ds;
	}
}
