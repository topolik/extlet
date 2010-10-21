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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import com.liferay.portal.kernel.deploy.hot.HotDeployEvent;
import com.liferay.portal.kernel.deploy.hot.HotDeployException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.util.PropsUtil;

/**
 * Util class for loading the liferay-extlet.properties file from each extlet WAR file.
 * Configuration can be placed both in the <ul>
 * <li>/WEB-INF/liferay-extlet.properties file</li>
 * <li>/META-INF/liferay-extlet.properties file</li>
 * </ul>
 * <br />
 * This configuration can be changed using specification below, in the portal properties:<br />
 * <code>eu.ibacz.extlet.deploy.hot.extletProperties=/WEB-INF/liferay-extlet.properties,/META-INF/liferay-extlet.properties</code>
 *
 *
 * @author Tomáš Polešovský
 */
public class ExtletPropsUtils {

    /**
     * Definition of the configuration files, default configuration is the
     * <code>eu.ibacz.extlet.deploy.hot.extletProperties=/WEB-INF/liferay-extlet.properties,/META-INF/liferay-extlet.properties</code>
     */
    public static final String[] EXTLET_PROPERTIES = StringUtil.split(
            GetterUtil.getString(PropsUtil.get("eu.ibacz.extlet.deploy.hot.extletProperties"),
            "/WEB-INF/liferay-extlet.properties,/META-INF/liferay-extlet.properties"));

    protected static Properties getExtletProperties(HotDeployEvent event) throws HotDeployException {
        for (String propertiesLocation : EXTLET_PROPERTIES) {
            if (_log.isDebugEnabled()) {
                _log.debug("Loading extlet properties: " + propertiesLocation);
            }
            URL properties;
            try {
                properties = event.getServletContext().getResource(propertiesLocation);
            } catch (MalformedURLException e) {
                throw new HotDeployException("Cannot open property file", e);
            }

            if (_log.isDebugEnabled()) {
                _log.debug("Loading extlet properties from " + properties);
            }
            InputStream is = null;

            try {
                if (properties != null) {
                    is = properties.openStream();
                }

                if (is != null) {
                    String propertiesString = StringUtil.read(is);

                    if (_log.isDebugEnabled()) {
                        _log.debug("Extlet properties loaded from " + properties);
                    }
                    return PropertiesUtil.load(propertiesString);
                }

            } catch (Exception e) {
                _log.error(event.getServletContextName() + ": " + e.toString(), e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        if (_log.isDebugEnabled()) {
                            _log.debug(ioe);
                        }
                    }
                }
            }
        }

        if (_log.isDebugEnabled()) {
            _log.debug(
                    event.getServletContextName() + " does not have any of these: "
                    + Arrays.asList(EXTLET_PROPERTIES));
        }

        return null;
    }
    private static Log _log =
            LogFactoryUtil.getLog(ExtletPropsUtils.class);
}
