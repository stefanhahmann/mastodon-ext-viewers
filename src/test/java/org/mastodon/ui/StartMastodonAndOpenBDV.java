/*-
 * #%L
 * Online Mastodon Exports
 * %%
 * Copyright (C) 2021 - 2024 Vladimír Ulman
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.ui;

import mpicbg.spim.data.SpimDataException;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.project.MamutProjectIO;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.scijava.Context;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class StartMastodonAndOpenBDV {
	public static void main(String[] args) {
		try {
			//Apple stuff: move the menu dialog to the top of the screen (native Apple)
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );

			final String dummyDataPath = "x=100 y=100 z=100 sx=1 sy=1 sz=1 t=10.dummy";
			final ProjectModel projectModel = ProjectLoader.open(
					MamutProjectIO.fromBdvFile(new File(dummyDataPath)), new Context() );

			//note that the BDV window of Mastodon can exist without the main Mastodon window being around
			//
			//final MainWindow win = new MainWindow( projectModel );
			//win.setVisible( true );
			//win.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

			//opens a new BDV GUI window
			final MamutViewBdv bdv = projectModel.getWindowManager().createView(MamutViewBdv.class);

			//demo interaction with that window
			for (int i = 0; i < 5; ++i) {
				System.out.println("requested to advance to another time point");
				bdv.getViewerPanelMamut().nextTimePoint();
				Thread.sleep(3000);
			}

		} catch (IOException | SpimDataException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
