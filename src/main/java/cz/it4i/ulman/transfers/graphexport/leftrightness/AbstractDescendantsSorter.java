package cz.it4i.ulman.transfers.graphexport.leftrightness;

import org.mastodon.collection.RefList;
import org.mastodon.mamut.model.Spot;
import java.util.Comparator;

public class AbstractDescendantsSorter implements DescendantsSorter {
	@Override
	public void sort(RefList<Spot> listOfDaughters) {
		listOfDaughters.sort(comparator);
	}

	/** default behaviour that compares using spots' labels */
	Comparator<Spot> comparator = Comparator.comparing(Spot::getLabel);
}
