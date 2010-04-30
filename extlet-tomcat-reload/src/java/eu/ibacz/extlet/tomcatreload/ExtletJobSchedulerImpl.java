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
package eu.ibacz.extlet.tomcatreload;

import com.liferay.portal.job.JobSchedulerImpl;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Class that add funcitonality to delete all scheduled jobs
 * @author Tomáš Polešovský
 */
public class ExtletJobSchedulerImpl extends JobSchedulerImpl {

    @Override
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        super.setScheduler(scheduler);
    }

    public void deleteAllJobs() throws SchedulerException {
        for (String groupName : scheduler.getJobGroupNames()) {
            for (String jobName : scheduler.getJobNames(groupName)) {
                if(_log.isDebugEnabled()){
                    _log.debug("DELETING: " + jobName);
                }
                try {
                    scheduler.deleteJob(jobName, Scheduler.DEFAULT_GROUP);
                } catch (SchedulerException ex) {
                    _log.error("Cannot delete job " + jobName, ex);
                }
            }
        }
    }
    private Scheduler scheduler;
    private static Log _log = LogFactoryUtil.getLog(ExtletJobSchedulerImpl.class);
}
