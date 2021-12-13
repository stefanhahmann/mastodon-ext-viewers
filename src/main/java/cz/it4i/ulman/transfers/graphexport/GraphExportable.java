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
package cz.it4i.ulman.transfers.graphexport;

public interface GraphExportable
{
	int get_xColumnWidth();
	int get_yLineStep();
	int get_defaultBendingPointAbsoluteOffsetY();
	int get_defaultNodeWidth();
	int get_defaultNodeHeight();
	int get_defaultNodeColour();

	void set_xColumnWidth(int val);
	void set_yLineStep(int val);
	void set_defaultBendingPointAbsoluteOffsetY(int val);
	void set_defaultNodeWidth(int val);
	void set_defaultNodeHeight(int val);
	void set_defaultNodeColour(int val);

	/** adds node whose color should have a meaning
	    when read in hexadecimal 0xRRGGBB format; default
	    width and height of the node graphics shall be used */
	void addNode(final String id,
	             final String label, final int colorRGB,
	             final int x, final int y);

	/** adds node whose color should have a meaning
	    when read in hexadecimal 0xRRGGBB format */
	void addNode(final String id,
	             final String label, final int colorRGB,
	             final int x, final int y,
	             final int width, final int height);


	void addStraightLine(final String fromId, final String toId);

	/** adds a new node and connects the given parent with this node;
	    default size of the node is expected */
	void addStraightLineConnectedVertex(final String parentNodeID,
	                                    final String newNodeID,
	                                    final String label, final int colorRGB,
	                                    final int x, final int y);


	/** adds bended line where the bending shall happen at
	    [toX + defaultNodeWidth/2 , toY + defaultBendingPointAbsoluteOffsetY ] */
	void addBendedLine(final String fromId, final String toId,
	                   final int toX, final int toY);

	/** adds bended line where the bending shall happen at
	    [toX + defaultNodeWidth/2 , toY + bendingOffsetY ] */
	void addBendedLine(final String fromId, final String toId,
	                   final int toX, final int toY, final int bendingOffsetY);

	/** adds a new node and connects the given parent with this node;
	    default size of the node is expected as well as default bending point */
	void addBendedLineConnectedVertex(final String parentNodeID,
	                                  final String newNodeID,
	                                  final String label, final int colorRGB,
	                                  final int x, final int y);

	/** optional, whether to leave empty or implement depends on the underlying export mechanism */
	void close();

	/*
	static void runExample()
	{
		System.out.println("graph export example started");

		//the main root of the tree
		addNode("A", "A",defaultNodeColour, 200,0);

		//left subtree: straight lines
		addStraightLineConnectedVertex("A" , "AL" , "AL" ,defaultNodeColour, 100,200,0);
		addStraightLineConnectedVertex("AL", "ALL", "ALL",defaultNodeColour,  50,400,0);
		addStraightLineConnectedVertex("AL", "ALR", "ALR",defaultNodeColour, 150,400,0);

		//right subtree: bended lines
		addBendedLineConnectedVertex( "A" , "AR" , "AR" ,defaultNodeColour, 300,200,0);
		addBendedLineConnectedVertex( "AR", "ARL", "ARL",defaultNodeColour, 250,400,0);
		addBendedLineConnectedVertex( "AR", "ARR", "ARR",defaultNodeColour, 350,400,0);

		System.out.println("graph export example stopped");
	}
	*/
}
