package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.BlenderWriter;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.prefs.PrefService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Export into Blender")
public class BlenderWriterDlg extends AbstractGraphExportableDlg implements Command {
	// ------ options and setup of this particular export mode ------
	@Parameter(label = "Connecting line width:")
	float defaultLineWidth = 5;

	@Parameter(label = "Use this Z-position:")
	float defaultZCoord = 0;

	//NB: persist = false because we read/store ourselves
	@Parameter(label = "Nickname of this Mastodon instance (ns):", initializer = "loadDataNickname", persist = false)
	String dataNickname = "Mastodon1";

	/** this param comes from the caller and should identify the project behind this */
	@Parameter(persist = false, required = false)
	String projectID = "default";

	@Parameter
	PrefService prefService;
	//
	private void loadDataNickname() {
		dataNickname = PerProjectPrefsService.loadStringParam(prefService,this.getClass(),projectID,"dataNickname","Mastodon1");
	}
	private void storeDataNickname() {
		PerProjectPrefsService.storeStringParam(prefService,this.getClass(),projectID,"dataNickname",dataNickname);
	}

	@Parameter(label = "Address of the listening Blender:",
		description = "Provide always in the form hostname:port number.")
	String url = "localhost:9081";

	@Override
	void provideDefaults() {
		xColumnWidth = 100;
		defaultNodeWidth = 15;
	}

	@Parameter
	LogService logService;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	final String msg_nsA = "Dialog starts as usually with last entered and thus project-agnostic values,";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	final String msg_nsB = "except for (ns - not shared) values which are memorized per project.";

	// ------ after all options are set, the workhorse is to be created here ------
	@Override
	public void run() {
		storeDataNickname();
		final BlenderWriter bw = new BlenderWriter(url,logService);
		bw.lineRadius = defaultLineWidth / 2.f;
		bw.z_coord = defaultZCoord;
		bw.sendMessage(dataNickname);
		//
		worker = bw;
	}
}
