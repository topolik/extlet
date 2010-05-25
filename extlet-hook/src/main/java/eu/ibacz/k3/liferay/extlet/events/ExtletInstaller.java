package eu.ibacz.k3.liferay.extlet.events;

import java.io.File;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.util.PortalUtil;

/**
 * This action installs Extlet Setup jar into portal's <code>WEB-INF/lib</code> directory. 
 * Installer takes no action in case the file is already installed.  
 *  
 * @author Petr Vlcek
 *
 */
public class ExtletInstaller extends SimpleAction {

	@Override
	public void run(String[] arg0) throws ActionException {
		if (!fileExists("extlet-setup.jar")) {
			installFile("extlet-setup.jar");
			
			_log.info("Extlet environment has been successfully installed. Please restart your server.");			
		} else {
			_log.info("Extlet environment is present.");
		}
	}
	
	private void installFile(String fileName) {
		String source = getHookLibDir() + fileName;
		String destination = getPortalLibdir() + fileName;
		
		_log.info("Copying " + source + " to " + destination + ".");
		
		FileUtil.copyFile(source, destination);
	}
	
	private boolean fileExists(String fileName) {
		File file = new File(getPortalLibdir() + fileName);
		
		return file.exists();
	}
	
	private String getPortalLibdir() {
		return PortalUtil.getPortalLibDir();
	}
	
	private String getHookLibDir() {				
		String classesDir = ExtletInstaller.class.getClassLoader().getResource("/").toString().replaceAll("^file:", "");		
		String hookLibDir = classesDir.substring(0, classesDir.indexOf("/classes")) + "/lib/";
		
		return hookLibDir;		
	}
	
	private static Log _log = LogFactoryUtil.getLog(ExtletInstaller.class);

}
