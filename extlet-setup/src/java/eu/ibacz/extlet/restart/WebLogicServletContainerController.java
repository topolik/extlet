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
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Support for WebLogic. There is no support for reloading Liferay web app.
 * @author Tomáš Polešovský
 */
public class WebLogicServletContainerController implements ServletContainerController{

    /**
     * Returns {@link ServerDetector#WEBLOGIC_ID}
     */
    public String getServerDetectorContainerId() {
        return ServerDetector.WEBLOGIC_ID;
    }

    /**
     * Returns true if WebLogic is starting
     */
    public boolean isStarting() {
        // I know this is a raw superhack, but this is the only one I was able to invent so as it would be simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        String stacktrace = sw.toString();
        // liferay is also starting
        return stacktrace.contains("weblogic.servlet.internal.StubSecurityHelper.createServlet(");
    }

    /**
     * Returns true if WebLogic is shutting down
     */
    public boolean isShuttingDown() {
        // I know this is a raw superhack, but this is the only one I was able to invent so as it would be simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        String stacktrace = sw.toString();
        return
            stacktrace.contains("weblogic.management.deploy.internal.DeploymentServerService.stop(") ||
            stacktrace.contains("weblogic.application.internal.SingleModuleDeployment.deactivate(");
    }

    /**
     * There isn't support for triggering restart yet, thus returning false.
     */
    public boolean isTriggeredRestart() {
        return false;
    }

    /**
     * There isn't support for triggering restart yet.
     */
    public void restartPortal() {
        if (_log.isWarnEnabled()) {
            _log.warn("There is not support for WebLogic reload. Please restart WebLogic manually.");
        }
    }

    private static Log _log =
            LogFactoryUtil.getLog(WebLogicServletContainerController.class);
}
