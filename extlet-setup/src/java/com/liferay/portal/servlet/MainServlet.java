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
package com.liferay.portal.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.digester.Digester;
import org.xml.sax.InputSource;

/**
 * <p>This class replaces the original one, which is left untouched but renamed to {@link LiferayMainServlet}.</p>
 *
 * <p>Implemented functionality enables Extlet to load the Struts config files from each extlet.</p>
 *
 * @author Tomáš Polešovský
 * @author Ondřej Životský
 */
public class MainServlet extends LiferayMainServlet {
	/**
	 * Defines extlet-struts-config.xml file.
	 */
	public static final String EXTLET_STRUTS_CONFIG = "META-INF/extlet-struts-config.xml";

    /**
     * Count number of the extlet config files.
     *
     * We need for each extlet-struts-config.xml file add config file
     * to the super.config, so as the digester's stack is filled enough
     * to parse all the config files
     *
     * Also we need to change the possible comma character in the path
     * for something else, for example \u2615 - coffee :)
     * (which is 9749 in decimal, try <a href="http://www.google.com/search?q=%26%239749;">&#9749;</a> ;)
     */
    protected void initOther() throws ServletException {
        super.initOther();
        /*
         */

        StringBuilder sb = new StringBuilder(super.config);

        ClassLoader portalClassLoader = com.liferay.portal.kernel.util.PortalClassLoaderUtil.getClassLoader();
        try {
            Enumeration<URL> urls = portalClassLoader.getResources(EXTLET_STRUTS_CONFIG);
            while(urls.hasMoreElements()){                
                sb.append("," + urls.nextElement().toString().replaceAll(",", "\u2615"));
            }
        } catch (IOException ex) {
            log.error("Problem with gathering struts config files ", ex);
        }
        
        super.config = sb.toString();
    }

    /**
     * <p>Parses original struts config file calling super.parseModuleConfigFile(), then try to load all extlet config files.</p>
     */
    protected void parseModuleConfigFile(Digester digester, String path)
        throws UnavailableException {

        if(!path.contains(EXTLET_STRUTS_CONFIG)){
            super.parseModuleConfigFile(digester, path);
            return;
        }

		try {
            //load file (and replace back the coffee for comma)
            URL url = new URL(path.replaceAll("\u2615", ","));

            InputStream is = url.openStream();
            try {
                InputSource xmlStream = new InputSource(url.toExternalForm());
                xmlStream.setByteStream(is);
                digester.parse(is);
            } catch (Exception e) {
                log.error("Cannot load Extlet struts config file: " + url, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        log.error("Cannot close stream to the struts config file: " + url, e);
                    }
                }
            }
		} catch (Exception e) {
            log.error("Cannot load Extlet Struts config files!", e);
		}

    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        super.service(request, response);
    }

}
