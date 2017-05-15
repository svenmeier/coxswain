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
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public abstract class AbstractValueFragment extends DialogFragment {

    private List<Tab> tabs = new ArrayList<>();

    private ViewPager pager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_tabs, container, false);

        pager = (ViewPager) view.findViewById(R.id.tabs_pager);
        pager.setAdapter(new MyFragmentAdapter());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        for (Tab tab : tabs) {
            if (tab.segmentToIndex(getCallback().getSegment()) >= 0) {
                pager.setCurrentItem(tabs.indexOf(tab));
                break;
            }
        }
    }

    private void changeSegment(int position) {
        Segment segment = getCallback().getSegment();

        Tab tab = tabs.get(pager.getCurrentItem());
        tab.indexToSegment(segment, position);

        getCallback().setSegment(segment);

        AbstractValueFragment.this.dismiss();
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
            return tabs.get(tab).getTitle();
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

                    return new ValueHolder(v, tab.getPattern());
                }

                @Override
                public void onBindViewHolder(ValueHolder holder, int position) {
                    holder.onBind(tab.getValue(position));
                }
            });

            Segment segment = getCallback().getSegment();
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

        private final ValueView valueView;

        public ValueHolder(View view, String pattern) {
            super(view);

            valueView = (ValueView) view.findViewById(R.id.values_value);
            valueView.setPattern(pattern);

            valueView.setOnClickListener(this);
        }

        public void onBind(int value) {
            valueView.setValue(value);
        }

        @Override
        public void onClick(View v) {
            changeSegment(getAdapterPosition());
        }
    }

    private Callback getCallback() {
        return Utils.getParent(this, Callback.class);
    }

    public interface Tab {
        CharSequence getTitle();

        int getCount();

        String getPattern();

        int getValue(int index);

        void indexToSegment(Segment segment, int index);

        int segmentToIndex(Segment segment);
    }

    public interface Callback {
        public Segment getSegment();

        public void setSegment(Segment segment);
    }
}