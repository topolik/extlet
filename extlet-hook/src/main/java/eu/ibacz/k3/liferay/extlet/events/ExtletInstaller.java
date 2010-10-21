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
package eu.ibacz.k3.liferay.extlet.events;

import java.io.File;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PortalUtil;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This action unpacks Extlet Setup jar into portal's <code>WEB-INF/classes</code> directory.
 * Installer takes no action in case the file is already installed.  
 *  
 * @author Petr Vlcek
 * @author Tomáš Polešovský
 *
 */
public class ExtletInstaller extends SimpleAction {

    @Override
    public void run(String[] arg0) throws ActionException {
        if (!fileExists("META-INF/extlet-setup-spring.xml")) {
            installFile("extlet-setup.jar");

            if(_log.isWarnEnabled()){_log.warn("Extlet environment has been successfully installed. Please restart your server.");}
        } else {
            _log.info("Extlet environment is present.");
        }
    }

    private void installFile(String fileName) {
        String source = getHookLibDir() + fileName;
        String destinationDir = getPortalWebInfDir();

        _log.info("Unpacking " + source + " to " + destinationDir);

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(source));
            ZipEntry e = null;
            while ((e = zis.getNextEntry()) != null) {
                File outputFile = new File(destinationDir + e.getName());
                if(e.isDirectory()){
                    outputFile.mkdirs();
                    zis.closeEntry();
                    continue;
                }

                FileOutputStream out = new FileOutputStream(outputFile);
                try {
                    // Transfer bytes from the ZIP file to the output file
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = zis.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    // Close the stream
                    if (out != null) {
                        out.close();
                    }
                    zis.closeEntry();
                }

            }
        } catch (FileNotFoundException ex) {
            _log.error("Cannot deploy extlet environment!", ex);
        } catch (IOException ex) {
            _log.error("Cannot deploy extlet environment!", ex);
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException ex) {
                    // don't need to do anything
                }
            }
        }
    }

    private boolean fileExists(String fileName) {
        File file = new File(getPortalWebInfDir() + fileName);
        return file.exists();
    }

    private String getPortalWebInfDir() {
        return PortalUtil.getPortalWebDir() + "WEB-INF/classes/";
    }

    private String getHookLibDir() {
        String classesDir = ExtletInstaller.class.getClassLoader().getResource("/").toString().replaceAll("^file:", "");
        String hookLibDir = classesDir.substring(0, classesDir.indexOf("/classes")) + "/lib/";

        return hookLibDir;
    }
    private static Log _log = LogFactoryUtil.getLog(ExtletInstaller.class);
}
