package cz.it4i.ulman.transfers.graphexport.ui.util;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.AbstractDescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.ui.PolesSorterDlg;
import cz.it4i.ulman.transfers.graphexport.leftrightness.ui.TriangleSorterDlg;

import org.mastodon.mamut.MamutAppModel;

import org.scijava.command.CommandService;
import org.scijava.command.CommandModule;

import java.util.concurrent.ExecutionException;

/** util class that lists all known descendants sorters (if used
 *  as a part of caller's GUI, and that can instantiate and harvest
 *  user-selected sorter.
 */
public class SortersChooserDlg {
	public static final String M_TRACKSCHEME = "as in TrackScheme";
	public static final String M_ALPHANUM = "alphanumeric on labels";
	public static final String M_POLES = "poles method";
	public static final String M_POLES_IC = "poles m. with implicit centre";
	public static final String M_TRIANGLE = "triangle method";

	/** instantiates the appropriate sorter and harvests user params
	 *  that the particular sorter needs for its work; because of the
	 *  latter, a bunch of extra parameters is required... */
	public static DescendantsSorter resolveSorterOfDaughters(
			final String chosenMethod,
			final CommandService commandService,
			final MamutAppModel appModel,
			final String projectID)
	throws ExecutionException, InterruptedException
	{
		DescendantsSorter sorterOfDaughters = null; //intentionally, indicates a problem...

		if (chosenMethod.equals(M_TRACKSCHEME)) {
			sorterOfDaughters = listOfDaughters -> {}; //NB: no sorting
		}
		else if (chosenMethod.equals(M_ALPHANUM)) {
			sorterOfDaughters = new AbstractDescendantsSorter();
		}
		else if (chosenMethod.equals(M_POLES)) {
			//explicit centre
			final CommandModule m = commandService
					.run(PolesSorterDlg.class, true,
							"appModel", appModel,
							"projectID", projectID,
							"useImplicitCentre", false)
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((PolesSorterDlg)m.getCommand()).sorter;
		}
		else if (chosenMethod.equals(M_POLES_IC)) {
			//implicit centre
			final CommandModule m = commandService
					.run(PolesSorterDlg.class, true,
							"appModel", appModel,
							"projectID", projectID,
							"useImplicitCentre", true,
							"spotCentreName", "implicit_centre") //providedNotBeVisibleInTheGUI
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((PolesSorterDlg)m.getCommand()).sorter;
		}
		else if (chosenMethod.equals(M_TRIANGLE)) {
			final CommandModule m = commandService
					.run(TriangleSorterDlg.class, true,
							"appModel", appModel,
							"projectID",projectID)
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((TriangleSorterDlg)m.getCommand()).sorter;
		}

		return sorterOfDaughters;
	}
}