package cz.it4i.ulman.transfers.graphics;

import cz.it4i.ulman.transfers.graphics.protocol.BucketsWithGraphics;
import io.grpc.stub.StreamObserver;

public class EmptyIgnoringStreamObservers implements StreamObserver<BucketsWithGraphics.Empty> {
	@Override
	public void onNext(BucketsWithGraphics.Empty empty) { /* EMPTY */ }

	@Override
	public void onError(Throwable throwable) { /* EMPTY */ }

	@Override
	public void onCompleted() { /* EMPTY */ }
}