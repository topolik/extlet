/**
 * Copyright (c) 2009-2010 IBA CZ, s. r. o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.ibacz.extlet.restart;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ServerDetector;
import com.liferay.portal.util.PropsUtil;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * Implementation of the {@link Runnable} interface for restarting current instance of tomcat Host object.<br />
 * <br />
 * <strong>Important</strong>: Restart is disabled by default and the current implementation use Tomcat's JMX for restarting.
 * In order to use this functionality:<ul>
 * <li>the local MBean server must be started. See <a href="http://tomcat.apache.org/tomcat-5.5-doc/monitoring.html">Tomcat JMX</a> for further info.</li>
 * <li>enable restart using <code>eu.ibacz.extlet.restart.enabled=true</code> in the portal properties.</li>
 * </ul>
 *
 * <br />
 * <strong>Also important</strong>: Please use extlet-tomcat-reload component, to fix bugs in the Liferay.
 * These bugs prevent the Liferay from the correct restart.<br />
 * <br />
 * Note: Most of the calls are invoked using reflection because the old stopping portal webapp has different classloader
 * than the new - starting one - portal webapp. Both have their own defitions of this class loaded in the memory.
 * And because TomcatHostRestart class is communication with itself, it must use the reflection for calling the other instance.
 * 
 * @author Tomáš Polešovský
 */
public class TomcatRestarter implements Runnable {

    /**
     * Tries to restart the tomcat only if the tomcat is not currenty restarting.
     * <ol><li>If the process has been started,</li><ol><li>just leave a message that another extlet wants restart - using the {@link TomcatHostRestart#incrementCaller()} method</li></ol>
     * <li>If the process has not been started,</li> <ol><li>register this instance into the JNDI and</li><li>run the restart thread</li></ol>
     */
    public static synchronized void startThread() {

        if (!ENABLED) {
            if (_log.isWarnEnabled()) {
                _log.warn("TomcatHostRestart is disabled. Please restart the Tomcat manually!");
            }
            return;
        }

        if(!ServerDetector.isTomcat()){
            if (_log.isWarnEnabled()) {
                _log.warn("Liferay is not running on the Tomcat! Please reload application manually!");
            }
            return;
        }

        /*
         * Keep only one instance of the class until restart finishes.
         */
        if (getRestartPhase() == PHASE_WAITING_FOR_REQUESTS || getRestartPhase() == PHASE_RESTARTING) {
            try {
                if (_log.isDebugEnabled()) {
                    _log.debug("Tomcat restart is running, just increment the caller counter");
                }
                // increment caller using reflection (because of class reloading)
                getInstance().getClass().getDeclaredMethod("incrementCaller", (Class[]) null).invoke(getInstance(), (Object[]) null);
            } catch (Exception ex) {
                _log.error("Problem while incrementing caller", ex);
            }
            return;
        }

        TomcatRestarter process = new TomcatRestarter();
        // save instance into the JNDI
        setInstance(process);
        
        Thread restartProcess = new Thread(process);
        restartProcess.setDaemon(true);
        restartProcess.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        restartProcess.start();
    }


    /**
     * The thread implementation. Waits before restart and then restart.<br />
     * <br />
     * <ol><li>set phase = PHASE_WAITING_FOR_REQUESTS</li>
     * <li>Waits x miliseconds (specified using {@link TomcatHostRestart#SLEEP_TIMEOUT_KEY}) 
     * until there is no other request for restart, then </li>
     * <li>set phase = PHASE_RESTARTING and</li>
     * <li>calls {@link TomcatHostRestart#restartHost()} or {@link TomcatHostRestart#reloadRootWebApp()} depending on the {@link #FULL_RESTART}</li>
     * </ol>
     */
    public void run() {
        /*
         * Start the process
         */
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Setting phase PHASE_WAITING_FOR_REQUESTS");
            }
            phase.set(PHASE_WAITING_FOR_REQUESTS);


