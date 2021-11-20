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
