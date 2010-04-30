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

package com.liferay.portal.dao.jdbc.util;

import com.liferay.portal.kernel.jndi.JNDIUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.SortedProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PropsUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import com.mchange.v2.c3p0.DataSources;
import java.util.Enumeration;
import java.util.Properties;

import javax.naming.InitialContext;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Changes to this class fixes DB connection leak.<br />
 * <br />
 * Note: This class is the same as the portal's DataSourceFactoryBean.<br />
 * <br />
 * It introduces new method {@link DataSourceFactoryBean#destroyInstance(java.lang.Object) } 
 * which is used to close database connection.
 *
 * @author Brian Wing Shun Chan
 * @author Tomáš Polešovský
 */
public class DataSourceFactoryBean extends AbstractFactoryBean {

	public Class<?> getObjectType() {
		return DataSource.class;
	}

	public void setPropertyPrefix(String propertyPrefix) {
		_propertyPrefix = propertyPrefix;
	}

	protected Object createInstance() throws Exception {
		Properties properties = PropsUtil.getProperties(_propertyPrefix, true);

		String jndiName = properties.getProperty("jndi.name");

		if (Validator.isNotNull(jndiName)) {
			try {
				return JNDIUtil.lookup(new InitialContext(), jndiName);
			}
			catch (Exception e) {
				_log.error("Unable to lookup " + jndiName, e);
			}
		}

		DataSource dataSource = new ComboPooledDataSource();

		Enumeration<String> enu =
			(Enumeration<String>)properties.propertyNames();

		while (enu.hasMoreElements()) {
			String key = enu.nextElement();

			String value = properties.getProperty(key);

			// Map org.apache.commons.dbcp.BasicDataSource to C3PO

			if (key.equalsIgnoreCase("driverClassName")) {
				key = "driverClass";
			}
			else if (key.equalsIgnoreCase("url")) {
				key = "jdbcUrl";
			}
			else if (key.equalsIgnoreCase("username")) {
				key = "user";
			}

			BeanUtils.setProperty(dataSource, key, value);
		}

		if (_log.isDebugEnabled()) {
			SortedProperties sortedProperties = new SortedProperties(
				properties);

			_log.debug("Properties for prefix " + _propertyPrefix);

			sortedProperties.list(System.out);
		}

		return dataSource;
	}

	protected void destroyInstance(Object instance) throws Exception {
        // instance should be ComboPooledDataSource
        if(instance instanceof ComboPooledDataSource){
            DataSources.destroy((DataSource) instance);
        } else {
            _log.fatal("Cannot close DB DataSource. ["+instance.getClass().getName()+"] not instance of the ["+ComboPooledDataSource.class.getName()+"]");
        }
    }

	private static Log _log =
		 LogFactoryUtil.getLog(DataSourceFactoryBean.class);

	private String _propertyPrefix;

}
