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

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Reference;
import propoid.util.content.Preference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public abstract class AbstractValueFragment extends DialogFragment {

    private Gym gym;

    private Segment segment;

    private List<Tab> tabs = new ArrayList<>();

    private ViewPager pager;

    private boolean arabic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_tabs, container, false);

        pager = (ViewPager) view.findViewById(R.id.tabs_pager);
        pager.setAdapter(new MyFragmentAdapter());

        arabic = Preference.getBoolean(getContext(), R.string.preference_numbers_arabic).get();

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gym = Gym.instance(getContext());

        segment = gym.get(Reference.<Segment>from(getArguments()));
    }

    @Override
    public void onResume() {
        super.onResume();

        for (Tab tab : tabs) {
            if (tab.segmentToIndex(segment) >= 0) {
                pager.setCurrentItem(tabs.indexOf(tab));
                break;
            }
        }
    }

    private void changeSegment(int position) {
        Tab tab = tabs.get(pager.getCurrentItem());
        tab.indexToSegment(segment, position);

        gym.mergeSegment(segment);

        AbstractValueFragment.this.dismiss();

        Utils.getCallback(this, Callback.class).onChanged(segment);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(STYLE_NO_TITLE);

        return dialog;
    }

    protected void addTab(Tab tab) {
        this.tabs.add(tab);
    }

    private class MyFragmentAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public CharSequence getPageTitle(int tab) {
            return getString(tabs.get(tab).getBinding().label);
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int tabIndex) {

            final Tab tab = tabs.get(tabIndex);

            ViewGroup root = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.layout_values, container, false);
            container.addView(root);

            RecyclerView valuesView = (RecyclerView) root.findViewById(R.id.values);
            valuesView.setTag(tab);
            valuesView.setLayoutManager(new LinearLayoutManager(getContext()));
            valuesView.setHasFixedSize(true);
            valuesView.addOnScrollListener(new GridScroll());
            valuesView.setAdapter(new RecyclerView.Adapter<ValueHolder>() {

                @Override
                public int getItemCount() {
                    return tab.getCount();
                }

                @Override
                public long getItemId(int index) {
                    return index;
                }

                @Override
                public ValueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View v = (View) LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_values_item, parent, false);

                    return new ValueHolder(v);
                }

                @Override
                public void onBindViewHolder(ValueHolder holder, int position) {
                    holder.onBind(tab.getBinding(), tab.getValue(position));
                }
            });

            int index = tab.segmentToIndex(segment);
            if (index >= 0) {
                valuesView.scrollToPosition(index);
            }

            return root;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }

    private class ValueHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView valueView;

        public ValueHolder(View view) {
            super(view);

            valueView = (TextView) view.findViewById(R.id.values_value);

            valueView.setOnClickListener(this);
        }

        public void onBind(ValueBinding binding, int value) {
            valueView.setText(binding.format(getContext(), value, arabic));
        }

        @Override
        public void onClick(View v) {
            changeSegment(getAdapterPosition());
        }
    }

    public interface Tab {

        ValueBinding getBinding();

        int getCount();

        int getValue(int index);

        void indexToSegment(Segment segment, int index);

        int segmentToIndex(Segment segment);
    }

    public interface Callback {

        void onChanged(Segment segment);
    }

    public static AbstractValueFragment createTarget(Segment segment) {

        TargetDialogFragment fragment = new TargetDialogFragment();

        Bundle args = new Bundle();
        new Reference<Segment>(segment).to(args);
        fragment.setArguments(args);

        return fragment;
    }

    public static AbstractValueFragment createLimit(Segment segment) {
        LimitDialogFragment fragment = new LimitDialogFragment();

        Bundle args = new Bundle();
        new Reference<Segment>(segment).to(args);
        fragment.setArguments(args);

        return fragment;
    }
}