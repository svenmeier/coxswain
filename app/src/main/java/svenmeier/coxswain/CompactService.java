/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CompactService extends JobService {

    private static final long PERIOD = 4 * 60 * 60 * 1000l;

    private static final int WORKOUT_COUNT = 10;

    private Gym gym;

    @Override
    public void onCreate() {
        gym = Gym.instance(this);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        new Thread(new Compacter(jobParameters)).start();

        // asynchronous
        return true;
    }

    private class Compacter implements Runnable {

        private final JobParameters parameters;

        public Compacter(JobParameters jobParameters) {
            this.parameters = jobParameters;
        }

        @Override
        public void run() {
            gym.compact(WORKOUT_COUNT);

            jobFinished(parameters, false);
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public static void setup(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        JobInfo info = new JobInfo.Builder(42,
                new ComponentName(context, CompactService.class))
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(true)
                .setPeriodic(PERIOD)
                .build();

        jobScheduler.schedule(info);
    }
}
