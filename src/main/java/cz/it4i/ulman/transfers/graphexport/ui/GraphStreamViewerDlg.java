package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.GraphStreamViewer;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Export into GraphStreamer Window")
public class GraphStreamViewerDlg extends AbstractGraphExportableDlg implements Command {
	// ------ options and setup of this particular export mode ------

	// ------ after all options are set, the workhorse is to be created here ------
	@Override
	public void run() {
		worker = new GraphStreamViewer("Mastodon Generated Lineage");
	}
}
