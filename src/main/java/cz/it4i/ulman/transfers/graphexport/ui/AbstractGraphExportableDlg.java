package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.scijava.plugin.Parameter;

public abstract class AbstractGraphExportableDlg implements GraphExportableFetcher {
	@Parameter(label = "grid stepping along x")
	int xColumnWidth = 50;

	@Parameter(label = "grid stepping along y")
	int yLineStep = 100;

	@Parameter(label = "the bending point along y")
	int defaultBendingPointAbsoluteOffsetY = -80;

	@Parameter
	int defaultNodeWidth  = 30;

	@Parameter
	int defaultNodeHeight = 30;

	@Parameter(description = "RGB value in the hexadecimal format; 00 is black, FF is white")
	int defaultNodeColour = 0xCCCCCC;


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