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
package cz.it4i.ulman.transfers;

import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;

public class DemoBlenderSending {
	public static void main(String[] args) throws InterruptedException {
		BlenderSendingUtils.BlenderConnectionHandle connA = BlenderSendingUtils.connectToBlender("localhost:9083", "demoMastodonClient");
		BlenderSendingUtils.BlenderConnectionHandle connB = BlenderSendingUtils.connectToBlender("localhost:9084", "leadsNowhereClient");

		System.out.println(BlenderSendingUtils.reportConnections());

		System.out.println("ConnB is closed? "+connB.isConnectionClosed());
		System.out.println("closing ConnB");
		connB.closeConnection();
		System.out.println("ConnB is closed? "+connB.isConnectionClosed());

		System.out.println(BlenderSendingUtils.reportConnections());
		System.out.println("left connection should be: "+connA.url);

		System.out.println("//removing not needed connections");
		//BlenderSendingUtils.closeNotNeededConnections();
		System.out.println(BlenderSendingUtils.reportConnections());
		System.out.println("left connection should be: "+connA.url);
		System.out.println("identifing itself as: "+connA.clientIdObj.getClientName());

		connA.sendInitialIntroHandshake();
		connA.commBlocking.showMessage( BucketsWithGraphics.SignedTextMessage
				.newBuilder()
				.setClientID( connA.clientIdObj )
				.setClientMessage( BucketsWithGraphics.TextMessage
						.newBuilder()
						.setMsg("hello Blender from A")
						.build() )
				.build()
		);

		connB = BlenderSendingUtils.connectToBlender("localhost:9083", "demoMastodonClient BBB");
		connB.sendInitialIntroHandshake("localhost:3809");
		System.out.println(BlenderSendingUtils.reportConnections());

		connA.commBlocking.showMessage( BucketsWithGraphics.SignedTextMessage
				.newBuilder()
				.setClientID( connA.clientIdObj )
				.setClientMessage( BucketsWithGraphics.TextMessage
						.newBuilder()
						.setMsg("hello Blender from AAA")
						.build() )
				.build()
		);

		connB.commBlocking.showMessage( BucketsWithGraphics.SignedTextMessage
				.newBuilder()
				.setClientID( connB.clientIdObj )
				.setClientMessage( BucketsWithGraphics.TextMessage
						.newBuilder()
						.setMsg("hello Blender from BBB")
						.build() )
				.build()
		);

		System.out.println(BlenderSendingUtils.reportConnections());
		connA.closeConnection();
		System.out.println(BlenderSendingUtils.reportConnections());
	}
}
