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
package eu.ibacz.extlet.deploy.hot;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Properties;

import com.liferay.portal.kernel.deploy.hot.HotDeployEvent;
import com.liferay.portal.kernel.deploy.hot.HotDeployException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.util.PortalUtil;

import eu.ibacz.extlet.restart.TomcatRestarter;

/**
 * Deployer that copies (removes) extlet jars throughout the portal.<br />
 * <br />
 * The jars to be copied are defined in the <code>liferay-extlet.properties</code> file
 * with the keys: {@link ExtletHotDeployer#EXTLET_IMPL_JAR_NAME_KEY} and {@link ExtletHotDeployer#EXTLET_SERVICE_JAR_NAME_KEY}.
 * @author Tomáš Polešovský
 * @author Jaromír Hamala
 */
public class ExtletHotDeployer {

    /**
     * @deprecated Use {@link ExtletHotDeployer#EXTLET_IMPL_JAR_NAME_KEY}
     *
     * Key of the liferay-extlet.properties file for defining the extlet jar name.
     */
    public static final String EXTLET_JAR_NAME_KEY = "extlet.jar.name";
    /**
     * Key for defining all jars (separated using comma), 
     * that should be copied into the portal's classloader =&gt;
     * into the Liferay's WEB-INF/lib directory.
     */
    public static final String EXTLET_IMPL_JAR_NAME_KEY = "extlet.impl.jar.name";
    /**
     * Key for defining all jars (separated using comma), 
     * that should be copied into the global's classloader =&gt;
     * the directory where portal-kernel.jar resides.
     */
    public static final String EXTLET_SERVICE_JAR_NAME_KEY = "extlet.service.jar.name";
    private HotDeployEvent event;
    private Properties extletProperties;

    public ExtletHotDeployer(HotDeployEvent event, Properties properties) {
        this.event = event;
        this.extletProperties = properties;
    }

    /**
     * This method is called by the the ExtletHotDeployListener when: <ul>
     * <li>Extlet webapp is deploying</li>
     * <li>Tomcat starts or restart</li></ul>
     * <br />
     * If the extlet is really deploying, this method copies all jars
     * into their locations in the portal and tries to restart tomcat.
     */
    protected void deployPortalExtlet() throws HotDeployException {
        if (extletProperties == null) {
            if (_log.isDebugEnabled()) {
                _log.debug("Application " + event.getServletContextName() + " is not an extlet.");
            }
            return;
        }

        if (!isDeployTime()) {
            return;
        }

        boolean isRestartNeeded = false;
        if (hasExtletServiceJar()) {
            copyExtletServiceJars();
            isRestartNeeded = true;
        }
        if (hasExtletImplJar()) {
            copyExtletImplJar();
            isRestartNeeded = true;
        }

        if (_log.isInfoEnabled()) {
            _log.info("Extlet " + event.getServletContextName() + " has been succesfully deployed");
        }
        if (isRestartNeeded) {
            if (_log.isInfoEnabled()) {
                _log.info("Restart is needed. Restart should start in a few seconds ... ");
            }
            TomcatRestarter.startThread();
        }
    }

    /**
     * This method is called by the ExtletHotDeployListener when webapp is unloaded from the Tomcat, e.g. when:<ul>
     * <li>Extlet webapp is deployed</li>
     * <li>Tomcat stops or restarts<li>
     * <br />
     * If the extlet is really undeployed (not only tomcat is restarting), 
     * then method remove all jars from their locations in the portal and tries to restart tomcat.
     */
    protected void unDeployPortalExtlet() throws HotDeployException {
        if (!isUndeployTime()) {
            return;
        }

        boolean isRestartNeeded = false;
        if (hasExtletImplJar()) {
            removeExtletImplJar();
            isRestartNeeded = true;
        }
        if (hasExtletServiceJar()) {
            removeExtletServiceJar();
            isRestartNeeded = true;
        }
        if (_log.isInfoEnabled()) {
            _log.info("Extlet " + event.getServletContextName() + " has been succesfully undeployed");
        }
        if (isRestartNeeded) {
            if (_log.isInfoEnabled()) {
                _log.info("Restart is needed. Restart should start in a few seconds ... ");
            }
            TomcatRestarter.startThread();
        }
    }

