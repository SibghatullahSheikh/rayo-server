package com.rayo.gateway.jmx;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.GatewayDatastore;
import com.voxeo.servlet.xmpp.JID;

@ManagedResource(objectName="com.rayo.gateway:Type=GatewayStatistics", description="Gateway Statistics")
public class GatewayStatistics implements GatewayStatisticsMXBean {

	private GatewayDatastore gatewayDatastore;
	
	private AtomicLong totalCallsCount = new AtomicLong();
	private AtomicLong messagesCount = new AtomicLong();
	private AtomicLong errorsCount = new AtomicLong();
	
	private Set<String> clients = new HashSet<String>();
	private Set<String> resources = new HashSet<String>();
	
	@Override
	@ManagedAttribute(description="Active Calls")	
	public long getActiveCallsCount() {

		long total = 0;
		for (JID client : gatewayDatastore.getClientResources()) {
			total+= gatewayDatastore.getCalls(client).size();
		}
		return total;
	}

	@Override
	@ManagedAttribute(description="Total Calls")	
	public long getTotalCallsCount() {

		return totalCallsCount.longValue();
	}

	@Override
	@ManagedAttribute(description="Active Clients")	
	public long getActiveClientsCount() {

		return gatewayDatastore.getClientResources().size();
	}

	@Override
	@ManagedAttribute(description="Total Clients")	
	public long getTotalClientsCount() {

		return clients.size();
	}

	@Override
	@ManagedAttribute(description="Total Client Resources")	
	public long getTotalClientResourcesCount() {

		return resources.size();
	}
	
	@Override
	@ManagedAttribute(description="Active Rayo Nodes")	
	public long getActiveRayoNodesCount() {

		long total = 0;
		for (String platform : gatewayDatastore.getRegisteredPlatforms()) {
			total+= gatewayDatastore.getRayoNodes(platform).size();
		}
		return total;
	}

	@Override
	@ManagedAttribute(description="Total Messages")	
	public long getMessagesCount() {

		return messagesCount.longValue();
	}

	@Override
	@ManagedAttribute(description="Total Errors")	
	public long getErrorsCount() {

		return errorsCount.longValue();
	}
	
	public void clientRegistered(JID jid) {
		
		clients.add(jid.getBareJID().toString());
		resources.add(jid.toString());
	}
	
	public void messageProcessed() {
		
		messagesCount.incrementAndGet();
	}
	
	public void errorProcessed() {
		
		errorsCount.incrementAndGet();
	}
	
	public void callRegistered() {
		
		totalCallsCount.incrementAndGet();
	}

	public void setGatewayDatastore(GatewayDatastore gatewayDatastore) {
		this.gatewayDatastore = gatewayDatastore;
	}
}