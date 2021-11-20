package cz.it4i.ulman.transfers.graphexport.leftrightness.ui;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.PolesSorter;
import cz.it4i.ulman.transfers.graphexport.ui.PerProjectPrefsService;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolCollectionWrapper;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import java.util.Optional;

@Plugin(type = Command.class, name = "Poles method: Parameters")
public class PolesSorterDlg implements Command {
	//NB: persist = false because we read/store ourselves
	@Parameter(label = "Label of centre spot:", persist = false, initializer = "loadParams")
	public String spotCentreName;

	@Parameter(label = "Label of south-pole spot:", persist = false)
	public String spotSouthPoleName;

	@Parameter(label = "Label of north-pole spot:", persist = false)
	public String spotNorthPoleName;

	@Parameter(label = "Angle (deg) to switch to up/down test:", min = "0", max = "91")
	public int leftRightToUpDownCutOff = 60;

	@Parameter(label = "Angle (deg) for inner layer:", min = "0", max = "90")
	public int innerLayerCutOff = 30;

	@Parameter(label = "Angle (deg) for outer layer:", min = "90", max = "180")
	public int outerLayerCutOff = 150;

	@Parameter(persist = false)
	private MamutAppModel appModel;

	@Parameter
	private LogService logService;

	/** this param comes from the caller and should identify the project behind this */
	@Parameter(persist = false, required = false)
	String projectID = "default";
	@Parameter
	private PrefService ps;
	//
	void loadParams() {
		spotCentreName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotCentreName","--choose spot label--");
		spotSouthPoleName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotSouthPoleName","--choose spot label--");
		spotNorthPoleName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotNorthPoleName","--choose spot label--");
	}
	void storeParams() {
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotCentreName",spotCentreName);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotSouthPoleName",spotSouthPoleName);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotNorthPoleName",spotNorthPoleName);
	}

	//null indicates failure during the resolving of this dialog choices
	public DescendantsSorter sorter = null;

	@Override
	public void run() {
		storeParams();
		final PoolCollectionWrapper<Spot> vertices = appModel.getModel().getGraph().vertices();

		final Optional<Spot> spotCentre = vertices.stream().filter(s -> s.getLabel().equals(spotCentreName)).findFirst();
		if (!spotCentre.isPresent()) {
			logService.error("Couldn't find (centre) spot with label "+spotCentreName);
			return;
		}

		final Optional<Spot> spotS = vertices.stream().filter(s -> s.getLabel().equals(spotSouthPoleName)).findFirst();
		if (!spotS.isPresent()) {
			logService.error("Couldn't find (A) spot with label "+spotSouthPoleName);
			return;
		}

		final Optional<Spot> spotN = vertices.stream().filter(s -> s.getLabel().equals(spotNorthPoleName)).findFirst();
		if (!spotN.isPresent()) {
			logService.error("Couldn't find (B) spot with label "+spotNorthPoleName);
			return;
		}

		logService.info("PolesSorter: proceeding with spots "+spotCentreName+", "+spotSouthPoleName
				+" and "+spotNorthPoleName+" found ("+leftRightToUpDownCutOff+"; "+innerLayerCutOff+","+outerLayerCutOff+")");

		final PolesSorter ps = new PolesSorter(spotCentre.get(), spotS.get(), spotN.get());
		ps.lrTOupThresholdAngleDeg = leftRightToUpDownCutOff;
		ps.layeringLowerCutoffAngleDeg = innerLayerCutOff;
		ps.layeringUpperCutoffAngleDeg = outerLayerCutOff;
		sorter = ps;
	}
}
