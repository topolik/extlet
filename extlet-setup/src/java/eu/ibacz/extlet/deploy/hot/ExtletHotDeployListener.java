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

import java.util.Properties;

import com.liferay.portal.deploy.hot.BaseHotDeployListener;
import com.liferay.portal.kernel.deploy.hot.HotDeployEvent;
import com.liferay.portal.kernel.deploy.hot.HotDeployException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PropsKeys;

/**
 * HotDeployListener that copies (removes) extlet portal jar that modifies Liferay's classes
 * in the (un)deploy phase.
 * 
 * @author Tomáš Polešovský
 * @author Jaromír Hamala 
 * 
 */
public class ExtletHotDeployListener extends BaseHotDeployListener implements PropsKeys {
    public void invokeDeploy(HotDeployEvent event) throws HotDeployException {
    	Properties extletProperties = ExtletPropsUtils.getExtletProperties(event);
		
    	if (extletProperties != null) {
    		ExtletHotDeployer extletHotDeployer = new ExtletHotDeployer(event, extletProperties);
    		extletHotDeployer.deployPortalExtlet();
    	}
    }

    public void invokeUndeploy(HotDeployEvent event) throws HotDeployException {
    	Properties extletProperties = ExtletPropsUtils.getExtletProperties(event);
		
    	if (extletProperties != null) {
    		ExtletHotDeployer extletHotDeployer = new ExtletHotDeployer(event, extletProperties);
    		extletHotDeployer.unDeployPortalExtlet();
    	}
    }

    private static Log _log =
            LogFactoryUtil.getLog(ExtletHotDeployListener.class);
}
