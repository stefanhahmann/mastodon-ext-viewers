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
