package org.mastodon.ui.coloring;

import org.junit.Test;

public class testFixedColorGenerator {
	@Test
	public void testCreatingAndPrinting() {
		System.out.println( new FixedColorGenerator(0.5f, 0.5f, 0.5f) );
		System.out.println( new FixedColorGenerator(127, 128, 129) );

		FixedColorGenerator cg = new FixedColorGenerator(-10, 270, 0);
		System.out.println( cg );
		System.out.println( "color = " + cg.color( null ) );
	}
}
