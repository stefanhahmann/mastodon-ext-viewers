/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2021, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cz.it4i.ulman.transfers.graphexport.ui.util;

import cz.it4i.ulman.transfers.graphexport.leftrightness.DescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.AbstractDescendantsSorter;
import cz.it4i.ulman.transfers.graphexport.leftrightness.ui.PolesSorterDlg;
import cz.it4i.ulman.transfers.graphexport.leftrightness.ui.SlicesSorterDlg;
import cz.it4i.ulman.transfers.graphexport.leftrightness.ui.TriangleSorterDlg;

import org.mastodon.mamut.ProjectModel;
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
	public static final String M_SLICES = "slices method";
	public static final String M_TRIANGLE = "triangle method";

	/** instantiates the appropriate sorter and harvests user params
	 *  that the particular sorter needs for its work; because of the
	 *  latter, a bunch of extra parameters is required... */
	public static DescendantsSorter resolveSorterOfDaughters(
			final String chosenMethod,
			final CommandService commandService,
			final ProjectModel projectModel,
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
							"projectModel", projectModel,
							"projectID", projectID,
							"useImplicitCentre", false)
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((PolesSorterDlg)m.getCommand()).sorter;
		}
		else if (chosenMethod.equals(M_POLES_IC)) {
			//implicit centre
			final CommandModule m = commandService
					.run(PolesSorterDlg.class, true,
							"projectModel", projectModel,
							"projectID", projectID,
							"useImplicitCentre", true,
							"spotCentreName", "implicit_centre") //providedNotBeVisibleInTheGUI
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((PolesSorterDlg)m.getCommand()).sorter;
		}
		else if (chosenMethod.equals(M_SLICES)) {
			final CommandModule m = commandService
					.run(SlicesSorterDlg.class, true,
							"projectModel", projectModel,
							"projectID",projectID)
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((SlicesSorterDlg)m.getCommand()).sorter;
		}
		else if (chosenMethod.equals(M_TRIANGLE)) {
			final CommandModule m = commandService
					.run(TriangleSorterDlg.class, true,
							"projectModel", projectModel,
							"projectID",projectID)
					.get();
			if (!m.isCanceled()) sorterOfDaughters = ((TriangleSorterDlg)m.getCommand()).sorter;
		}

		return sorterOfDaughters;
	}
}