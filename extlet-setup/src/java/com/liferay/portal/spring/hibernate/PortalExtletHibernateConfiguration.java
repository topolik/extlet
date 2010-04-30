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
package com.liferay.portal.spring.hibernate;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import org.springframework.core.io.UrlResource;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.hibernate.cfg.Configuration;

/**
 * Enables overridding of the portal hibernate configuration files *-hbm.xml using the META-INF/extlet-hbm.xml.
 * 
 * @author Tomáš Polešovský
 */
public class PortalExtletHibernateConfiguration extends PortalHibernateConfiguration {

    @Override
	protected Configuration newConfiguration() {
		Configuration configuration = super.newConfiguration();

		ClassLoader classLoader = getConfigurationClassLoader();

        String resourceName = EXTLET_HIBERNATE_RESOURCE_NAME;

        try {
            Enumeration<URL> resources = classLoader.getResources(resourceName);
		    while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
		        try {
                    if(_log.isDebugEnabled()){
                        _log.debug("Loading extlet-hbm.xml from: " + resource);
                    }
		            InputStream is = new UrlResource(resource).getInputStream();

		            if (is != null) {
                        try {
                            configuration.addInputStream(is);
                        } finally {
                            is.close();
                        }
		            }
                    if(_log.isDebugEnabled()){
                        _log.debug("Loading OK: " + resource);
                    }
		        }
		        catch (Exception e2) {
			        if (_log.isWarnEnabled()) {
				        _log.warn("Problem while loading " + resource, e2);
			        }
		        }
            }
        }
        catch (Exception e2) {
	        if (_log.isWarnEnabled()) {
		        _log.warn("Problem while loading classLoader resources: " + resourceName, e2);
	        }
        }

		return configuration;
	}

    public final String EXTLET_HIBERNATE_RESOURCE_NAME = "META-INF/extlet-hbm.xml";

    private static Log _log =
		LogFactoryUtil.getLog(PortalExtletHibernateConfiguration.class);

}
