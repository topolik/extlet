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
package eu.ibacz.extlet.conflict;

import java.io.File;

import com.liferay.portal.kernel.deploy.hot.HotDeployEvent;
import com.liferay.portal.kernel.deploy.hot.HotDeployException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import eu.ibacz.extlet.deploy.hot.ExtletHotDeployer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Check extlet jars for duplicity classes
 * @author Tomáš Polešovský
 */
public class ExtletConflictRegister {
    protected static final Object lock = new Object();
    protected static final Map<String, RegistryInfo> extletsOverridenFiles = new HashMap<String, RegistryInfo>();
    protected static final List<Pattern> mergeablePatterns = Arrays.asList(new Pattern[]{
                Pattern.compile("content/Language-extlet.*")
            });
    protected static final List<String> mergeableFiles = Arrays.asList(new String[]{
                "META-INF/MANIFEST.MF",
                "META-INF/extlet-hbm.xml",
                "META-INF/extlet-model-hints.xml",
                "META-INF/extlet-portal.properties",
                "META-INF/extlet-remoting-servlet.xml",
                "META-INF/extlet-spring.xml",
                "META-INF/extlet-struts-config.xml",
                "META-INF/extlet-tiles-defs.xml",
                "META-INF/liferay-portlet-extlet.xml",
                "META-INF/portlet-extlet.xml"
            });
    private HotDeployEvent event;

    public ExtletConflictRegister(HotDeployEvent event) {
        this.event = event;
    }

    public void register(List<File> jars) throws HotDeployException {
        int count = 0;
        synchronized(lock){
            count = extletsOverridenFiles.size();
        }
        for (File jar : jars) {
            try {
                RegistryInfo info = createRegistryInfo(jar);
                registerClasses(info);
            } catch (IOException ex) {
                throw new HotDeployException("Cannot open extlet jar file for conflict checking", ex);
            }
        }
        synchronized(lock){
            count = extletsOverridenFiles.size() - count;
        }
        _log.info("Conflict checking: Indexed " + count + " files for " + event.getServletContextName());
    }

    public void checkConflict(List<File> jars) throws HotDeployException{
        _log.info("Checking conflict files for " + event.getServletContextName());
        for (File jar : jars) {
            try {
                RegistryInfo info = createRegistryInfo(jar);
                checkConflict(info);
            } catch (IOException ex) {
                throw new HotDeployException("Cannot open extlet jar file for conflict checking", ex);
            }
        }
        _log.info("No conflicts found - OK");
    }

    protected void checkConflict(RegistryInfo info) throws HotDeployException{
        StringBuffer sb = new StringBuffer();
        synchronized(lock){
            for (String extletFile : info.getFiles()) {
                if(!isMergeable(extletFile) && extletsOverridenFiles.containsKey(extletFile)){
                    RegistryInfo deployed = extletsOverridenFiles.get(extletFile);
                    // redeploying
                    if(deployed.getCtxName().equals(info.getCtxName())){
                        continue;
                    }
                    sb.append("File conflict [conflicting-file]: ").append(extletFile);
                    sb.append("[existing-web-app, existing-jar]: [").append(deployed.getCtxName()).append(", ").append(deployed.getJarName()).append("] - ");
                    sb.append("[deploying-web-app, deploying-jar]: [").append(info.getCtxName()).append(", ").append(info.getJarName()).append("]\n");
                }
            }
        }
        if(sb.length() > 0){
            throw new HotDeployException("Cannot deploy "+info.getCtxName()+"!\n" + sb.toString());
        }
    }

    protected RegistryInfo createRegistryInfo(File jar) throws IOException {
        List<String> jarFiles = getZipEntries(jar);
        return new RegistryInfo(event.getServletContextName(), jar.getName(), jarFiles);
    }

    protected List<String> getZipEntries(File zip) throws IOException {
        ArrayList<String> result = new ArrayList<String>();

        ZipFile zipFile = new ZipFile(zip);
        Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            if(entry.isDirectory()){
                continue;
            }
            result.add(entry.getName());
        }

        return result;
    }

    protected void registerClasses(RegistryInfo info) {
        synchronized (lock) {
            for (String extletFile : info.getFiles()) {
                if(!isMergeable(extletFile)){
                    extletsOverridenFiles.put(extletFile, info);
                }
            }
        }
    }

    protected boolean isMergeable(String fileName){

        for(String mergeableFile : mergeableFiles){
            if(mergeableFile.equalsIgnoreCase(fileName)){
                return true;
            }
        }
        for (Pattern pattern : mergeablePatterns) {
            if(pattern.matcher(fileName).matches()){
                return true;
            }
        }
        return false;
    }

    private static Log _log =
            LogFactoryUtil.getLog(ExtletHotDeployer.class);
}