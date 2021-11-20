package cz.it4i.ulman.transfers.graphexport.leftrightness;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.joml.Vector3d;
import org.mastodon.mamut.model.Spot;

import java.util.Comparator;

public class PolesSorter extends AbstractDescendantsSorter {
	/** centre position (around which the lineage "revolves"), the centre
	 *  needs not to sit on the south-to-north oriented axis */
	final Vector3d centre;

	/** the south-to-north reference oriented axis */
	final Vector3d axisUp;

	/** see PoleSorter c'tor docs... if the angle between the triangle's and observer's
	 *  up vectors is insidre [ lrTOupThresholdAngleDeg; 180-lrTOupThresholdAngleDeg ]
	 *  then up/down relation is investigated (instead of left/right) */
	public double lrTOupThresholdAngleDeg = 60;

	/** user param: what maximal incidence angle can there be between a vector from daughter1
	 *  to this.centre and a vector from daughter1 to daughter2 so that daughter2 will be declared
	 *  to be moving *towards* the centre (establishing another cell level) rather than dividing
	 *  into a side-by-side configuration (within animal surface) */
	public double layeringLowerCutoffAngleDeg = 30;

	/** user param: quite similar to this.layeringLowerCutoffAngleDeg but to declare that daughter2
	 *  is moving *outwards* the centre */
	public double layeringUpperCutoffAngleDeg = 150; //NB: 150 = 180-30

	/** south and north positions together define a south-to-north oriented axis
	 *  that serves as a reference for observer's up-vector, the daughters and
	 *  the centre position form a triangle and thus a plane in which, using the
	 *  up-vector and looking at the centre, one can tell which daughter is left/right;
	 *  if, however, the angle between this plane's normal vector and the up-vector is
	 *  larger than 'lrTOupThresholdAngleDeg' the sense becomes more down/up than left/right
	 *  and the decision is made accordingly, the bottom cell is said to be left */
	public PolesSorter(final Spot spotAtCentre, final Spot spotSouth, final Spot spotNorth)
	{
		centre = createVector3d(spotAtCentre);
		axisUp = createVector3d(spotNorth).sub(createVector3d(spotSouth)).normalize();

		//memorize for this.exportDebugGraphics()
		this.spotSouth = spotSouth;
		this.spotNorth = spotNorth;

		final double radToDegFactor = 180.0 / Math.PI;

		this.comparator = new Comparator<Spot>() {
			@Override
			public int compare(Spot d1, Spot d2) {
				if (d1.equals(d2)) return 0;

				final Vector3d d1pos = createVector3d(d1);
				final Vector3d d2pos = createVector3d(d2);

				//super useful shortcuts...
				final Vector3d d1tod2 = new Vector3d(d2pos).sub(d1pos).normalize();
				final Vector3d d1toc  = new Vector3d(centre).sub(d1pos).normalize();

				//layering:
				//
				//check the angle between d1->centre and d1->d2,
				//does it carry a sign of starting two layers?
				double angle_d2d1c_deg = Math.acos( d1toc.dot(d1tod2) ) *radToDegFactor;
				//d2 is closer to centre than d1
				if (angle_d2d1c_deg <= layeringLowerCutoffAngleDeg) return +1;
				//d1 is closer to centre than d2
				else if (angle_d2d1c_deg >= layeringUpperCutoffAngleDeg) return -1;
				//NB: tree of a daughter closer to the centre is drawn first (in left)

				//side-by-side configuration:
				//
				//consider a triangle/plane given by d1,d2 and c
				final Vector3d triangleUp = new Vector3d(d1tod2).cross(d1toc).normalize();

				//angle between triangle's normal and up-vector (south-to-north axis)
				double angle_upsDiff_deg = Math.acos( triangleUp.dot(axisUp) ) *radToDegFactor;
				if (angle_upsDiff_deg < lrTOupThresholdAngleDeg)
				{
					//left-right case, up vectors are nearly parallel
					//d1 is left d2
					return -1;
				}
				else if (angle_upsDiff_deg > (180-lrTOupThresholdAngleDeg))
				{
					//left-right case, up vectors are nearly opposite
					//d1 is right d2
					return +1;
				}

				//up-down case                 down  up
				return d1tod2.dot(axisUp) > 0 ? -1 : +1;
			}
		};
	}

	Vector3d createVector3d(final Spot s)
	{
		return new Vector3d( s.getDoublePosition(0), s.getDoublePosition(1), s.getDoublePosition(2) );
	}

	@Override
	public void exportDebugGraphics(final GraphExportable ge)
	{
		ge.addNode("Centre","centre at "+printVector(centre), 0, 0,0);
		ge.addNode("South","south at "+printVector(createVector3d(spotSouth),1), 0, 0,0);
		ge.addNode("North","north at "+printVector(createVector3d(spotNorth),1), 0, 0,0);
	}
	private final Spot spotSouth,spotNorth;
}
