package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import cz.it4i.ulman.transfers.graphexport.yEdGraphMLWriter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;

@Plugin(type = Command.class, name = "Export into yEd")
public class yEdGraphMLWriterDlg implements Command, GraphExportableFetcher {
	// ------ options and setup of this particular export mode ------
	@Parameter(label = "Define .graphml file to save the lineage: ", style = FileWidget.SAVE_STYLE)
	private File graphMLfile = new File("/tmp/mastodon.graphml");

	// ------ the exporting workhorse ------
	private yEdGraphMLWriter worker;

	// ------ after all options are set, the workhorse is to be created here ------
	@Override
	public void run() {
		worker = new yEdGraphMLWriter(graphMLfile.getPath());
	}

	// ------ common way to return the underlying workhorse ------
	@Override
	public GraphExportable getUnderlyingGraphExportable() {
		return worker;
	}
}