            /*
             * Try to sleep and wait until there is no other request for restart
             */
            // wait at least once
            while (-1 != callCount.getAndDecrement()) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Waiting for others before restart ... " + (callCount.get() + 1));
                }
                try {
                    Thread.sleep(SLEEP_TIMEOUT);
                } catch (InterruptedException ex) {
                    _log.error("Thread interrupted while waiting for other restart requests", ex);
                }
            }

            /*
             * Restart instance
             */
            if (_log.isDebugEnabled()) {
                _log.debug("Setting phase PHASE_RESTARTING");
            }
            phase.set(PHASE_RESTARTING);

            long timer = System.currentTimeMillis();

            if(FULL_RESTART){
                // restart all modules in the tomcat
                restartHost();
            } else {
                // just reload the portal web app
                reloadRootWebApp();
            }

            if(_log.isInfoEnabled()){
                _log.info("Restart took " + (System.currentTimeMillis() - timer) + " ms");
            }
            
        } finally {

            phase.set(PHASE_RESTARTED);
            
            /*
             * Reset instance to null for another run
             */
            setInstance(null);
        }
    }

    /**
     * Counter of how many calls has been made for restart.
     */
    public void incrementCaller(){
        callCount.incrementAndGet();
    }

    /**
     * Returns on of the phases PHASE_STARTING, PHASE_WAITING_FOR_REQUESTS or PHASE_RESTARTING depending on the current state.<br />
     * <br />
     * Retrieves the instance from the JNDI and using reflection get current phase ID.
     *
     * @return PHASE_STARTING, PHASE_WAITING_FOR_REQUESTS or PHASE_RESTARTING. In case of error returns -1;
     */
    public static int getRestartPhase() {
        try {
            // if there is no instance set, we are in the starting phase
            if(getInstance() == null){
                return PHASE_STARTING;
            }
            // get current phase using reflection (because of class reloading)
            return (Integer) getInstance().getClass().getDeclaredMethod("getPhase", (Class[]) null).invoke(getInstance(), (Object[]) null);
        } catch (Exception ex) {
            _log.error("Problem while accessing getPhase method", ex);
        }
        return -1;
    }

    /**
     * Current phase ID
     * @return
     */
    public int getPhase(){
        return phase.get();
    }

    /**
     * Saves current thread instance into the JNDI.
     * @param instance - Instance to be saved
     */
    protected static void setInstance(TomcatRestarter instance) {
        if(_log.isDebugEnabled()){
            _log.debug("Registering instance of TomcatHostRestart in the JNDI: " + instance);
        }
        try {
            Context ctx = new InitialContext();
            if (instance != null) {
                ctx.bind(TomcatRestarter.class.getName(), instance);
            } else {
                ctx.unbind(TomcatRestarter.class.getName());
            }
        } catch(NameNotFoundException ex){
            if(_log.isDebugEnabled()){
                _log.debug("Context not bound in the JNDI", ex);
            }
        } catch (NamingException ex) {
            _log.error("Problem while binding the instance to the JNDI environment", ex);
        }
    }

    /**
     * Loads current instance from the JDNI.
     * @return The only one instance for in the jvm.
     */
    public static Object getInstance() {
        if(_log.isDebugEnabled()){
            _log.debug("Looking up the instance of TomcatHostRestart in the JNDI");
        }
        try {
            Context ctx = new InitialContext();
            Object result = ctx.lookup(TomcatRestarter.class.getName());
            if(_log.isDebugEnabled()){
                _log.debug("Lookup instance: " + result);
            }
            return result;
        } catch(NameNotFoundException ex){
            if(_log.isDebugEnabled()){
                _log.debug("Context not bound in the JNDI", ex);
            }
        } catch (NamingException ex) {
            _log.error("Problem while looking up the JDNI environment ", ex);
        }
        return null;
    }


    /**
     * Main restart logic. <br />
     * <br />
     * Load the Catalina <code>Host</code> MBean from the MBean server and invokes:<ul>
     * <li>stop - stops all applications in the current server</li>
     * <li>start - starts all application in the current server</li>
     * </ul>
     *
     */
    protected void restartHost() {
        if (_log.isWarnEnabled()) {
            _log.warn("Restarting tomcat ...");
        }
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName(OBJECT_NAME_HOST);

            if (_log.isInfoEnabled()) {
                _log.info("Stopping tomcat host " + OBJECT_NAME_HOST);
            }
            mbs.invoke(objectName, "stop", (Object[]) null, (String[]) null);

            if (_log.isDebugEnabled()){
                _log.debug("Waiting for Tomcat threads to finish [" + SLEEP_TIMEOUT + "] ms");
            }
            Thread.sleep(SLEEP_TIMEOUT);

            if (_log.isInfoEnabled()) {
                _log.info("Tomcat host stopped successfully. Starting tomcat host " + OBJECT_NAME_HOST);
            }

            mbs.invoke(objectName, "start", (Object[]) null, (String[]) null);

            if (_log.isInfoEnabled()) {
                _log.info("Tomcat host " + OBJECT_NAME_HOST + " started successfully.");
            }
        } catch (MalformedObjectNameException ex1) {
            _log.error("Problem while creating ObjectName [" + OBJECT_NAME_HOST + "]", ex1);
        } catch (Exception ex) {
            _log.error("Problem while restarting Tomcat's Host instance [" + OBJECT_NAME_HOST + "]", ex);
        } finally {
            if (_log.isWarnEnabled()) {
                _log.warn("Restarting tomcat finished");
            }
        }

    }


    /**
     * Main liferay webapp reload logic. <br />
     * <br />
     * Load the Catalina <code>WebApp</code> MBean from the <code>MBeanServer</code> and invokes:<ul>
     * <li>reload() - which stops and starts the root application - portal</li>
     * </ul>
     *
     */
    protected void reloadRootWebApp() {
        if (_log.isWarnEnabled()) {
            _log.warn("Reloading Root WebModule ...");
        }
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName contextObjectName = new ObjectName(OBJECT_NAME_PORTALWEBAPP);

            if (_log.isDebugEnabled()) {
                _log.debug("Reloading Root WebModule " + OBJECT_NAME_PORTALWEBAPP);
            }

            mbs.invoke(contextObjectName, "reload", (Object[]) null, (String[]) null);

            if (_log.isDebugEnabled()) {
                _log.debug("Tomcat WebModule " + OBJECT_NAME_PORTALWEBAPP + " reloaded successfully.");
            }
        } catch (MalformedObjectNameException ex1) {
            _log.error("Problem while creating ObjectName [" + OBJECT_NAME_PORTALWEBAPP + "]", ex1);
        } catch (Exception ex) {
            _log.error("Problem while restarting Tomcat's WebApp instance [" + OBJECT_NAME_PORTALWEBAPP + "]", ex);
        } finally {

            if (_log.isWarnEnabled()) {
                _log.warn("Reloading Root WebModule finished");
            }
        }

    }



    /**
     * Restart process has not been launched and is just about to start (see {@link TomcatHostRestart#run()}.
     */
    public static final int PHASE_STARTING = 0;
    /**
     * Restart process has been launched, the thread is waiting to initiate the restart (see {@link TomcatHostRestart#SLEEP_TIMEOUT_KEY}).
     */
    public static final int PHASE_WAITING_FOR_REQUESTS = 1;
    /**
     * Restart process has been launched, Tomcat is actally restarting.
     */
    public static final int PHASE_RESTARTING = 2;
    /**
     * Restart process has been finished
     */
    public static final int PHASE_RESTARTED = 3;

    /**
     * Key for enabling/disabling the restart on extlet (re)deploy.<br />
     * Default value: <code>eu.ibacz.extlet.restart.enabled=false</code>
     */
    public static final String ENABLED_KEY = "eu.ibacz.extlet.restart.enabled";
    /**
     * Number of miliseconds to wait before initiate restart. Useful when deploying many extlets at once and the deploy process takes longer time to finish.<br />
     * Default value: <code>eu.ibacz.extlet.restart.timeout=5000</code>
     */
    public static final String SLEEP_TIMEOUT_KEY = "eu.ibacz.extlet.restart.timeout";
    /**
     * The name of the Host object inside the JMX.<br />
     * Default value: <code>eu.ibacz.extlet.restart.objectName=Catalina:type=Host,host=localhost</code>
     */
    public static final String OBJECT_NAME_HOST_KEY = "eu.ibacz.extlet.restart.objectName";
    /**
     * The name of the WebApp object representing root web app inside the JMX.<br />
     * Default value: <code>Catalina:j2eeType=WebModule,name=//localhost/,J2EEApplication=none,J2EEServer=none</code>
     */
    public static final String OBJECT_NAME_PORTALWEBAPP_KEY = "eu.ibacz.extlet.restart.objectNameRootWebApp";
    /**
     * If enabled, all web applications are reloaded, only portal otherwise.<br />
     * Default value: <code>eu.ibacz.extlet.restart.full=false</code>
     */
    public static final String FULL_RESTART_KEY = "eu.ibacz.extlet.restart.full";

    protected static final boolean ENABLED = GetterUtil.getBoolean(PropsUtil.get(ENABLED_KEY), false);
    protected static final int SLEEP_TIMEOUT = GetterUtil.getInteger(PropsUtil.get(SLEEP_TIMEOUT_KEY), 5000);
    protected static final String OBJECT_NAME_HOST = GetterUtil.getString(PropsUtil.get(OBJECT_NAME_HOST_KEY), "Catalina:type=Host,host=localhost");
    protected static final String OBJECT_NAME_PORTALWEBAPP = GetterUtil.getString(PropsUtil.get(OBJECT_NAME_PORTALWEBAPP_KEY), "Catalina:j2eeType=WebModule,name=//localhost/,J2EEApplication=none,J2EEServer=none");
    protected static final boolean FULL_RESTART = GetterUtil.getBoolean(PropsUtil.get(FULL_RESTART_KEY), false);

    protected AtomicInteger callCount = new AtomicInteger(0);
    protected AtomicInteger phase = new AtomicInteger(PHASE_STARTING);

    private static Log _log = LogFactoryUtil.getLog(TomcatRestarter.class);
}
