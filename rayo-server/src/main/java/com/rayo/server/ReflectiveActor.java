package com.rayo.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetlang.channels.Channel;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.Callback;
import org.jetlang.core.Disposable;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;

import com.voxeo.exceptions.NotFoundException;
import com.voxeo.logging.Loggerf;

public abstract class ReflectiveActor implements Actor, Callback<Object> {

    private static final Loggerf log = Loggerf.getLogger(ReflectiveActor.class);

    private Fiber fiber;
    private Channel<Object> channel;
    private boolean running = false;
    private Map<Class<?>, Method> targets = new HashMap<Class<?>, Method>();
    private static Map<String, Method> globalMethodsCache = new ConcurrentHashMap<String, Method>();

    private PoolFiberFactory fiberFactory;
    private Set<EventHandler> eventHandlers = new LinkedHashSet<EventHandler>();

    @Override
    public void onMessage(Object message) {

        if (!running) {
            if (message instanceof Request) {
                ((Request) message).reply(new NotFoundException());
            }
            log.info("Actor is disposed. Ignoring message. [%s]", message);
            return;
        }

        try {
            if (message instanceof EventHandler) {
                eventHandlers.add((EventHandler) message);
            }
            else if (message instanceof Request) {
                Request request = (Request) message;
                Object command = request.getCommand();

                if (log.isDebugEnabled()) {
                	log.debug("[%s] : Request [%s]", this, request);
                }

                try {

                    Method method = findMethod(command);

                    if (method == null) {
                        log.warn("Could not find command handler for message [%s]", message);
                        request.reply(new UnsupportedOperationException());
                        return;
                    }

                    Object result = method.invoke(this, command);
                    request.reply(result);
                } catch (InvocationTargetException e) {
                    Throwable targetException = e.getCause();
                    if (targetException != null) {
                        request.reply(targetException);
                        throw targetException;
                    } else {
                        request.reply(e);
                    }                    
                    throw e;
                } catch (Exception e) {
                    request.reply(e);
                    throw e;
                }
            } else {

                Method method = findMethod(message);

                if (method == null) {
                    log.warn("Could not find command handler for message [%s]", message);
                    return;
                }

                log.info("Message [%s]", message);
                method.invoke(this, message);
            }

        } catch (Throwable e) {
            log.error("Exception while processing command", e);
            if(!handleException(e)) {
                stop();
            }
        }
        finally {
            flushEvents();
        }
        
    }

    private Method findMethod(Object message) {
        
        Method method = globalMethodsCache.get(getMethodKey(message));
        if (method != null) return method;
        
        Queue<Class<?>> queue = new LinkedList<Class<?>>();
        queue.add(message.getClass());
        
        while(!queue.isEmpty()) {
            Class<?> clz = queue.poll();
            method = targets.get(clz);
            if (method != null) {
                break;
            }
            for (Class<?> iface : clz.getInterfaces()) {
                queue.add(iface);
            }
            Class<?> superclz = clz.getSuperclass();
            if(superclz != null && !superclz.equals(Object.class)) {
                queue.add(superclz);
            }
        }
        
        if (method != null) {
        	globalMethodsCache.put(getMethodKey(message), method);
        }
        
        return method;
    }

    private String getMethodKey(Object message) {

    	return this.getClass().toString() + message.getClass().toString();
	}

	/**
     * Continue after an exception by default
     * 
     * @param throwable
     * @return
     */
    protected boolean handleException(Throwable throwable) {
        return true;
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    @Override
    public synchronized void start() {

        this.fiber = fiberFactory.create();
        this.channel = new MemoryChannel<Object>();

        // Subscribe ourselves to receive events
        channel.subscribe(fiber, this);

        final Method[] methods = this.getClass().getMethods();
        for (final Method method : methods) {
            final Class<?>[] types = method.getParameterTypes();
            if (types.length != 1) {
                continue; // method must have one parameter 
            }
            if (method.getAnnotation(Message.class) == null) {
                continue; // method must have a Message annotation
            }
            final Class<?> commandClass = types[0];
            targets.put(commandClass, method);
        }

        fiber.start();
        running = true;
    }

    public void link(final ActorLink link) {
        fiber.add(new Disposable() {
            public void dispose() {
                link.postStop();
            }
        });
    }

    @Override
    public synchronized void stop() {
        if(running) {
            running = false;
            fiber.dispose();
        }
    }

    @Override
    public synchronized boolean publish(Object message) {

        if (running) {
            channel.publish(message);
        } else {
            log.info("Actor %s is disposed. Ignoring message. [%s]", this.getClass().getSimpleName(), message);
            if (message instanceof Request) {
                ((Request) message).reply(new NotFoundException());
            }
        }
        return running;
    }

    public PoolFiberFactory getFiberFactory() {
        return fiberFactory;
    }

    public void setFiberFactory(PoolFiberFactory fiberFactory) {
        this.fiberFactory = fiberFactory;
    }
    
    private Queue<Object> eventQueue = new LinkedList<Object>();

    private void flushEvents() {
        while(!eventQueue.isEmpty()) {
            Object message = eventQueue.poll();
            log.info("Event [%s]", message);
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(message);
                } catch (Exception e) {
                    log.error("Exception in event handler [event=%s]", message, e);
                }
            }
        }
    }
    
    public void flush() {
    	
    	flushEvents();
    }
    
    protected void fire(Object message) {
        if(running) {
            log.info("Queued Event [%s]", message);
            eventQueue.offer(message);
        }
        else {
            log.info("Actor %s is disposed. Ignoring event. [%s]", this.getClass().getSimpleName(), message);
        }
    }
    
    public void addEventHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }

    public void removeEventHandler(EventHandler handler) {
        eventHandlers.remove(handler);
    }

    public Collection<EventHandler> getEventHandlers() {
        return Collections.unmodifiableSet(eventHandlers);
    }

}
