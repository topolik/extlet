/**
 * Copyright (c) 2010 IBA CZ, s. r. o.
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
package com.liferay.portal.struts;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.InputStream;

import java.net.URL;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;


import java.util.Set;
import org.apache.struts.util.MessageResourcesFactory;
import org.springframework.core.io.UrlResource;

/**
 * Enables use of the <code>content/Language-extlet(_en|_cs_CZ|...).properties</code> file, which overriddes
 * the original <code>content/Language.properties</code> file.<br />
 * <br />
 * Note: The only change is the method {@link MultiMessageResources#loadLocale(java.lang.String)},
 * which also calls {@link MultiMessageResources#_loadPropsExtlet(java.lang.String)} to load the extlet files.
 *
 * @author Tomáš Polešovský
 *
 */
public class ExtletMultiMessageResources extends MultiMessageResources {

    public ExtletMultiMessageResources(
            MessageResourcesFactory factory, String config) {

        super(factory, config, true);
    }

    public ExtletMultiMessageResources(
            MessageResourcesFactory factory, String config, boolean returnNull) {

        super(factory, config, returnNull);

    }

    public void loadLocale(String localeKey) {
        super.loadLocale(localeKey);
        _loadPropsExtlet(localeKey);
    }

    /**
     * Load properties from the <code>content/Language-extlet.properties</code> files.
     *
     * @param localeKey Locale to use in the name of the file.
     */
    private void _loadPropsExtlet(String localeKey) {
        synchronized(this){
            if (extletLocales.contains(localeKey)) {
                return;
            }
            extletLocales.add(localeKey);
        }

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // load content/Language-extlet.properties
            String resourceName = "content/Language-extlet" + (localeKey.length() > 0 ? "_" : "") + localeKey + ".properties";
            Enumeration<URL> resources = classLoader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource == null) {
                    continue;
                }

                InputStream is = null;
                try {
                    is = new UrlResource(resource).getInputStream();
                    Properties props = new Properties();
                    props.load(is);

                    putMessages(props, localeKey);

                    if (_log.isInfoEnabled()) {
                        _log.info(
                                "Loading " + resource + " with " + props.size() + " values");
                    }
                } catch (Exception e) {
                    _log.error("Problem while loading file " + resource, e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {
                            _log.error("Problem while closing input stream " + resource, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            _log.error("Problem while loading extlet content/Language-ext.properties files.", e);
        }

    }
    private static Log _log =
            LogFactoryUtil.getLog(ExtletMultiMessageResources.class);
    protected Set extletLocales = new HashSet();
}
