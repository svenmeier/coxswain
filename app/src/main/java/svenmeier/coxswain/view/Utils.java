package svenmeier.coxswain.view;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.Map;

/**
 * Created by sven on 19.08.15.
 */
public class Utils {

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        return (int)((dp * displayMetrics.density) + 0.5);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParent(Fragment fragment, Class<T> t) {
        Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment != null && t.isInstance(parentFragment)) {
            return (T) parentFragment;
        }

        FragmentActivity activity = fragment.getActivity();
        if (activity != null && t.isInstance(activity)) {
            return (T) activity;
        }

        throw new IllegalStateException("fragment does not have requested parent " + t.getSimpleName());
    }
}
