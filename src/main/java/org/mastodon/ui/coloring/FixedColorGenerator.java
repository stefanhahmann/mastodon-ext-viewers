package org.mastodon.ui.coloring;

import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.Spot;

public class FixedColorGenerator implements GraphColorGenerator<Spot, Link> {
	/**
	 * Values should be <0.0 ; 1.0>
	 */
	public FixedColorGenerator(float r, float g, float b) {
		int R = Math.max(0, Math.min(255, (int)(r*255) ));
		int G = Math.max(0, Math.min(255, (int)(g*255) ));
		int B = Math.max(0, Math.min(255, (int)(b*255) ));
		rgb = (R << 16) + (G << 8) + B;
	}

	/**
	 * Values should be <0 ; 255>
	 */
	public FixedColorGenerator(int r, int g, int b) {
		int R = Math.max(0, Math.min(255, r ));
		int G = Math.max(0, Math.min(255, g ));
		int B = Math.max(0, Math.min(255, b ));
		rgb = (R << 16) + (G << 8) + B;
	}

	final int rgb;

	@Override
	public int color(Spot vertex) {
		return rgb;
	}

	@Override
	public int color(Link edge, Spot source, Spot target) {
		return rgb;
	}

	@Override
	public String toString() {
		return String.format("%06x",rgb);
	}
}