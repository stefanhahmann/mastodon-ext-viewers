package cz.it4i.ulman.transfers.graphexport.leftrightness.ui;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.TriangleSorter;
import cz.it4i.ulman.transfers.graphexport.ui.PerProjectPrefsService;
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

@Plugin(type = Command.class, name = "Triangle method: Parameters")
public class TriangleSorterDlg implements Command {
	//NB: persist = false because we read/store ourselves
	@Parameter(label = "Label of centre spot (ns):", persist = false, initializer = "loadParams")
	public String spotCentreName;

	@Parameter(label = "Label of axis A spot (ns):", persist = false)
	public String spotAName;

	@Parameter(label = "Label of axis B spot (ns):", persist = false)
	public String spotBName;

	@Parameter(label = "Angle (deg) for inner layer:", min = "0", max = "90")
	public int innerLayerCutOff = 30;

	@Parameter(label = "Angle (deg) for outer layer:", min = "90", max = "180")
	public int outerLayerCutOff = 150;

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
		spotCentreName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotCentreName","--type in spot label--");
		spotAName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotAName","--type in spot label--");
		spotBName = PerProjectPrefsService.loadStringParam(ps,this.getClass(),projectID,"spotBName","--type in spot label--");
	}
	void storeParams() {
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotCentreName",spotCentreName);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotAName",spotAName);
		PerProjectPrefsService.storeStringParam(ps,this.getClass(),projectID,"spotBName",spotBName);
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

		final Optional<Spot> spotA = vertices.stream().filter(s -> s.getLabel().equals(spotAName)).findFirst();
		if (!spotA.isPresent()) {
			logService.error("Couldn't find (A) spot with label "+spotAName);
			return;
		}

		final Optional<Spot> spotB = vertices.stream().filter(s -> s.getLabel().equals(spotBName)).findFirst();
		if (!spotB.isPresent()) {
			logService.error("Couldn't find (B) spot with label "+spotBName);
			return;
		}

		logService.info("TriangleSorter: proceeding with spots "+spotCentreName+", "+spotAName
				+" and "+spotBName+" found ("+innerLayerCutOff+","+outerLayerCutOff+")");

		final TriangleSorter ts = new TriangleSorter(createVector3d(spotCentre.get()),
				createVector3d(spotA.get()), createVector3d(spotB.get()));
		ts.layeringLowerCutoffAngleDeg = innerLayerCutOff;
		ts.layeringUpperCutoffAngleDeg = outerLayerCutOff;
		sorter = ts;
	}
}
