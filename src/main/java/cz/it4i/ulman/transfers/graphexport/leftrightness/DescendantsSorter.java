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

import org.mastodon.collection.RefList;
import org.mastodon.mamut.model.Spot;
import org.scijava.log.Logger;

public interface DescendantsSorter {
	/** Sorts, by applying certain criterion, the descendant spots (daughters) within the given list. */
	void sort(final RefList<Spot> listOfDaughters);

	/** Sorts, by applying certain criterion, the descendant spots (daughters) within the given list.
	 * The process of sorting can be documented (debugged) using the logger. This functionality is,
	 * however, meant as optional extension, which is why the default implementation ignores the logger.
	 *
	 * The caller chooses between normal or verbose mode by calling the appropriate method (rather than
	 * providing an "empty logger"). The implementer may (but needs not) prepare a true verbose variant.
	 */
	default void sort(final RefList<Spot> listOfDaughters, final Logger debugLogger) {
		sort(listOfDaughters);
	}
}
