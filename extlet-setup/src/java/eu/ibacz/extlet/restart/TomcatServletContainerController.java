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

import com.liferay.portal.kernel.util.ServerDetector;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Support for Tomcat container. This class use {@link TomcatRestarter} for providing portal restart functionality.
 * @author Tomáš Polešovský
 */
public class TomcatServletContainerController implements ServletContainerController {
    /**
     * Returns {@link ServerDetector#TOMCAT_ID}
     */
    public String getServerDetectorContainerId() {
        return ServerDetector.TOMCAT_ID;
    }

    /**
     * Returns true if stracktrace contains <pre>org.apache.catalina.startup.Catalina.start(</pre>
     */
    public boolean isStarting() {
        // I know this is a raw superhack, but this is the only one I was able to invent so as it would be simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        return sw.toString().contains("org.apache.catalina.startup.Catalina.start(");
    }

    /**
     * Returns true if stracktrace contains <pre>org.apache.catalina.startup.Catalina.stop(</pre>
     */
    public boolean isShuttingDown() {
        // Another one raw superhack, again, this is the only one I was able to invent so as it would be still simple :D
        StringWriter sw = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(sw));
        return sw.toString().contains("org.apache.catalina.startup.Catalina.stop(");
    }

    /**
     * Asks TomcatRestarter if Tomcat is restarting
     */
    public boolean isTriggeredRestart() {
        return TomcatRestarter.getRestartPhase() == TomcatRestarter.PHASE_RESTARTING;
    }


    /**
     * Using TomcatRestarter initiates Portal restart
     */
    public void restartPortal() {
        TomcatRestarter.startThread();
    }

}
