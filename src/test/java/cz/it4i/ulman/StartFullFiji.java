package cz.it4i.ulman;

import net.imagej.legacy.ui.LegacyUI;
import org.scijava.Context;
import org.scijava.ui.UIService;

public class StartFullFiji {
	public static void main(String[] args) {
		final Context context = new Context();
		context.service( UIService.class ).showUI( LegacyUI.NAME );
	}
}