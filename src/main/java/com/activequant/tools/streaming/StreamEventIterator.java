package com.activequant.tools.streaming;

import java.util.Iterator;


public abstract class StreamEventIterator<T extends StreamEvent> implements Iterator<StreamEvent> {

	@Override
	public void remove() {
	}

}
