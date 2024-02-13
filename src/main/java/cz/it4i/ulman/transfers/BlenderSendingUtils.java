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
import cz.it4i.ulman.transfers.graphics.protocol.ClientToServerGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BlenderSendingUtils {
	static public class BlenderConnectionHandle {
		final public String url;

		private boolean isConnectionClosed = true;
		public boolean isConnectionClosed() {
			return isConnectionClosed;
		}

		private BlenderConnectionHandle()
		{
			//prevent from creating outside & silence the compiler
			url = null;
			commContinuous = null;
			commBlocking = null;
			clientIdObj = null;
		}

		private BlenderConnectionHandle(final String url, final String clientName,
		                                final ManagedChannel reuseThisChannel) {
			this.url = url;

			commContinuous = ClientToServerGrpc.newStub(reuseThisChannel);
			commBlocking = ClientToServerGrpc.newBlockingStub(reuseThisChannel);

			clientIdObj = BucketsWithGraphics.ClientIdentification.newBuilder()
					.setClientName(clientName)
					.build();

			isConnectionClosed = false;
		}

		public void sendInitialIntroHandshake() {
			sendInitialIntroHandshake("");
			//NB: this signals "please, send NO feedback"
		}

		public void sendInitialIntroHandshake(final String feedbackLocalUrl) {
			if (isConnectionClosed) return;
			commBlocking.introduceClient( BucketsWithGraphics.ClientHello
					.newBuilder()
					.setClientID( clientIdObj )
					.setReturnURL( feedbackLocalUrl )
					.build()
			);
		}

		public void closeConnection() {
			SINGLETON.deregisterChannelUsage(url);
			isConnectionClosed = true;
		}

		public final ClientToServerGrpc.ClientToServerStub commContinuous;
		public final ClientToServerGrpc.ClientToServerBlockingStub commBlocking;
		public final BucketsWithGraphics.ClientIdentification clientIdObj;
	}


	//the main connection-establishing/reusing method
	static public BlenderConnectionHandle connectToBlender(final String url, final String clientName) {
		return new BlenderConnectionHandle(url,clientName, SINGLETON.registerChannelUsage(url));
	}

	static void closeChannel(final ManagedChannel channel)
	throws InterruptedException {
		closeChannel(channel, 15, 2);
	}

	static void closeChannel(final ManagedChannel channel,
	                         final int halfOfMaxWaitTimeInSeconds,
	                         final int checkingPeriodInSeconds)
	throws InterruptedException {
		//first, make sure the channel describe itself as "READY"
		int timeSpentWaiting = 0;
		while (channel.getState(false) != ConnectivityState.READY && timeSpentWaiting < halfOfMaxWaitTimeInSeconds) {
			timeSpentWaiting += checkingPeriodInSeconds;
			Thread.sleep(checkingPeriodInSeconds * 1000L); //seconds -> milis
		}
		//but even when it claims "READY", it still needs some grace time to finish any commencing transfers;
		//request it to stop whenever it can, then keep asking when it's done
		channel.shutdown();
		timeSpentWaiting = 0;
		while (!channel.isTerminated() && timeSpentWaiting < halfOfMaxWaitTimeInSeconds) {
			timeSpentWaiting += checkingPeriodInSeconds;
			Thread.sleep(checkingPeriodInSeconds * 1000L); //seconds -> milis
		}
		//last few secs extra before a hard stop (if it is still not yet closed gracefully)
		channel.awaitTermination(checkingPeriodInSeconds, TimeUnit.SECONDS);
	}


	// ---------------------- internals ----------------------
	//the single (and invisible) memory of this whole circus
	private static final BlenderSendingUtils SINGLETON = new BlenderSendingUtils();

	//internal data of the singleton
	private final Map<String, ManagedChannel> openedChannels = new HashMap<>(5);
	private final Map<String, Integer> noOfOpenedChannelsUsers = new HashMap<>(5);

	/** increases usage counter; and if the first use of it is detected,
	 * the channel gets created and connected */
	private ManagedChannel registerChannelUsage(final String url) {
		if (!openedChannels.containsKey(url)) {
			openedChannels.put(url, ManagedChannelBuilder.forTarget(url).usePlaintext().build());
		}
		noOfOpenedChannelsUsers.put(url, noOfOpenedChannelsUsers.getOrDefault(url, 0)+1);
		return openedChannels.get(url);
	}

	/** decreases usage counter; and if it drops to zero afterwards,
	 * the underlying channel is still kept alive */
	private void deregisterChannelUsage(final String url) {
		if (noOfOpenedChannelsUsers.getOrDefault(url,0) == 0) return;
		noOfOpenedChannelsUsers.put(url, noOfOpenedChannelsUsers.get(url)-1);
	}

	public static String reportConnections() {
		final StringBuilder sb = new StringBuilder("Known connections:");
		for (String url : SINGLETON.openedChannels.keySet())
			sb.append("\n  url "+url+" is currently used "+SINGLETON.noOfOpenedChannelsUsers.get(url)+" times");
		return sb.toString();
	}

	/** find not-used channels (counter equals to zero), close them and remove them the management */
	public static void closeNotNeededConnections()
	throws InterruptedException {
		final Iterator<String> urls = SINGLETON.openedChannels.keySet().iterator();
		while (urls.hasNext()) {
			String url = urls.next();
			if (SINGLETON.noOfOpenedChannelsUsers.get(url) == 0) {
				closeChannel(SINGLETON.openedChannels.get(url));
				SINGLETON.noOfOpenedChannelsUsers.remove(url);
				urls.remove(); //essentially the same as SINGLETON.openedChannels.remove(url);
			}
		}
	}

	@Override
	protected void finalize() {
		//"nullify" all channels so that they could get closed
		noOfOpenedChannelsUsers.replaceAll((url, cnt) -> 0);
		try {
			closeNotNeededConnections();
		}
		catch (InterruptedException e)
		{ /* no time to care here... */ }
	}
}
