package cz.it4i.ulman.transfers.graphexport;

import org.joml.Vector3d;
import org.mastodon.mamut.model.Spot;

public class Utils {
	public static Vector3d createVector3d(final Spot s)
	{
		return new Vector3d( s.getDoublePosition(0), s.getDoublePosition(1), s.getDoublePosition(2) );
	}
}
