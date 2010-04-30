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
import javax.servlet.ServletException;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryConfig;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.TilesUtil;

/**
 * The class redefines new ExtletTilesDefinitionsFactory for loading the configuration files.
 *
 * @author Ondřej Životský (ondrej.zivotsky@ibacz.eu)
 * @author Tomáš Polešovský
 *
 */
public class ExtletPortalTilesPlugin extends PortalTilesPlugin {

    DefinitionsFactoryConfig config = null;

    /**
     * Name of the Extlet Tiles configuration file
     */
    public static final String EXTLET_TILES_FILE_NAME = "META-INF/extlet-tiles-defs.xml";

    /**
     * Reread DefinitionsFactoryConfig, throw away the old one
     */
    public void init(ActionServlet servlet, ModuleConfig moduleConfig) throws ServletException {
        super.init(servlet, moduleConfig);

        try {
            // create new one
            DefinitionsFactory newDefinitionFactory = TilesUtil.createDefinitionsFactory(servlet.getServletContext(), config);

            // then DESTROY THE OLD defitionFactory
            if (definitionFactory != null) {
                definitionFactory.destroy();
            }

            definitionFactory = newDefinitionFactory;

        } catch (DefinitionsFactoryException ex) {
            _log.error("Cannot create extlet definition factory!", ex);
        }
    }

    /**
     * Cache the config
     */
    protected synchronized DefinitionsFactoryConfig readFactoryConfig(ActionServlet servlet, ModuleConfig moduleConfig) throws ServletException {
        // read factory config (parent implementation]
        config = super.readFactoryConfig(servlet, moduleConfig);
        return config;
    }


	private static Log _log =
		 LogFactoryUtil.getLog(ExtletPortalTilesPlugin.class);
}

