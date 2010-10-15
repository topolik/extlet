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


/**
 * Interface describing methods needed to implement by each Servlet Container
 * @author Tomáš Polešovský
 */
public interface ServletContainerController {
    /**
     * Method should return {@link com.liferay.portal.kernel.util.ServerDetector} constant.
     */
    String getServerDetectorContainerId();

    /**
     * Method returns true if current container is starting (i.e. application is initializing, not deploying)
     */
    boolean isStarting();

    /**
     * Method returns true if current container is destroying (i.e. application is stopping, not undeploying)
     */
    boolean isShuttingDown();

    /**
     * Returns true if Extlet triggered restart which is still in progress
     */
    boolean isTriggeredRestart();

    /**
     * Method should restart server or inform user using console that there is no support for restart.
     */
    void restartPortal();

}
