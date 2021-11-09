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