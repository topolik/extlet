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
import com.liferay.portal.kernel.util.ServerDetector;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple detector of current start/stop state for indicating deploy/undeploy
 *
 * @author Tomáš Polešovský
 */
public class ServletContainerUtil {
    public static final Map SUPPORTED_CONTROLLERS;
    static {
        HashMap controllers = new HashMap(2);
        {
            ServletContainerController ctrl = new TomcatServletContainerController();
            controllers.put(ctrl.getServerDetectorContainerId(), ctrl);
        }
        {
            ServletContainerController ctrl = new WebLogicServletContainerController();
            controllers.put(ctrl.getServerDetectorContainerId(), ctrl);
        }
        SUPPORTED_CONTROLLERS = Collections.unmodifiableMap(controllers);
    };


    /**
     * Returns true only if current servlet container is supported by this implementation
     */
    public static boolean isContainerSupported(){
        return SUPPORTED_CONTROLLERS.containsKey(ServerDetector.getServerId());
    }

    /**
     * Tries to restart current container
     */
    public void restartContainer() {
        getController().restartPortal();
    }

    /**
     * Returns <code>TRUE</code> if and only if extlet is undeploying.
     */
    public boolean isUndeployTime() {
        return !(getController().isShuttingDown() || getController().isTriggeredRestart());
    }

    /**
     * Return <b>TRUE</b> if and only if the servlet container is deploying and we can start deploying the JARs
     */
    public boolean isDeployTime() {
        return ! (getController().isStarting() || getController().isTriggeredRestart());
    }

    /**
     * Returns current ServletContainerController implementation
     * @throws RuntimeException When there is not support for this servlet container
     */
    public static ServletContainerController getController() throws RuntimeException{
        if(!ServletContainerUtil.isContainerSupported()){
            throw new RuntimeException("This container is unsupported by Extlet!");
        }

        return (ServletContainerController) SUPPORTED_CONTROLLERS.get(ServerDetector.getServerId());
    }

    private static Log _log =
            LogFactoryUtil.getLog(ServletContainerUtil.class);
}
