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
package eu.ibacz.extlet.tomcatreload;

import com.liferay.portal.kernel.deploy.auto.AutoDeployUtil;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.job.JobSchedulerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

/**
 * Free and reinitialize resources in the global ClassLoaders (JARS that are loaded by the Tomcat or JVM).<br />
 * <br />
 * You can notice that the invalidation must be done using the Java Reflection API,
 * because portal is not prepared for such operations.
 *
 * @see LiferayKernelReflectionUtil
 * @author Tomáš Polešovský
 */
public class GlobalShutdownAction extends SimpleAction {

    @Override
    public void run(String[] arg0) throws ActionException {

        // Invalidate & clear the Cache registry

        try {
            new LiferayKernelReflectionUtil().invalidateCacheRegistry();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // Invalidate InstancePool

        try {
            new LiferayKernelReflectionUtil().invalidateInstancePool();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // Remove all jobs from Quartz scheduler
        try {
            ((ExtletJobSchedulerImpl) JobSchedulerUtil.getJobScheduler()).deleteAllJobs();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // Create new singleton instance of HotDeployUtil
        try {
            new LiferayKernelReflectionUtil().reinitializeHotDeployUtil();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // Invalidate PortletBagPool's map

        try {
            new LiferayKernelReflectionUtil().invalidatePortletBagPool();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }


        // Reinitialize MethodCache with new instance

        try {
            new LiferayKernelReflectionUtil().reinitializeMethodCache();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // Invalidate JNDIUtils's static cache

        try {
            new LiferayKernelReflectionUtil().invalidateJNDIUtil();
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

        // stop auto deploy scanner
        try {
            AutoDeployUtil.unregisterDir("defaultAutoDeployDir");
        } catch (Exception e) {
            if (_log.isWarnEnabled()) {
                _log.warn(e, e);
            }
        }

    }
    private static Log _log = LogFactoryUtil.getLog(GlobalShutdownAction.class);
}
