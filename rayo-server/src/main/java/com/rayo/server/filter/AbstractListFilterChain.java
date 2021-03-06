package com.rayo.server.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Base class for a filter chain. A filter chain will concatenate multiple 
 * message filters. Therefore, when executing a filter on the filter chain, 
 * the chain will take care of executing all the individual filters.</p> 
 * 
 * @author martin
 *
 */
abstract class AbstractListFilterChain implements FilterChain {
	
	List<MessageFilter> filters = new ArrayList<MessageFilter>();
		
	@Override
	public List<MessageFilter> getFilters() {
		
		return new ArrayList<MessageFilter>(filters);
	}

	public void setFilters(List<MessageFilter> filters) {
		this.filters = filters;
	}
	
	@Override
	public void addFilter(int index, MessageFilter filter) {
		
		filters.add(index,filter);
	}
	
	@Override
	public void addFilter(MessageFilter filter) {
	
		filters.add(filter);
	}
	
	@Override
	public void clear() {
		
		filters.clear();
	}
	
	@Override
	public void removeFilter(MessageFilter filter) {
		
		filters.remove(filter);
	}

}