    /**
     * Removes all extlet's jars specified by {@link ExtletHotDeployer#EXTLET_SERVICE_JAR_NAME_KEY} from the directory where the portal-kernel.jar resides.
     */
    private void removeExtletServiceJar() throws HotDeployException {
        if (_log.isDebugEnabled()) {
            _log.debug("About to remove exlet service.");
        }

        for (File extletServiceJar : getExtletServiceJars()) {
            File portalServiceJar = getPortalServiceJar(extletServiceJar);
            if (_log.isDebugEnabled()) {
                _log.info("Removing extlet service " + portalServiceJar.getName() + ".");
            }

            FileUtil.delete(portalServiceJar);

            if (_log.isDebugEnabled()) {
                _log.info("Extlet service " + portalServiceJar.getName() + " undeployed successfully.");
            }
        }
    }

    /**
     * Removes all extlet's jars specified by {@link ExtletHotDeployer#EXTLET_IMPL_JAR_NAME_KEY} from the portal's WEB-INF/lib directory.
     */
    protected void removeExtletImplJar() throws HotDeployException {
        if (_log.isDebugEnabled()) {
            _log.debug("About to remove extlet impl.");
        }

        for (File getExtletImplJar : getExtletImplJars()) {
            File portalImplJar = getPortalImplJar(getExtletImplJar);
            if (_log.isDebugEnabled()) {
                _log.info("Removing extlet impl [" + portalImplJar.getAbsolutePath() + "]");
            }

            FileUtil.delete(portalImplJar);

            if (_log.isDebugEnabled()) {
                _log.info("Extlet impl jar " + portalImplJar.getName() + " undeployed successfully.");
            }
        }
    }

    /**
     * Copies all extlet's jars specified by {@link ExtletHotDeployer#EXTLET_IMPL_JAR_NAME_KEY} into the portal's WEB-INF/lib directory.
     */
    private void copyExtletImplJar() throws HotDeployException {
        if (_log.isDebugEnabled()) {
            _log.debug("About to deploying the extlet impl.");
        }
        File[] extletImplJars = getExtletImplJars();

        for (File extletImplJar : extletImplJars) {
            File portalImplJar = getPortalImplJar(extletImplJar);
            if (_log.isDebugEnabled()) {
                _log.info("Deploying extlet impl " + extletImplJar.getName() + " from " + event.getServletContextName());
            }
            FileUtil.copyFile(extletImplJar, portalImplJar);
            if (_log.isDebugEnabled()) {
                _log.info("Extlet Impl Jar " + extletImplJar.getName() + " deployed successfully.");
            }
        }
    }

    /**
     * Copies all extlet's jars specified by {@link ExtletHotDeployer#EXTLET_SERVICE_JAR_NAME_KEY} into the directory with portal-kernel.jar.
     */
    private void copyExtletServiceJars() throws HotDeployException {
        if (_log.isDebugEnabled()) {
            _log.debug("About to start to deploy extlet services.");
        }
        File[] extletServiceJars = getExtletServiceJars();
        for (File extletServiceJar : extletServiceJars) {
            File portalServiceJar = getPortalServiceJar(extletServiceJar);
            if (_log.isDebugEnabled()) {
                _log.info("Deploying extlet service " + extletServiceJar.getAbsolutePath() + " to " + portalServiceJar.getAbsolutePath());
            }
            FileUtil.copyFile(extletServiceJar, portalServiceJar);
            if (_log.isDebugEnabled()) {
                _log.info("Extlet Service Jar " + extletServiceJar.getName() + " deployed successfully.");
            }
        }
    }

    /**
     * Return <code>true</code> if Extlet contains JAR to be copied
     * into the Portal Classloader (with the portal-impl.jar)
     */
    private boolean hasExtletImplJar() {
        return (getExtletImplJarNames() != null);
    }

    /**
     * Return <code>true</code> if Extlet contains JAR to be copied
     * into the Global Classloader (with the portal-kernel.jar)
     */
    private boolean hasExtletServiceJar() {
        return (getExtletServiceJarNames() != null);
    }

    /**
     * Returns files from webapp that should be copied into the portal WEB-INF/lib location
     */
    public File[] getExtletImplJars() throws HotDeployException {
        String[] extletImplJarNames = getExtletImplJarNames();
        File[] extletImplJars = new File[extletImplJarNames.length];
        for (int i = 0; i < extletImplJarNames.length; i++) {
            File extletImplJar = new File(event.getServletContext().getRealPath(extletImplJarNames[i]));
            extletImplJars[i] = extletImplJar;
        }
        return extletImplJars;
    }

    /**
     * Return file that points into the portal's WEB-INF/lib directory
     */
    public File getPortalImplJar(File extletImplJar) throws HotDeployException {
        File portalImplJar = new File(getPortalImplJarName(extletImplJar.getName()));
        return portalImplJar;
    }

