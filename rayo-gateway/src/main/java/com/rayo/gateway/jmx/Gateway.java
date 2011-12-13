package com.rayo.gateway.jmx;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.rayo.gateway.GatewayServlet;
import com.rayo.gateway.GatewayStorageService;
import com.rayo.gateway.util.JIDUtils;

/**
 * <p>This Mbean exposes relevant information on the Distributed hash table. It 
 * can be used to find calls being managed by the gateway, registered client 
 * resources, registered rayo nodes, etc. </p>
 * 
 * @author martin
 *
 */
@ManagedResource(objectName="com.rayo.gateway:Type=Gateway", description="Rayo Gateway")
public class Gateway implements GatewayMXBean {
	
	private GatewayStorageService gatewayStorageService;
	private GatewayServlet gatewayServlet;

	@Override
	@ManagedAttribute(description="Platforms")
	public List<Platform> getPlatforms() {
		
		List<Platform> platforms = new ArrayList<Platform>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			platforms.add(new Platform(platform));
		}
		return platforms;
	}	

	@Override
	@ManagedAttribute(description="Nodes")
	public List<Node> getRayoNodes() {
		
		List<Node> nodes = new ArrayList<Node>();
		for(String platform: gatewayStorageService.getRegisteredPlatforms()) {
			for (String jid: gatewayStorageService.getRayoNodes(platform)) {
				Node node = new Node(jid);
				node.setGatewayStorageService(gatewayStorageService);
				if (!nodes.contains(node)) {
					nodes.add(node);
					node.addPlatform(platform);					
				} else {
					nodes.get(nodes.indexOf(node)).addPlatform(platform);
				}
			}
		}

		return nodes;
	}	
	
	@Override
	@ManagedAttribute(description="ClientApplications")
	public List<ClientApplication> getClientApplications() {
		
		List<ClientApplication> clients = new ArrayList<ClientApplication>();

		for(String jid: gatewayStorageService.getClientResources()) {
			String bareJid = JIDUtils.getBareJid(jid);			
			ClientApplication client = new ClientApplication(bareJid);
			if (!clients.contains(client)) {
				client.addResources(gatewayStorageService.getResourcesForClient(bareJid));
				clients.add(client);
			}
		}

		return clients;
	}	
	
	@ManagedOperation(description = "Returns call information")
	public Call callInfo(String callId) {
		
		String rayoNode = gatewayStorageService.getRayoNode(callId);
		String clientJID = gatewayStorageService.getclientJID(callId);
		
		return new Call(callId, rayoNode, clientJID);
	}

	@ManagedOperation(description = "Bans an aplication from the gateway")
	public void ban(String jid) {
		
		gatewayServlet.ban(jid);
	}

	@ManagedOperation(description = "Unbans an aplication from the gateway")
	public void unban(String jid) {
		
		gatewayServlet.unban(jid);
	}
	
	public void setGatewayStorageService(GatewayStorageService gatewayStorageService) {
		this.gatewayStorageService = gatewayStorageService;
	}

	public void setGatewayServlet(GatewayServlet gatewayServlet) {
		this.gatewayServlet = gatewayServlet;
	}	
}
