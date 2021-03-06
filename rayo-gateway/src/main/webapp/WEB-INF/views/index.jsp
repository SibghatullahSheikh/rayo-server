<%@ page import="com.rayo.gateway.web.*,com.rayo.server.*,com.rayo.server.web.*,com.rayo.server.admin.*,com.rayo.gateway.admin.*,com.rayo.server.jmx.*,com.rayo.gateway.jmx.*" %>
<%@page import="org.springframework.context.ApplicationContext"%>
		
<%
	if (getServletConfig().getServletContext().getAttribute(ContextLoaderListener.RAYO_STATUS) == RayoStatus.FAILED) {
	    response.sendError(500);
    }
    ApplicationContext context = (ApplicationContext)application.getAttribute(SpringXmppServlet.APPLICATION_CONTEXT);        
	GatewayAdminService adminService = (GatewayAdminService)context.getBean("adminService");
	GatewayStatistics gatewayStatistics = (GatewayStatistics)context.getBean("gatewayStatistics");
	Info info = (Info)context.getBean("infoBean");
	Gateway gateway = (Gateway)context.getBean("gatewayBean");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"> 
<html> 
<head> 
	<title>Rayo Gateway - Dashboard</title>
	<link href="css/rayo.css" rel="stylesheet" type="text/css">  
	<link rel="stylesheet" type="text/css" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/themes/start/jquery-ui.css"/>
</head> 
<body class="home"> 
  <div id="header">
    <div id="header-content">
      <div id="logo"></div>
    </div>
  </div>
  <div id="main">
	<div class="dashboard">
	  <div class="dashboard-content">
	    <div class="dashboard-info">
	    	<div class="dashboard-info-header">
	    		Server Status
	    	</div>
	    	<div class="dashboard-info-detail">
	    		<ul>
	    			<li><strong>Build Number:</strong> <%= adminService.getBuildNumber() %></li>
	    			<li><strong>Uptime:</strong> <span id="uptime"><%= info.getUptime() %></span></li>
	    			<li><strong>Rayo nodes:</strong> <span id="nodes"><%= gatewayStatistics.getActiveRayoNodesCount() %></span></li>
	    			<li><strong>Total clients:</strong> <span id="clients"><%= gatewayStatistics.getTotalClientsCount() %></span></li>
	    			<li><strong>Total Calls:</strong> <span id="calls"><%= gatewayStatistics.getTotalCallsCount() %></span></li>
	    			<li><strong>Total Messages:</strong> <span id="messages"><%= gatewayStatistics.getMessagesCount() %></span></li>
	    			<li><strong>Total Errors:</strong> <span id="errors"><%= gatewayStatistics.getErrorsCount() %></span></li>
	    		</ul>
	    	</div>
		</div>
		<div class="dashboard-congrats">
	      <div class="heading">Congratulations!</div>
	      <div class="heading">Your Rayo Gateway is online.</div>
		  <div id="banner-icon"></div>
		</div>
	  </div>
	</div>
	<div class="portlet ui-widget-content ui-helper-clearfix ui-corner-all" id="portlet-template">
	  <div class="portlet-header ui-widget-header ui-corner-all"><span class='ui-icon ui-icon-minusthick'></span><span class="title">&nbsp;</span></div>
	  <div class="portlet-content"></div>
	</div>

	<div class="column"></div>
	
  </div>  

  
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.10/jquery-ui.js"></script>
  <script src="js/jolokia.js"></script>
  <script src="js/jolokia-simple.js"></script>
  <script src="js/highcharts.src.js"></script>
  <script src="js/dashboard.js"></script>
  <script src="js/periodic.js"></script>
  <script>
	  $(document).ready(function() {
		$.periodic({period: 1000}, function() {
			var j4p = new Jolokia("<%=request.getContextPath()%>/jmx");	
			var uptime = { type: "read", mbean: "com.rayo:Type=Info", attribute: "Uptime" };
			var calls = { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "TotalCallsCount" };
			var nodes = { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ActiveRayoNodesCount" };
			var errors = { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "ErrorsCount" };
			var messages = { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "MessagesCount" };
			var clients = { type: "read", mbean: "com.rayo.gateway:Type=GatewayStatistics", attribute: "TotalClientResourcesCount" };
			
			var responses = j4p.request([uptime,calls,liveCalls,nodes,errors,messages,clients]);
			if (responses[0] != null) {
		    	$("#uptime").text(responses[0].value);
			}
			if (responses[1] != null) {
		    	$("#calls").text(responses[1].value);
			}
			if (responses[2] != null) {
		    	$("#nodes").text(responses[2].value);
			}
			if (responses[3] != null) {
		    	$("#errors").text(responses[3].value);
			}
			if (responses[4] != null) {
		    	$("#messages").text(responses[4].value);
			}
			if (responses[5] != null) {
		    	$("#clients").text(responses[5].value);
			}																		
		});	
	  });  
  </script>
</body> 
</html>

 
 
 