    /**
     * Returns files from webapp that should be copied into the portal service location
     */
    public File[] getExtletServiceJars() {
        String[] extletServiceJarNames = getExtletServiceJarNames();

        File[] extletServiceJars = new File[extletServiceJarNames.length];
        for (int i = 0; i < extletServiceJarNames.length; i++) {
            extletServiceJars[i] = new File(event.getServletContext().getRealPath(extletServiceJarNames[i]));
        }
        return extletServiceJars;
    }

    /**
     * Returns file that points into the directory, where portal-kernel.jar resides
     */
    public File getPortalServiceJar(File extletServiceJar) throws HotDeployException {
        File portalServiceJarParent = getPortalServiceJarParent();

        File portalServiceJar = new File(portalServiceJarParent, extletServiceJar.getName());

        return portalServiceJar;
    }

    /**
     * Creates path for the portal WEB-INF/lib/extlet-....jar file
     */
    private String getPortalImplJarName(String extletImplJarName) {
        return PortalUtil.getPortalLibDir() + new File(extletImplJarName).getName();
    }

    /**
     * Load names of the extlet jars that override portal-impl classes and configuration.
     * @return
     */
    private String[] getExtletImplJarNames() {
        String extletImplJarName = extletProperties.getProperty(EXTLET_IMPL_JAR_NAME_KEY) != null ? extletProperties.getProperty(EXTLET_IMPL_JAR_NAME_KEY) : extletProperties.getProperty(EXTLET_JAR_NAME_KEY); //fallback to old key
        if (extletImplJarName == null) {
            return null;
        }

        return extletImplJarName.split(",");
    }

    /**
     * Tries to locate path for the classloader where the services are saved.<br />
     *
     * Use the ReleaseInfo class from portal-kernel.jar for locating the directory.
     * @return directory where portal-kernel.jar is saved
     * @throws com.liferay.portal.kernel.deploy.hot.HotDeployException
     */
    private File getPortalServiceJarParent() throws HotDeployException {
        try {
            File file = new File(com.liferay.portal.kernel.util.ReleaseInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            if (_log.isDebugEnabled()) {
                _log.info("Path to global classloader: " + file.getParent());
            }
            return file.getParentFile();
        } catch (URISyntaxException e) {
            throw new HotDeployException("Cannot localize directory for global classloader!", e);
        }
    }

    /**
     * Load names of the extlet jars that override portal-service classes and configuration.
     * @return String[] names of the jars in the extlet webbapp.
     */
    private String[] getExtletServiceJarNames() {
        String extletServiceJarNames = extletProperties.getProperty(EXTLET_SERVICE_JAR_NAME_KEY);
        if (extletServiceJarNames == null) {
            return null;
        }
        return extletServiceJarNames.split(",");
    }

    /**
     * Returns <code>TRUE</code> if and only if extlet is undeploying.
     */
    private boolean isUndeployTime() {
        // the tomcat is shutting down, we don't need to remove extlet jar
        if (isTomcatShuttingDown()) {
            if (_log.isDebugEnabled()) {
                _log.debug("Liferay is shutting down, stopping undeploying extlet jar");
            }
            return false;
        }
        return true;
    }

    /**
     * Return <b>TRUE</b> if and only if the tomcat is deploying and we can start deploying the JARs
     */
    private boolean isDeployTime() {
        // the tomcat is starting, we don't need to copy extlet jar
        if (isTomcatStarting()) {
            if (_log.isDebugEnabled()) {
                _log.debug("Liferay is starting, stopping deploying extlet jars");
            }
            return false;
        }
        // tomcat is restarting using our Restarter => existing web app reloading
        if (TomcatRestarter.getRestartPhase() == TomcatRestarter.PHASE_RESTARTING) {
            if (_log.isDebugEnabled()) {
                _log.debug("Liferay is restarting, stopping deploying extlet jars");
            }
            return false;
        }

        return true;
    }

    /**
     * Return true if the tomcat server is in the starting phase, false otherwise
     */
    protected boolean isTomcatStarting() {
        // I know this is a raw superhack, but this is the only one I was able to invent so as it would be simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        return sw.toString().contains("org.apache.catalina.startup.Catalina.start(");
    }

    /**
     * Return true if the tomcat server is in the shutting down phase, false otherwise
     */
    protected boolean isTomcatShuttingDown() {
        // Another one raw superhack, again, this is the only one I was able to invent so as it would be still simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        return sw.toString().contains("org.apache.catalina.startup.Catalina.stop(");
    }
    private static Log _log =
            LogFactoryUtil.getLog(ExtletHotDeployer.class);
}
