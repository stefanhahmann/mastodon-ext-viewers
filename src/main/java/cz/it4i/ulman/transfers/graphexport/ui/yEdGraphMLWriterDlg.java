package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.yEdGraphMLWriter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import java.io.File;

@Plugin(type = Command.class, name = "Export into yEd")
public class yEdGraphMLWriterDlg extends AbstractGraphExportableDlg implements Command {
	// ------ options and setup of this particular export mode ------
	@Parameter(label = "Define .graphml file to save the lineage: ", style = FileWidget.SAVE_STYLE)
	File graphMLfile = new File("/tmp/mastodon.graphml");

	@Override
	void provideDefaults() {
		xColumnWidth = 80;
		defaultNodeWidth = 50;
	}

	// ------ after all options are set, the workhorse is to be created here ------
	@Override
	public void run() {
		worker = new yEdGraphMLWriter(graphMLfile.getPath());
	}
}