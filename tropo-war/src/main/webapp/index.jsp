<%@ page import="com.tropo.web.*" %>
		
<%
	if (getServletConfig().getServletContext().getAttribute(ContextLoaderListener.TROPO_STATUS) == TropoStatus.FAILED) {
	    response.sendError(500);
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"> 
<html> 
<head> 
<title>Tropo - Cloud API for Voice, SMS, and Instant Messaging Services</title>  
</head> 
<body style="color:#777"> 
	<img src="images/tropo-logo.png" style="position:absolute;left:50%;top:40%;margin-left:-153px;margin-top:-25px;"/>
	<p style="position:absolute;left:47%;top:50%;margin-left:-153px;margin-top:-25px;">Congratulations. Your Tropo 2 installation is up and running.</p>
</body> 
</html> 

 
 
 