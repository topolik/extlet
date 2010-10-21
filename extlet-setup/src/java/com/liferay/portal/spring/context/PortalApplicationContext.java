/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.spring.context;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PropsKeys;
import com.liferay.portal.util.PropsUtil;

import java.io.FileNotFoundException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * <a href="PortalApplicationContext.java.html"><b><i>View Source</i></b></a>
 *
 * <p>
 * This web application context will first load bean definitions in the
 * contextConfigLocation parameter in web.xml. Then, the context will load bean
 * definitions specified by the property "spring.configs" in portal.properties.
 * </p>
 *
 * <p>
 * Note: The class is the same as the original PortalApplicationContext,
 * because it contains the only method that has been changed.<br />
 * The only change is that it loads also the: <ul>
 * <li>META-INF/extlet-setup-spring.xml - extlet runtime environment initialization</li>
 * <li>META-INF/extlet-spring.xml - all other extlets spring configs</li>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @author Alexander Chow
 * @author Tomáš Polešovský
 *
 */
public class PortalApplicationContext extends XmlWebApplicationContext {

	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) {
		try {
			super.loadBeanDefinitions(reader);
		}
		catch (Exception e) {
			if (_log.isWarnEnabled()) {
				_log.warn(e, e);
			}
		}

		reader.setResourceLoader(new DefaultResourceLoader());

		String[] configLocations = PropsUtil.getArray(PropsKeys.SPRING_CONFIGS);

		if (configLocations == null) {
			return;
		}

		for (String configLocation : configLocations) {
			try {
				reader.loadBeanDefinitions(configLocation);
			}
			catch (Exception e) {
				Throwable cause = e.getCause();

				if (cause instanceof FileNotFoundException) {
					if (_log.isWarnEnabled()) {
						_log.warn(cause.getMessage());
					}
				}
				else {
					_log.error(e, e);
				}
			}
		}


        /*
         * MODIFICATION: load also our extlet-spring.xml from all jars on the classpath
         */
		reader.setResourceLoader(new PathMatchingResourcePatternResolver());
		try {
            // load setup of extlet environment
            if(_log.isDebugEnabled()) {
                _log.debug("Loading Extlet Setup Spring environment");
            }
			reader.loadBeanDefinitions(EXTLET_SETUP_SPRING_FILE_DEFINITION);
            if(_log.isDebugEnabled()) {
                _log.debug("Loading Extlet Spring environment - OK");
            }
            // load other extlets' spring files
            if(_log.isDebugEnabled()) {
                _log.debug("Loading Other Extlet Spring environments");
            }
			reader.loadBeanDefinitions(EXTLET_SPRING_FILE_DEFINITION);
            if(_log.isDebugEnabled()) {
                _log.debug("Loading Other Extlet Spring environments - OK");
            }
		}
		catch (Exception e) {
			Throwable cause = e.getCause();

			if (cause instanceof FileNotFoundException) {
				if (_log.isWarnEnabled()) {
					_log.warn(cause.getMessage());
				}
			}
			else {
				_log.error(e, e);
			}
		}
	}

    /**
     * Defines META-INF/extlet-setup-spring.xml file in all jar files that the classloader see.
     * This file is for the extlet environment and should not be overridden in the extlets!
     */
    public final String EXTLET_SETUP_SPRING_FILE_DEFINITION = "classpath*:META-INF/extlet-setup-spring.xml";

    /**
     * Defines META-INF/extlet-spring.xml file in all jar files that the classloader see.
     * In this file you can specify the changes in the portal ApplicationContext.
     */
    public final String EXTLET_SPRING_FILE_DEFINITION = "classpath*:META-INF/extlet-spring.xml";

    private static Log _log =
		LogFactoryUtil.getLog(PortalApplicationContext.class);

}
