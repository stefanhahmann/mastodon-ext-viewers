package cz.it4i.ulman.transfers.graphexport.leftrightness;

import org.mastodon.collection.RefList;
import org.mastodon.mamut.model.Spot;

public interface DescendantsSorter {
	/** Sorts, by applying certain criterion, the descendant spots (daughters) within the given list. */
	void sort(final RefList<Spot> listOfDaughters);
}
