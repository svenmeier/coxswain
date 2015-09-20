package svenmeier.coxswain.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Segment;

/**
 */
public abstract class AbstractValueFragment extends DialogFragment {

    private List<Tab> tabs = new ArrayList<>();

    private ViewPager pager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_tabs, container, false);

        pager = (ViewPager) view.findViewById(R.id.tabs_pager);
        pager.setAdapter(new MyFragmentAdapter());

        View titlesView = view.findViewById(R.id.tabs_titles);
        titlesView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSegment();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        for (Tab tab : tabs) {
            if (tab.segmentToIndex(getHolder().getSegment()) >= 0) {
                pager.setCurrentItem(tabs.indexOf(tab));
                break;
            }
        }
    }

    private void changeSegment() {
        Segment segment = getHolder().getSegment();

        Tab tab = tabs.get(pager.getCurrentItem());
        ListView listView = (ListView) pager.findViewWithTag(tab);
        tab.indexToSegment(segment, listView.getFirstVisiblePosition());

        getHolder().setSegment(segment);

        AbstractValueFragment.this.dismiss();
    }

    @NonNull
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

            ListView listView = (ListView) root.findViewById(R.id.values);
            listView.setTag(tab);
            listView.setOnScrollListener(new GridScroll());
            listView.setAdapter(new BaseAdapter() {

                @Override
                public int getCount() {
                    return tab.getCount();
                }

                @Override
                public Integer getItem(int index) {
                    return tab.getValue(index);
                }

                @Override
                public long getItemId(int index) {
                    return index;
                }

                @Override
                public View getView(int index, View view, ViewGroup parent) {
                    if (view == null) {
                        view = getActivity().getLayoutInflater().inflate(R.layout.layout_values_item, parent, false);
                    }

                    ValueView valueView = (ValueView) view.findViewById(R.id.values_value);
                    valueView.setPattern(tab.getPattern());
                    valueView.setValue(tab.getValue(index));
                    return view;
                }
            });
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    changeSegment();
                }
            });

            Segment segment = getHolder().getSegment();
            int index = tab.segmentToIndex(segment);
            if (index >= 0) {
                listView.setSelection(index);
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

    private SegmentHolder getHolder() {
        return Utils.getParent(this, SegmentHolder.class);
    }

    public interface Tab {
        CharSequence getTitle();

        int getCount();

        String getPattern();

        int getValue(int index);

        void indexToSegment(Segment segment, int index);

        int segmentToIndex(Segment segment);
    }

    public interface SegmentHolder {
        public Segment getSegment();

        public void setSegment(Segment segment);
    }
}