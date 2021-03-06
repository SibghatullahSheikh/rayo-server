package com.rayo.server.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import com.rayo.server.admin.AdminService;
import com.rayo.server.exception.ErrorMapping;
import com.rayo.server.exception.ExceptionMapper;
import com.rayo.server.listener.AdminListener;
import com.rayo.server.util.DomUtils;
import com.voxeo.logging.Loggerf;
import com.voxeo.servlet.xmpp.IQRequest;
import com.voxeo.servlet.xmpp.IQResponse;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.StanzaError;
import com.voxeo.servlet.xmpp.StanzaError.Condition;
import com.voxeo.servlet.xmpp.StanzaError.Type;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppSession;

@SuppressWarnings("serial")
public abstract class AbstractRayoServlet extends XmppServlet implements AdminListener {

	private static final Loggerf WIRE = Loggerf.getLogger("com.tropo.ozone.wire");
	
    private static final QName SESSION_QNAME = new QName("session", new Namespace("", "urn:ietf:params:xml:ns:xmpp-session"));
    private static final QName BIND_QNAME = new QName("bind", new Namespace("", "urn:ietf:params:xml:ns:xmpp-bind"));
    private static final QName PING_QNAME = new QName("ping", new Namespace("", "urn:xmpp:ping"));
    
    private static final String LOCAL_DOMAIN = "local-domain";

	private XmppFactory xmppFactory;
    private AdminService adminService;
    private ExceptionMapper exceptionMapper;
    
    private String localDomain;

	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		xmppFactory = (XmppFactory) config.getServletContext().getAttribute(XMPP_FACTORY);
		
		localDomain = config.getInitParameter(LOCAL_DOMAIN);
		if (localDomain == null) {
			try {
				localDomain = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				getLog().warn(e.getMessage());
				localDomain = "localhost";
			}
		}
		
