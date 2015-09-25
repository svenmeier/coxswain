package svenmeier.coxswain;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Set;

import propoid.db.Reference;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.view.ProgramsFragment;
import svenmeier.coxswain.view.SettingsFragment;
import svenmeier.coxswain.view.WorkoutsFragment;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);

        return intent;
    }
}
