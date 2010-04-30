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

package com.liferay.portal.util;

import com.liferay.portal.configuration.ExtletConfigurationImpl;
import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ServerDetector;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.CompanyConstants;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.util.SystemProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Changed the original implementation of Configuration from ConfigurationImpl to ExtletConfigurationImpl.<br />
 *
 * Note: This class is the same as the original one, because it is referenced directly from the code.<br />
 * <br />
 * There is changed {@link PropsUtil#_getConfiguration()} method and private constructor {@link PropsUtil#PropsUtil() },
 * they both now use the {@link ExtletConfigurationImpl} class instead of the <code>ConfigurationImpl</code>.
 *
 * @author Brian Wing Shun Chan
 * @author Tomáš Polešovský
 *
 */
public class PropsUtil {

	public static void addProperties(Properties properties) {
		_instance._addProperties(properties);
	}

	public static boolean contains(String key) {
		return _instance._contains(key);
	}

	public static String get(String key) {
		return _instance._get(key);
	}

	public static String get(String key, Filter filter) {
		return _instance._get(key, filter);
	}

	public static String[] getArray(String key) {
		return _instance._getArray(key);
	}

	public static String[] getArray(String key, Filter filter) {
		return _instance._getArray(key, filter);
	}

	public static Properties getProperties() {
		return _instance._getProperties();
	}

	public static Properties getProperties(
		String prefix, boolean removePrefix) {

		return _instance._getProperties(prefix, removePrefix);
	}

	public static void removeProperties(Properties properties) {
		_instance._removeProperties(properties);
	}

	public static void set(String key, String value) {
		_instance._set(key, value);
	}

	private PropsUtil() {
		SystemProperties.set("default.liferay.home", _getDefaultLiferayHome());

		_configuration = new ExtletConfigurationImpl(
			PropsUtil.class.getClassLoader(), PropsFiles.PORTAL);

		String liferayHome = _get(PropsKeys.LIFERAY_HOME);

		SystemProperties.set(
			"ehcache.disk.store.dir", liferayHome + "/data/ehcache");

		if (GetterUtil.getBoolean(
				SystemProperties.get("company-id-properties"))) {

			_configurations = new HashMap<Long, Configuration>();
		}
	}

	private void _addProperties(Properties properties) {
		_getConfiguration().addProperties(properties);
	}

	private boolean _contains(String key) {
		return _getConfiguration().contains(key);
	}

	private String _get(String key) {
		return _getConfiguration().get(key);
	}

	private String _get(String key, Filter filter) {
		return _getConfiguration().get(key, filter);
	}

	private String[] _getArray(String key) {
		return _getConfiguration().getArray(key);
	}

	private String[] _getArray(String key, Filter filter) {
		return _getConfiguration().getArray(key, filter);
	}

	private Configuration _getConfiguration() {
		if (_configurations == null) {
			return _configuration;
		}

		long companyId = CompanyThreadLocal.getCompanyId();

		if (companyId > CompanyConstants.SYSTEM) {
			Configuration configuration = _configurations.get(companyId);

			if (configuration == null) {
				configuration = new ExtletConfigurationImpl(
					PropsUtil.class.getClassLoader(), PropsFiles.PORTAL,
					companyId);

				_configurations.put(companyId, configuration);
			}

			return configuration;
		}
		else {
			return _configuration;
		}
	}

	private String _getDefaultLiferayHome() {
		String defaultLiferayHome = null;

		if (ServerDetector.isGeronimo()) {
			defaultLiferayHome =
				SystemProperties.get("org.apache.geronimo.base.dir") + "/..";
		}
		else if (ServerDetector.isGlassfish()) {
			defaultLiferayHome =
				SystemProperties.get("com.sun.aas.installRoot") + "/..";
		}
		else if (ServerDetector.isJBoss()) {
			defaultLiferayHome = SystemProperties.get("jboss.home.dir") + "/..";
		}
		else if (ServerDetector.isJOnAS()) {
			defaultLiferayHome = SystemProperties.get("jonas.base") + "/..";
		}
		else if (ServerDetector.isWebLogic()) {
			defaultLiferayHome =
				SystemProperties.get("env.DOMAIN_HOME") + "/..";
		}
		else if (ServerDetector.isJetty()) {
			defaultLiferayHome = SystemProperties.get("jetty.home") + "/..";
		}
		else if (ServerDetector.isResin()) {
			defaultLiferayHome = SystemProperties.get("resin.home") + "/..";
		}
		else if (ServerDetector.isTomcat()) {
			defaultLiferayHome = SystemProperties.get("catalina.base") + "/..";
		}
		else {
			defaultLiferayHome = SystemProperties.get("user.home") + "/liferay";
		}

		defaultLiferayHome = StringUtil.replace(
			defaultLiferayHome, StringPool.BACK_SLASH, StringPool.SLASH);

		defaultLiferayHome = StringUtil.replace(
			defaultLiferayHome, StringPool.DOUBLE_SLASH, StringPool.SLASH);

		if (defaultLiferayHome.endsWith("/..")) {
			int pos = defaultLiferayHome.lastIndexOf(
				StringPool.SLASH, defaultLiferayHome.length() - 4);

			if (pos != -1) {
				defaultLiferayHome = defaultLiferayHome.substring(0, pos);
			}
		}

		return defaultLiferayHome;
	}

	private Properties _getProperties() {
		return _getConfiguration().getProperties();
	}

	private Properties _getProperties(String prefix, boolean removePrefix) {
		return _getConfiguration().getProperties(prefix, removePrefix);
	}

	private void _removeProperties(Properties properties) {
		_getConfiguration().removeProperties(properties);
	}

	private void _set(String key, String value) {
		_getConfiguration().set(key, value);
	}

	private static PropsUtil _instance = new PropsUtil();

	private Configuration _configuration;
	private Map<Long, Configuration> _configurations;

}
