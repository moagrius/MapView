package com.qozix.mapview.viewmanagers;

import android.content.Context;
import android.view.View;

public abstract class ViewFactory<E extends View> {
	public abstract E fetch( Context c );
}