        adminService.readConfigurationFromContext(getServletConfig());     
        adminService.addAdminListener(this);
	}
	
	@Override
	public void onPropertyChanged(String property, String newValue) {}
	
    /**
     * Called by Spring on component initialization
     * 
     * Cannot be called 'init' since it would override super.init() and ultimately be 
     * called twice: once by Spring and once by super.init(context)
     */
    public void start() {

    	getLog().info("Initializing %s. Build number: %s", adminService.getServerName(), adminService.getBuildNumber());
    }
	
    @Override
    protected void doIQRequest(IQRequest request) throws ServletException,IOException {

		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s", request,
					request.getSession().getId());
		}

		DOMElement requestElement = null;
		try {
			requestElement = toDOM(request.getElement());
		}
		catch (DocumentException ee) {
			throw new IOException("Could not parse XML content", ee);
		}

    	try {
			// Extract Request
        	DOMElement payload = (DOMElement) requestElement.elementIterator().next();
            QName qname = payload.getQName();
    		if (request.getSession().getType() == XmppSession.Type.INBOUNDCLIENT) {
                // Resource Binding
                if (qname.equals(BIND_QNAME)) {
                    String boundJid = request.getFrom().getNode() + "@" + request.getFrom().getDomain() + "/" + request.getFrom().getResource();
                    DOMElement bindElement = (DOMElement) DOMDocumentFactory.getInstance().createElement(BIND_QNAME);
                    bindElement.addElement("jid").setText(boundJid);
                    sendIqResult(request, bindElement);
                    getLog().info("Bound client resource [jid=%s]", boundJid); 
                    return;
                } 
                
                if (qname.equals(SESSION_QNAME)) {
                	sendIqResult(request);
                	return;
                }
    		}
    		
    		if (qname.equals(PING_QNAME) || qname.equals(SESSION_QNAME)) {
            	sendIqResult(request);
            	return;
            }
    		
    		if (DomUtils.isSupportedNamespace(payload)) {
    			// Validate jid
    			if (!validJid(request.getTo())) {
    				sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.JID_MALFORMED, String.format("Malformed JID", request.getTo()));
    			} else {
    				processIQRequest(request, payload);
    			}
    		} else {
           	 	// We don't handle this type of request...
    			sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.FEATURE_NOT_IMPLEMENTED, "Feature not supported");
    		}    		
    	} catch (Exception e) {
    		getLog().error(e.getMessage(),e);
            getLog().error("Exception processing IQ request", e);
            sendIqError(request, StanzaError.Type.CANCEL, StanzaError.Condition.INTERNAL_SERVER_ERROR, e.getMessage());
    	}
    }
        
	protected abstract void processIQRequest(IQRequest request, DOMElement payload);

	@Override
	public void onQuiesceModeEntered() {
	}
	
	@Override
	public void onQuiesceModeExited() {
	}
	
	@Override
	public void onShutdown() {
	}

    protected void sendIqError(IQRequest request, String type, String error, String text) throws IOException {
    	//TODO: Not needed once https://evolution.voxeo.com/ticket/1520421 is fixed
    	error = error.replaceAll("-", "_");
        sendIqError(request, request.createError(StanzaError.Type.valueOf(type.toUpperCase()), StanzaError.Condition.valueOf(error.toUpperCase()), text));
    }

    protected void sendIqError(IQRequest request, StanzaError.Type type, StanzaError.Condition error, String text) throws IOException {
         sendIqError(request, request.createError(type, error, text));
    }

    protected void sendIqError(IQRequest request, IQResponse response) throws IOException {
    	
        response.setFrom(request.getTo());
        response.send();
    }

    
    protected IQResponse sendIqResult(IQRequest request) throws IOException {
    	
    	return sendIqResult(request, null);
    }
    
    protected IQResponse sendIqResult(IQRequest request, org.w3c.dom.Element result) throws IOException {
    	
    	IQResponse response = null;
    	if (result != null) {
    		response = request.createResult(result);
    	} else {
    		response = request.createResult();
    	}
        response.setFrom(request.getTo());
        response.send();
        
        return response;
    }
    
    protected void sendPresenceError(JID fromJid, JID toJid) throws IOException, ServletException {

		sendPresenceError(fromJid, toJid, new Element[]{});
    }

    protected void sendPresenceError(JID fromJid, JID toJid, Condition condition) throws IOException, ServletException {
    
    	sendPresenceError(fromJid,  toJid, condition, Type.CANCEL);
    }

    protected void sendPresenceError(JID fromJid, JID toJid, Condition condition, Type type) throws IOException, ServletException {
        
    	sendPresenceError(fromJid,  toJid, condition, Type.CANCEL, null);
    }

    protected void sendPresenceError(JID fromJid, JID toJid, String condition, String type, String text) throws IOException, ServletException {
    	
    	Condition errorCondition = null;
    	Type errorType = null;
    	try {
	    	errorCondition = Condition.valueOf(condition);
	    	errorType = Type.valueOf(type);
    	} catch (Exception e) {
    		getLog().error("Cannot parser condition and type: [%s] :: [%s]. Ignoring it.", condition, type);
    		sendPresenceError(fromJid, toJid);
    		return;
    	}
    	sendPresenceError(fromJid, toJid, errorCondition, errorType, text);
    }
    
    protected void sendPresenceError(JID fromJid, JID toJid, Condition condition, Type type, String text) throws IOException, ServletException {
    	
		CoreDocumentImpl document = new CoreDocumentImpl(false);
		org.w3c.dom.Element errorElement = document.createElement("error");
		errorElement.setAttribute("type", type.toString());
		org.w3c.dom.Element conditionElement = document.createElement(condition.toString());
		errorElement.appendChild(conditionElement);
		if (text != null) {
			org.w3c.dom.Element textElement = document.createElement("text");
			textElement.setTextContent(text);
			errorElement.appendChild(textElement);
		}		
		sendPresenceError(fromJid, toJid, errorElement);
    }   
    
    protected void sendPresenceError(JID fromJid, JID toJid, Element... elements) throws IOException, ServletException {

    	PresenceMessage errorPresence;
    	if (elements == null || elements.length == 0) {
    		errorPresence = getXmppFactory()
				.createPresence(fromJid, toJid, "error");
    	} else {
    		errorPresence = getXmppFactory()
				.createPresence(fromJid, toJid, "error", elements);    		
    	}
		errorPresence.send();
		if (getWireLogger().isDebugEnabled()) {
			getWireLogger().debug("%s :: %s",
					errorPresence,
					errorPresence.getSession().getId());
		}
    }
	
	protected static String asXML (org.w3c.dom.Element element) {
		
		DOMImplementationLS impl = (DOMImplementationLS)element.getOwnerDocument().getImplementation();
		LSSerializer serializer = impl.createLSSerializer();
		serializer.getDomConfig().setParameter("xml-declaration", false);
		return serializer.writeToString(element);
	}
	
	public static DOMElement toDOM (org.dom4j.Element dom4jElement) throws DocumentException {
		
		DOMElement domElement = null;
		if (dom4jElement instanceof DOMElement) {
			domElement = (DOMElement) dom4jElement;
		} else {
			DOMDocument requestDocument = (DOMDocument)
				new DOMWriter().write(dom4jElement.getDocument());
			domElement = (DOMElement)requestDocument.getDocumentElement();
		}
		return domElement;
	}

	public static DOMElement toDOM (org.w3c.dom.Element w3cElement) throws DocumentException {
		
		DOMElement domElement = null;
		if (w3cElement instanceof DOMElement) {
			domElement = (DOMElement) w3cElement;
		} else {
			DOMDocument requestDocument = (DOMDocument)
				new DOMReader(DOMDocumentFactory.getInstance())
					.read(w3cElement.getOwnerDocument());
			domElement = (DOMElement)requestDocument.getDocumentElement();
		}
		return domElement;
	}
	
	private boolean validJid(JID jid) {
		
		if (jid.getDomain() == null || jid.getDomain().isEmpty()) {
			return false;
		}
		return true;
	}
	
    protected String getBareJID(String address) {

    	address = address.replaceAll("sip:", "");
    	int colon = address.indexOf(":"); 
    	if (colon != -1) {
    		address = address.substring(0, colon);
    	}
    	return address;
	}
	
    protected void sendIqError(IQRequest request, Exception e) {
        try {
            ErrorMapping error = exceptionMapper.toXmppError(e);
            sendIqError(request, error.getType(), error.getCondition(), error.getText());
        }
        catch (Exception e1) {
            throw new IllegalStateException("Cannot dispatch result", e);
        }
    }
    
	protected static Loggerf getWireLogger() {
		return WIRE;
	}
	
	protected XmppFactory getXmppFactory() {
		return xmppFactory;
	}
	
	protected abstract Loggerf getLog();

	public AdminService getAdminService() {
		return adminService;
	}

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}	
	
	public String getLocalDomain() {
		
		return localDomain;
	}
	
	public void setExceptionMapper(ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }
	
	public ExceptionMapper getExceptionMapper() {
		
		return exceptionMapper;
	}
}
