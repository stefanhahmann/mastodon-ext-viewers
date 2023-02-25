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
