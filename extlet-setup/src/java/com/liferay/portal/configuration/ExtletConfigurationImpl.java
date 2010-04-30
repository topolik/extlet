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
package com.liferay.portal.configuration;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.CompanyConstants;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import com.liferay.portal.util.PropsFiles;

import org.springframework.core.io.UrlResource;

/**
 * Properties specified in the META-INF/extlet-portal.properties file can extend/override existing values in the portal.properties file.<br />
 * <ul><li> key+=value: appends value after the existing values within the same key</li>
 * <li> key=value: defines new value for the key and overrides existing values</li></ul>
 *
 * @author Tomáš Polešovský
 * @author Jaromír Hamala
 *
 */
public class ExtletConfigurationImpl
        extends com.liferay.portal.configuration.ConfigurationImpl {

    public ExtletConfigurationImpl(ClassLoader classLoader, String name) {
        this(classLoader, name, CompanyConstants.SYSTEM);
    }

    public ExtletConfigurationImpl(ClassLoader classLoader, String name, long companyId) {
        super(classLoader, name, companyId);

        // add extlet properties to the portal.properties
        if (name.equals(PropsFiles.PORTAL)) {
            addExtletProperties();
        }
    }

    /**
     * Find all extlet-portal.properties in all jars and override actual content
     */
    public void addExtletProperties() {
        ClassLoader classLoader = getClass().getClassLoader();
        String resourceName = EXTLET_PORTAL_PROPERTIES;

        try {
            // load all resource file from the classpath (all jars).
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Loading extlet-portal.properties from: " + resource);
                    }
                    InputStream is = new UrlResource(resource).getInputStream();

                    if (is != null) {
                        addPropertiesToLiferay(is);
                    }
                    if (_log.isDebugEnabled()) {
                        _log.debug("Loading OK: " + resource);
                    }
                } catch (Exception e2) {
                    if (_log.isWarnEnabled()) {
                        _log.warn("Problem while loading " + resource, e2);
                    }
                }
            }
        } catch (Exception e2) {
            if (_log.isWarnEnabled()) {
                _log.warn("Problem while loading classLoader resources: " + resourceName, e2);
            }
        }
    }

    /**
     * Merge the .properties file with the portal.properties
     */
    protected void addPropertiesToLiferay(InputStream is) throws IOException {
        if (is == null) {
            return;
        }
        try {
            Properties props = new Properties();
            props.load(is);

            Properties newProps = new Properties();
            for (Iterator iter = props.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                if (_log.isDebugEnabled()) {
                    _log.debug("Key found: " + key + ", value is " + props.getProperty(key));
                }

                String newValue;
                String newKey;
                // key+=value
                if (key.endsWith(PLUS_SIGN)) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Key is ending with +, so we will merge the properties.");
                    }
                    newKey = key.substring(0, key.length() - 1);
                    if (_log.isDebugEnabled()) {
                        _log.debug("New key is: " + newKey);
                    }

                    String oldValue = get(newKey);
                    if ((oldValue == null) || ("".equals(oldValue.trim()))) {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Old value not found, creating new property.");
                        }
                        newValue = props.getProperty(key);
                    } else {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Old value found, it's: " + oldValue + ", therefore I'm merging.");
                        }
                        newValue = oldValue + "," + props.getProperty(key);
                    }

                } else {
                    // key=value
                    if (_log.isDebugEnabled()) {
                        _log.debug("Key is not ending with +, so I'm in overwrite more.");
                    }
                    newValue = props.getProperty(key);
                    newKey = key;
                }

                if (_log.isDebugEnabled()) {
                    _log.debug("Key is: " + newKey);
                    _log.debug("Value is: " + newValue);
                }

                newProps.setProperty(newKey, newValue);
            }

            // add properties to the original one
            addProperties(newProps);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    public static final String EXTLET_PORTAL_PROPERTIES = "META-INF/extlet-portal.properties";
    private static String PLUS_SIGN = "+";
    private static Log _log = LogFactoryUtil.getLog(ExtletConfigurationImpl.class);
}
