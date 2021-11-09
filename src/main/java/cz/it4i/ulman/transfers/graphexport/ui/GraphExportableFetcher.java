package cz.it4i.ulman.transfers.graphexport.ui;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;

public interface GraphExportableFetcher {
	GraphExportable getUnderlyingGraphExportable();
}
