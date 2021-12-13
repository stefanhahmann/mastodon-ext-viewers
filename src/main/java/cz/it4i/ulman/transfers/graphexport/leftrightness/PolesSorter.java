/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019, Vladimír Ulman
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
import static cz.it4i.ulman.transfers.graphexport.Utils.createVector3d;

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
	public PolesSorter(final Vector3d posCentre, final Vector3d posSouth, final Vector3d posNorth)
	{
		centre = new Vector3d(posCentre); //NB: own copy!
		axisUp = new Vector3d(posNorth).sub(posSouth).normalize();

		//memorize for this.exportDebugGraphics()
		spotSouth = new Vector3d(posSouth); //NB: own copy!
		spotNorth = new Vector3d(posNorth);

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
			//consider a triangle/plane given by d1,d2 and c
			final Vector3d triangleUp = new Vector3d(d1tod2).cross(d1toc).normalize();

			log.info("  tUP: "+printVector(triangleUp,100));

			//angle between triangle's normal and up-vector (south-to-north axis)
			double angle_upsDiff_deg = Math.acos( triangleUp.dot(axisUp) ) *radToDegFactor;
			if (angle_upsDiff_deg < lrTOupThresholdAngleDeg)
			{
				log.info("  parallel (diff: "+angle_upsDiff_deg+" deg): "
						+d1.getLabel()+" is left of "+d2.getLabel());

				//left-right case, up vectors are nearly parallel
				//d1 is left d2
				return -1;
			}
			else if (angle_upsDiff_deg > (180-lrTOupThresholdAngleDeg))
			{
				log.info("  opposite (diff: "+(180-angle_upsDiff_deg)+" deg): "
						+d1.getLabel()+" is right of "+d2.getLabel());

				//left-right case, up vectors are nearly opposite
				//d1 is right d2
				return +1;
			}

			//up-down case
			log.info("  perpendicularity (abs ang: "+angle_upsDiff_deg+" deg), would have said: "
					+d1.getLabel()+" is "+(angle_upsDiff_deg < 90? "left":"right")+" of "+d2.getLabel());
			double upDownAngle = d1tod2.dot(axisUp);
			if (upDownAngle > 0) {
				//down
				upDownAngle = Math.acos(upDownAngle) * radToDegFactor;
				log.info("  same orientation (ang: "+upDownAngle+" deg): "
						+d1.getLabel()+" is down/left of "+d2.getLabel());
				return -1;
			} else {
				//up
				upDownAngle = Math.acos(upDownAngle) * radToDegFactor;
				log.info("  opposite orientation (ang: "+(180-upDownAngle)+" deg): "
						+d1.getLabel()+" is up/right of "+d2.getLabel());
				return +1;
			}
		};
	}

	@Override
	public void exportDebugGraphics(final GraphExportable ge)
	{
		ge.addNode("Centre","centre at "+printVector(centre), 0, 0,0);
		ge.addNode("South","south at "+printVector(spotSouth,1), 0, 0,0);
		ge.addNode("North","north at "+printVector(spotNorth,1), 0, 0,0);
	}
	private final Vector3d spotSouth,spotNorth;
}
