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
package cz.it4i.ulman.transfers.graphexport.leftrightness;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.joml.Vector3d;
import org.mastodon.collection.RefList;
import org.mastodon.mamut.model.Spot;
import org.scijava.log.Logger;
import java.util.Comparator;
import java.util.Iterator;

public class AbstractDescendantsSorter implements DescendantsSorter {
	@Override
	public void sort(RefList<Spot> listOfDaughters) {
		listOfDaughters.sort(comparator);
	}

	@Override
	public void sort(RefList<Spot> listOfDaughters, final Logger log) {
		this.log = log;
		log.info("SORTER BEFORE: "+printList(listOfDaughters));
		listOfDaughters.sort(verboseComparator);
		log.info("SORTER  AFTER: "+printList(listOfDaughters));
	}


	/** default behaviour that compares using spots' labels */
	Comparator<Spot> comparator = Comparator.comparing(Spot::getLabel);

	/** verbose comparator that should be using this.log to report about its work,
	 * but that in this default form only uses the optimal (non-verbose) one */
	Comparator<Spot> verboseComparator = (a, b) -> comparator.compare(a,b);
	Logger log;


	public static
	String printList(RefList<Spot> listOfDaughters) {
		StringBuilder sb = new StringBuilder();
		Iterator<Spot> iter = listOfDaughters.iterator();
		while (iter.hasNext())
			sb.append(iter.next().getLabel()+", ");
		return sb.toString();
	}

	public static
	String printVector(final Vector3d v) {
		return printVector(v,1);
	}

	public static
	String printVector(final Vector3d v, final int scale) {
		return String.format("(%.1f,%.1f,%.1f)",scale*v.x,scale*v.y,scale*v.z);
	}

	/** if a caller want a sorter to export some "graphics" (as nodes and/or lines),
	 * this method is the way to do it... */
	public void exportDebugGraphics(final GraphExportable ge) {
		/* empty */
	}
}
