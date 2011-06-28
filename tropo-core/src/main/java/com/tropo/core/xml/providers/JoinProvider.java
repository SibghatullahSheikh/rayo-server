package com.tropo.core.xml.providers;

import java.net.URISyntaxException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.tropo.core.verb.Join;
import com.tropo.core.verb.JoinCompleteEvent;

public class JoinProvider extends BaseProvider {

    // XML -> Object
    // ================================================================================

    private static final Namespace NAMESPACE = new Namespace("", "urn:xmpp:ozone:join:1");
    private static final Namespace COMPLETE_NAMESPACE = new Namespace("", "urn:xmpp:ozone:join:complete:1");
    
    @Override
    protected Object processElement(Element element) throws Exception {
        if (element.getName().equals("join")) {
            return buildJoin(element);
        }
        return null;
    }

    private Object buildJoin(Element element) throws URISyntaxException {
        
    	Join join = new Join();
    	if (element.attribute("type") != null) {
    		join.setType(element.attributeValue("type"));
    	}
    	
    	if (element.attribute("direction") != null) {
    		join.setDirection(element.attributeValue("direction"));
    	}
    	
    	if (element.attribute("to") != null) {
    		join.setTo(element.attributeValue("to"));
    	}
    	join.setHeaders(grabHeaders(element));
        return join;
    }
    
    
    // Object -> XML
    // ================================================================================

    @Override
    protected void generateDocument(Object object, Document document) throws Exception {

        if (object instanceof Join) {
            createJoin((Join) object, document);
        } else if (object instanceof JoinCompleteEvent) {
        	createJoinCompleteEvent((JoinCompleteEvent) object, document);
        }
    }
    
	private void createJoinCompleteEvent(JoinCompleteEvent event, Document document) throws Exception {
	    
		addCompleteElement(document, event, COMPLETE_NAMESPACE);
	}
    
    private void createJoin(Join join, Document document) throws Exception {
    	
        Element root = document.addElement(new QName("join", NAMESPACE));
        
        if (join.getDirection() != null) {
        	root.addAttribute("direction", join.getDirection());        	
        }
        if (join.getType() != null) {
        	root.addAttribute("type", join.getType());
        }
        if (join.getTo() != null) {
        	root.addAttribute("to", join.getTo());
        }
        addHeaders(join.getHeaders(), root);
    }

    @Override
    public boolean handles(Class<?> clazz) {

        //TODO: Refactor out to spring configuration and put everything in the base provider class
        return clazz == Join.class ||
        	   clazz == JoinCompleteEvent.class;
    }
}
