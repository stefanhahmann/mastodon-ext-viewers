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
package cz.it4i.ulman.transfers.graphexport;

public class AbstractGraphExporter {
	int xColumnWidth = 50;
	int yLineStep = 100;
	int defaultBendingPointAbsoluteOffsetY = -80;
	int defaultNodeWidth  = 30;
	int defaultNodeHeight = 30;
	int defaultNodeColour = 0xCCCCCC;


	public int get_xColumnWidth() {
		return xColumnWidth;
	}
	public int get_yLineStep() {
		return yLineStep;
	}
	public int get_defaultBendingPointAbsoluteOffsetY() {
		return defaultBendingPointAbsoluteOffsetY;
	}
	public int get_defaultNodeWidth() {
		return defaultNodeWidth;
	}
	public int get_defaultNodeHeight() {
		return defaultNodeHeight;
	}
	public int get_defaultNodeColour() {
		return defaultNodeColour;
	}


	public void set_xColumnWidth(int val) {
		xColumnWidth = val;
	}
	public void set_yLineStep(int val) {
		yLineStep = val;
	}
	public void set_defaultBendingPointAbsoluteOffsetY(int val) {
		defaultBendingPointAbsoluteOffsetY = val;
	}
	public void set_defaultNodeWidth(int val) {
		defaultNodeWidth = val;
	}
	public void set_defaultNodeHeight(int val) {
		defaultNodeHeight = val;
	}
	public void set_defaultNodeColour(int val) {
		defaultNodeColour = val;
	}
}