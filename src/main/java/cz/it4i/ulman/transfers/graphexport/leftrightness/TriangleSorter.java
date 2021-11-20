package cz.it4i.ulman.transfers.graphexport.leftrightness;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.joml.Vector3d;
import org.mastodon.mamut.model.Spot;

import java.util.Comparator;

public class TriangleSorter extends AbstractDescendantsSorter {
	/** centre position (around which the lineage "revolves") */
	final Vector3d centre;

	/** user-given reference spots give rise to these (hopefully) quasi-orthogonal
	 * "up axes", that said, show positive orientation of some two axes (that are
	 * likely representing some intrinsic axes of the tracked data/animal) */
	final Vector3d axisA, axisB;

	/** cross product of axisA x axisB */
	final Vector3d axisC;

	/** user param: what maximal incidence angle can there be between a vector from daughter1
	 * to this.centre and a vector from daughter1 to daughter2 so that daughter2 will be declared
	 * to be moving *towards* the centre (establishing another cell level) rather than dividing
	 * into a side-by-side configuration (within animal surface) */
	public double layeringLowerCutoffAngleDeg = 30;

	/** user param: quite similar to this.layeringLowerCutoffAngleDeg but to declare that daughter2
	 * is moving *outwards* the centre */
	public double layeringUpperCutoffAngleDeg = 150; //NB: 150 = 180-30

	public TriangleSorter(final Spot spotAtCentre, final Spot spotA, final Spot spotB)
	{
		centre = createVector3d(spotAtCentre);
		axisA = createVector3d(spotA).sub(centre).normalize();
		axisB = createVector3d(spotB).sub(centre).normalize();
		axisC = new Vector3d(axisA).cross(axisB).normalize();

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
				//consider a triangle/plane given by d1,d2 and c,
				//to tell if d1 is left from d2 within this plane, we need an "up" vector
				//'cause left-right gets reversed if you're up-side-down
				//
				//for the outside global observer (as if standing on the ground), the left wing of
				//a rolled-over plane is in fact the plane's right wing (plane's local un-anchored view)
				final Vector3d triangleUp = new Vector3d(d1tod2).cross(d1toc).normalize();

				//find the most parallel axis (aka the most relevant anchor) to the triangle's up vector
				double bestParallelAng = Math.PI / 2.0;
				int bestAxis = -1;
				boolean positiveDirOfBestAxis = true;

				double angle = Math.acos( axisC.dot(triangleUp) );
				if (angle < bestParallelAng) {
					bestAxis = 2;
					positiveDirOfBestAxis = true;
					bestParallelAng = angle;
				}
				if (angle > (Math.PI-bestParallelAng)) {
					bestAxis = 2;
					positiveDirOfBestAxis = false;
					bestParallelAng = Math.PI - angle;
				}

				angle = Math.acos( axisB.dot(triangleUp) );
				if (angle < bestParallelAng) {
					bestAxis = 1;
					positiveDirOfBestAxis = true;
					bestParallelAng = angle;
				}
				if (angle > (Math.PI-bestParallelAng)) {
					bestAxis = 1;
					positiveDirOfBestAxis = false;
					bestParallelAng = Math.PI - angle;
				}

				angle = Math.acos( axisA.dot(triangleUp) );
				if (angle < bestParallelAng) {
					bestAxis = 0;
					positiveDirOfBestAxis = true;
					bestParallelAng = angle;
				}
				if (angle > (Math.PI-bestParallelAng)) {
					bestAxis = 0;
					positiveDirOfBestAxis = false;
					bestParallelAng = Math.PI - angle;
				}

				System.out.println("Best axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")+bestAxis);
				System.out.println("Best angle: "+bestParallelAng*radToDegFactor+" deg");

				return positiveDirOfBestAxis? -1 : +1;
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
		ge.addNode("A","A at "+printVector(axisA,100), 0, 0,0);
		ge.addNode("B","B at "+printVector(axisB,100), 0, 0,0);
		ge.addNode("C","C at "+printVector(axisC,100), 0, 0,0);
	}
}
