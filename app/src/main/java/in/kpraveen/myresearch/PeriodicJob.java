package in.kpraveen.myresearch;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class PeriodicJob extends JobService {
    public static int PERIODIC_JOB_ID = 500;

    public PeriodicJob() {
    }

    static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, LocationJob.class);
            JobInfo jobInfo = new JobInfo.Builder(PERIODIC_JOB_ID, componentName)
                    .setMinimumLatency(1)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).
                            build();
            if (jobInfo != null) {
                int ret = jobScheduler.schedule(jobInfo);
                Log.d(ApplicationData.TAG, "Scheduled " + PERIODIC_JOB_ID);
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // If location job not scheduled then schedule it
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            boolean jobScheduled = false;
            List<JobInfo> list = jobScheduler.getAllPendingJobs();
            for (JobInfo jobInfo : list) {
                if (jobInfo.getId() >= LocationJob.START_JOB_ID &&  jobInfo.getId() <= LocationJob.END_JOB_ID) {
                    jobScheduled = true;
                }
            }
            if (!jobScheduled) {
                LocationJob.scheduleJob(LocationJob.START_JOB_ID, this, 1);
            }
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
