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
package svenmeier.coxswain.view;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Snapshot;


/**
 */
public class SnapshotsFragment extends Fragment {

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private Handler handler;

    private ChartView speedView;
    private ChartView strokeRateView;
    private ChartView pulseView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.layout_snapshots, container, false);

        speedView = (ChartView) root.findViewById(R.id.snapshots_speed);
        strokeRateView = (ChartView) root.findViewById(R.id.snapshots_strokeRate);
        pulseView = (ChartView) root.findViewById(R.id.snapshots_pulse);

        speedView.setData(new AbstractData() {
            @Override
            public float max() {
                return 999;
            }

            @Override
            protected float value(Snapshot snapshot) {
                return snapshot.speed;
            }
        });
        strokeRateView.setData(new AbstractData() {
            @Override
            public float max() {
                return 50;
            }

            @Override
            protected float value(Snapshot snapshot) {
                return (float) snapshot.strokeRate;
            }
        });
        pulseView.setData(new AbstractData() {
            @Override
            public float max() {
                return 200;
            }

            @Override
            protected float value(Snapshot snapshot) {
                return (float) snapshot.pulse;
            }
        });

        root.setTag(this);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        handler = new Handler();
        valueUpdate.run();
    }

    @Override
    public void onPause() {
        super.onPause();

        handler = null;
    }

    private Runnable valueUpdate = new Runnable() {
        public void run() {
            if (handler == null) {
                return;
            }

            speedView.invalidate();
            strokeRateView.invalidate();
            pulseView.invalidate();

            handler.postDelayed(this, 500);
        }
    };

    private abstract class AbstractData implements ChartView.Data {

        @Override
        public int length() {
            // TODO where to getPrograms snapshots from
            return 1;
        }

        @Override
        public float value(int index) {
            // TODO where to getPrograms snapshots from
            Snapshot snapshot = new Snapshot();

            return value(snapshot);
        }

        protected abstract float value(Snapshot snapshot);
    }
}