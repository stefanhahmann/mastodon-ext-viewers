package org.mastodon.lineage.processors;

import org.apache.commons.lang.StringUtils;
import org.mastodon.collection.RefList;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.collection.ref.RefArrayList;
import org.mastodon.lineage.processors.util.SpotsIterator;
import org.mastodon.mamut.MamutAppModel;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.Logger;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, name = "Filter lineage and remove solists spots" )
public class FilterOutSolists implements Command
{
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	final String hintMsg = "Solist has no ancestors and no descendants, plus the conditions below:";

	@Parameter
	private boolean isInTheLastTimePoint = true;

	@Parameter
	private boolean hasLabelMadeOfNumbersOnly = true;

	@Parameter(persist = false)
	private MamutAppModel appModel;

	@Parameter
	private LogService logService;

	@Override
	public void run()
	{
		final Logger loggerRoots = logService.subLogger("Searcher...");
		final Logger loggerSolists = logService.subLogger("Remove Solists");

		final RefList<Spot> solists = new RefArrayList<>(appModel.getModel().getGraph().vertices().getRefPool(),100);
		final int maxTP = appModel.getMaxTimepoint();

		final SpotsIterator ss = new SpotsIterator(appModel, loggerRoots);
		ss.visitAllSpots(s -> {
			if (ss.countAncestors(s) == 0 && ss.countDescendants(s) == 0) {
				loggerSolists.info("Potential solist: "+s.getLabel()+" at timepoint "+s.getTimepoint());
				//
				if (isInTheLastTimePoint && s.getTimepoint() != maxTP) return;
				if (hasLabelMadeOfNumbersOnly && !StringUtils.isNumeric(s.getLabel())) return;
				loggerSolists.info("                  ^^^^^ will be removed");
				solists.add(s);
			}});

		final ModelGraph g = appModel.getModel().getGraph();
		for (Spot s : solists) g.remove(s);
		appModel.getModel().getGraph().notifyGraphChanged();
	}
}
