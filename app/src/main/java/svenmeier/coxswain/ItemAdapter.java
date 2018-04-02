
package svenmeier.coxswain;

import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.List;

import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.view.AbstractValueFragment;
import svenmeier.coxswain.view.BindingView;
import svenmeier.coxswain.view.LevelView;
import svenmeier.coxswain.view.ValueBinding;

class ItemAdapter extends DragItemAdapter<Segment, ItemAdapter.ViewHolder> {

    private final FragmentManager supportFragmentManager;
    private final Program program;
    private final Gym gym;
    private int mLayoutId;
    private int mGrabHandleId;
    private boolean mDragOnLongPress;

    ItemAdapter(List<Segment> list, int layoutId, int grabHandleId, boolean dragOnLongPress, FragmentManager supportFragmentManager,
                Program program, Gym gym) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        this.supportFragmentManager = supportFragmentManager;
        setItemList(list);
        this.program = program;
        this.gym = gym;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        final Segment item = mItemList.get(position);
        holder.itemView.setTag(mItemList.get(position));


        holder.addView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                program.duplicateSegmentAfter(item);

                gym.mergeProgram(program);

                notifyDataSetChanged();
            }
        });

        if (item.duration.get() > 0) {
            holder.targetView.setBinding(ValueBinding.DURATION_SHORT);
            holder.targetView.changed(item.duration.get());
        } else if (item.distance.get() > 0) {
            holder.targetView.setBinding(ValueBinding.DISTANCE);
            holder.targetView.changed(item.distance.get());
        } else if (item.strokes.get() > 0) {
            holder.targetView.setBinding(ValueBinding.STROKES);
            holder.targetView.changed(item.strokes.get());
        } else if (item.energy.get() > 0) {
            holder.targetView.setBinding(ValueBinding.ENERGY);
            holder.targetView.changed(item.energy.get());
        }
        holder.targetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbstractValueFragment.createTarget(item).show(supportFragmentManager, "changed");
                gym.mergeProgram(program);
                notifyDataSetChanged();
            }
        });

        if (item.speed.get() > 0) {
            holder.limitView.setBinding(ValueBinding.SPEED);
            holder.limitView.changed(item.speed.get());
        } else if (item.pulse.get() > 0) {
            holder.limitView.setBinding(ValueBinding.PULSE);
            holder.limitView.changed(item.pulse.get());
        } else if (item.strokeRate.get() > 0) {
            holder.limitView.setBinding(ValueBinding.STROKE_RATE);
            holder.limitView.changed(item.strokeRate.get());
        } else {
            holder.limitView.setBinding(ValueBinding.NONE);
            holder.limitView.changed(0);
        }
        holder.limitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbstractValueFragment.createLimit(item).show(supportFragmentManager, "changed");
                gym.mergeProgram(program);
                notifyDataSetChanged();
            }
        });

        holder.difficultyView.setLevel(item.difficulty.get().ordinal());
        holder.difficultyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.difficulty.set(item.difficulty.get().increase());

                gym.mergeProgram(program);
                notifyDataSetChanged();

            }
        });
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).getId();
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {

        private final BindingView targetView;
        private final BindingView limitView;
        private final LevelView difficultyView;
        private final View addView;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            ListSwipeItem swipeItem = (ListSwipeItem) itemView;
            swipeItem.setSwipeInStyle(ListSwipeItem.SwipeInStyle.SLIDE);
            swipeItem.setSupportedSwipeDirection(ListSwipeItem.SwipeDirection.LEFT);
            targetView = itemView.findViewById(R.id.segments_item_target);
            limitView = itemView.findViewById(R.id.segments_item_limit);
            difficultyView = itemView.findViewById(R.id.segments_difficulty);
            addView = itemView.findViewById(R.id.addButton);
        }
    }
}
