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

package com.liferay.portal.spring.servlet;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.spring.context.TunnelApplicationContext;
import com.liferay.portal.util.PortalInstances;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * Enables configuring of the remoting servlet also using the META-INF/extlet-remoting-servlet.xml.<br />
 * <br />
 * Note: The class is the same as the original RemotingServlet,
 * because servlets can't be easily overridden.<br />
 * The only change is definition of the final field {@link RemotingServlet#CONTEXT_CONFIG_LOCATION}.
 *
 * @author Brian Wing Shun Chan
 * @author Tomáš Polešovský
 *
 */
public class RemotingServlet extends DispatcherServlet {

	public static final String CONTEXT_CLASS =
		TunnelApplicationContext.class.getName();

    /**
     * Configuration is loaded from:<ul>
     * <li>portal /WEB-INF/remoting-servlet.xml</li>
     * <li>portal /WEB-INF/remoting-servlet-ext.xml</li>
     * <li>all META-INF/extlet-remoting-servlet.xml on the classpath</li>
     */
	public static final String CONTEXT_CONFIG_LOCATION =
		"/WEB-INF/remoting-servlet.xml,/WEB-INF/remoting-servlet-ext.xml,classpath*:META-INF/extlet-remoting-servlet.xml";

	public Class<?> getContextClass() {
		try {
			return Class.forName(CONTEXT_CLASS);
		}
		catch (Exception e) {
			_log.error(e);
		}

		return null;
	}

	public String getContextConfigLocation() {
		return CONTEXT_CONFIG_LOCATION;
	}

	public void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException {

		try {
			String remoteUser = request.getRemoteUser();

			if (_log.isDebugEnabled()) {
				_log.debug("Remote user " + remoteUser);
			}

			long companyId = PortalInstances.getCompanyId(request);

			CompanyThreadLocal.setCompanyId(companyId);

			if (remoteUser != null) {
				PrincipalThreadLocal.setName(remoteUser);

				long userId = GetterUtil.getLong(remoteUser);

				User user = UserLocalServiceUtil.getUserById(userId);

				PermissionChecker permissionChecker =
					PermissionCheckerFactoryUtil.create(user, true);

				PermissionThreadLocal.setPermissionChecker(permissionChecker);
			}
			else {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"User id is not provided. An exception will be " +
							"thrown  if a protected method is accessed.");
				}
			}

			super.service(request, response);
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(RemotingServlet.class);

}
