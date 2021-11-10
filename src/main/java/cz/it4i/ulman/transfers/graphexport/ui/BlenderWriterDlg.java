package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.BlenderWriter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Export into Blender")
public class BlenderWriterDlg extends AbstractGraphExportableDlg implements Command {
	// ------ options and setup of this particular export mode ------
	@Parameter(label = "Connecting line width:")
	float defaultLineWidth = 10;

	@Parameter(label = "Use this Z-position:")
	float defaultZCoord = 0;

	@Parameter(label = "Address of the listening Blender:",
		description = "Provide always in the form hostname:port number.")
	String url = "localhost:9081";

	// ------ after all options are set, the workhorse is to be created here ------
	@Override
	public void run() {
		final BlenderWriter bw = new BlenderWriter(url);
		bw.lineRadius = defaultLineWidth / 2.f;
		bw.z_coord = defaultZCoord;
		//
		worker = bw;
	}
}
