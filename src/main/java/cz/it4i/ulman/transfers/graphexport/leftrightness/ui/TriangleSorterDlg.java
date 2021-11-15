package cz.it4i.ulman.transfers.graphexport.leftrightness.ui;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.TriangleSorter;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;
import org.mastodon.pool.PoolCollectionWrapper;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Optional;

@Plugin(type = Command.class, name = "Triangle method: Parameters")
public class TriangleSorterDlg implements Command {
	@Parameter(label = "Label of centre spot:")
	public String spotCentreName;

	@Parameter(label = "Label of axis A spot:")
	public String spotAName;

	@Parameter(label = "Label of axis B spot:")
	public String spotBName;

	@Parameter(label = "Angle (deg) for inner layer:", min = "0", max = "90")
	public int innerLayerCutOff = 30;

	@Parameter(label = "Angle (deg) for outer layer:", min = "90", max = "180")
	public int outerLayerCutOff = 150;

	@Parameter(persist = false)
	private MamutAppModel appModel;

	@Parameter
	private LogService logService;

	//null indicates failure during the resolving of this dialog choices
	public DescendantsSorter sorter = null;

	@Override
	public void run() {
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

		final TriangleSorter ts = new TriangleSorter(spotCentre.get(), spotA.get(), spotB.get());
		ts.layeringLowerCutoffAngleDeg = innerLayerCutOff;
		ts.layeringUpperCutoffAngleDeg = outerLayerCutOff;
		sorter = ts;
	}
}
