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
package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.scijava.plugin.Parameter;

public abstract class AbstractGraphExportableDlg implements GraphExportableFetcher {
	@Parameter(label = "grid stepping along x", initializer = "provideDefaults")
	int xColumnWidth = 50;

	@Parameter(label = "grid stepping along y")
	int yLineStep = 100;

	@Parameter(label = "the bending point along y")
	int defaultBendingPointAbsoluteOffsetY = -80;

	@Parameter
	int defaultNodeWidth  = 15;

	@Parameter
	int defaultNodeHeight = 15;

	@Parameter(description = "RGB value in the hexadecimal format; 00 is black, FF is white")
	int defaultNodeColour = 0xCCCCCC;

	/** override this in upstream class if you wanna have own different default values */
	void provideDefaults() {
	}


	GraphExportable worker = null;
	//
	@Override
	public GraphExportable getUnderlyingGraphExportable() {
		if (worker == null) throw new IllegalStateException("Dialog "+this.getClass().getName()+" is broken.");

		worker.set_xColumnWidth(this.xColumnWidth);
		worker.set_yLineStep(this.yLineStep);
		worker.set_defaultBendingPointAbsoluteOffsetY(this.defaultBendingPointAbsoluteOffsetY);
		worker.set_defaultNodeWidth(this.defaultNodeWidth);
		worker.set_defaultNodeHeight(this.defaultNodeHeight);
		worker.set_defaultNodeColour(this.defaultNodeColour);
		return worker;
	}
}
