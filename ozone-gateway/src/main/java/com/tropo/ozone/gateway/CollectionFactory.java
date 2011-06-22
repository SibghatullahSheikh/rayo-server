package com.tropo.ozone.gateway;

import java.util.Collection;

public interface CollectionFactory<U extends Collection<V>, V> {
	U newCollection ();
	U newCollection (Collection<V> collection);
}
