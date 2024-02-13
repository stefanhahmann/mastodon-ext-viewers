/*-
 * #%L
 * Online Mastodon Exports
 * %%
 * Copyright (C) 2021 - 2024 Vladimír Ulman
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package cz.it4i.ulman.transfers.graphexport.leftrightness;

import cz.it4i.ulman.transfers.graphexport.GraphExportable;
import org.joml.Vector3d;
import static cz.it4i.ulman.transfers.graphexport.Utils.createVector3d;

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

	public TriangleSorter(final Vector3d posCentre, final Vector3d posA, final Vector3d posB)
	{
		centre = new Vector3d(posCentre); //NB: own copy!
		axisA = new Vector3d(posA).sub(centre).normalize();
		axisB = new Vector3d(posB).sub(centre).normalize();
		axisC = new Vector3d(axisA).cross(axisB).normalize();

		final double radToDegFactor = 180.0 / Math.PI;

		this.comparator = (d1, d2) -> {
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
			AxisName bestAxis = AxisName.NONE;
			boolean positiveDirOfBestAxis = true;

			double angle = Math.acos( axisC.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.C_ZZ;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.C_ZZ;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
			}

			angle = Math.acos( axisB.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.B_YY;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.B_YY;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
			}

			angle = Math.acos( axisA.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.A_XX;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.A_XX;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
			}

			System.out.println("Best axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")+bestAxis);
			System.out.println("Best angle: "+bestParallelAng*radToDegFactor+" deg");
			//NB: super.log might not be set here (in this non-verbose regime)

			return positiveDirOfBestAxis? -1 : +1;
		};


		this.verboseComparator = (d1, d2) -> {
			log.info("Comparing between: "+d1.getLabel()+" and "+d2.getLabel());
			if (d1.equals(d2)) {
				log.info("... which are at the same position");
				return 0;
			}

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
			if (angle_d2d1c_deg <= layeringLowerCutoffAngleDeg) {
				log.info("  "+d1.getLabel()+" is outer->right of "+d2.getLabel());
				return +1;
			}
			//d1 is closer to centre than d2
			else if (angle_d2d1c_deg >= layeringUpperCutoffAngleDeg) {
				log.info("  "+d1.getLabel()+" is inner->left of "+d2.getLabel());
				return -1;
			}
			//NB: tree of a daughter closer to the centre is drawn first (in left)

			log.info("  layering angle: "+angle_d2d1c_deg);

			//side-by-side configuration:
			//
			//consider a triangle/plane given by d1,d2 and c,
			//to tell if d1 is left from d2 within this plane, we need an "up" vector
			//'cause left-right gets reversed if you're up-side-down
			//
			//for the outside global observer (as if standing on the ground), the left wing of
			//a rolled-over plane is in fact the plane's right wing (plane's local un-anchored view)
			final Vector3d triangleUp = new Vector3d(d1tod2).cross(d1toc).normalize();

			log.info("  tUP: "+printVector(triangleUp,100));

			//find the most parallel axis (aka the most relevant anchor) to the triangle's up vector
			double bestParallelAng = Math.PI / 2.0;
			AxisName bestAxis = AxisName.NONE;
			boolean positiveDirOfBestAxis = true;

			double angle = Math.acos( axisC.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.C_ZZ;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.C_ZZ;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}

			angle = Math.acos( axisB.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.B_YY;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.B_YY;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}

			angle = Math.acos( axisA.dot(triangleUp) );
			if (angle < bestParallelAng) {
				bestAxis = AxisName.A_XX;
				positiveDirOfBestAxis = true;
				bestParallelAng = angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}
			if (angle > (Math.PI-bestParallelAng)) {
				bestAxis = AxisName.A_XX;
				positiveDirOfBestAxis = false;
				bestParallelAng = Math.PI - angle;
				log.info("  better axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")
						+bestAxis+"   (angle "+angle*radToDegFactor+" deg)");
			}

			log.info("  best axis: "+(positiveDirOfBestAxis ? "positive ":"negative ")+bestAxis);
			log.info("  best angle: "+bestParallelAng*radToDegFactor+" deg");

			log.info("= "+d1.getLabel()+(positiveDirOfBestAxis ? " left of ":" right of ")+d2.getLabel());
			return positiveDirOfBestAxis? -1 : +1;
		};
	}

	public enum AxisName { A_XX,B_YY,C_ZZ,NONE };

	@Override
	public void exportDebugGraphics(final GraphExportable ge)
	{
		ge.addNode("Centre","centre at "+printVector(centre), 0, 0,0);
		ge.addNode("A","A at "+printVector(axisA,100), 0, 0,0);
		ge.addNode("B","B at "+printVector(axisB,100), 0, 0,0);
		ge.addNode("C","C at "+printVector(axisC,100), 0, 0,0);
	}
}
