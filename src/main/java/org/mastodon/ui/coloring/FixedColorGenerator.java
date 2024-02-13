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
