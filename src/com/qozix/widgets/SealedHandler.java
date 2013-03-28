package com.qozix.widgets;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

public abstract class SealedHandler<T> extends Handler {

	private final WeakReference<T> reference;

	public SealedHandler( T entity ) {
		super();
		reference = new WeakReference<T>( entity );
	}

	@Override
	public final void handleMessage( Message message ) {
		final T entity = reference.get();
		if ( entity != null ) {
			handleMessage( message, entity );
		}
	}

	public abstract void handleMessage( Message message, T entity );

}