package com.rayo.server;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.voxeo.logging.Loggerf;
import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.AcceptableEvent.Reason;

public class MohoDriver implements Application {

	private static final Loggerf log = Loggerf.getLogger(MohoDriver.class);

    private CallManager callManager;
    private AdminService adminService;
    private CallStatistics callStatistics;
    private CdrManager cdrManager;

    public void destroy() {}

    public void init(final ApplicationContext context) {
        
    	if (log.isDebugEnabled()) {
    		log.debug("Initializing Moho Driver");
    	}
        XmlWebApplicationContext wac = (XmlWebApplicationContext)WebApplicationContextUtils
            .getRequiredWebApplicationContext(context.getServletContext());
        
        callManager = wac.getBean(CallManager.class);
        callManager.setApplicationContext(context);
        callManager.start();
        
        adminService = wac.getBean(AdminService.class);
        callStatistics = wac.getBean(CallStatistics.class);
        cdrManager = wac.getBean(CdrManager.class);
    }

    @State
    public void onIncomingCall(final IncomingCall call) throws Exception {

    	if (log.isDebugEnabled()) {
    		log.debug("Received incoming call");
    	}
    	
    	cdrManager.create(call);
    	
    	if (adminService.isQuiesceMode()) {
            log.warn("Quiesce Mode ON. Dropping incoming call: %s", call.getId());
            callStatistics.callRejected();
            callStatistics.callBusy();
            call.reject(Reason.BUSY);
            cdrManager.end(call);
            cdrManager.store(call.getId());
            return;
    	}                    	

        callManager.publish(call);
    }
}