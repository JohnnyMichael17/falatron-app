package com.falatron;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.falatron.fragments.MenuFragment;
import com.falatron.fragments.RvcFragment;
import com.falatron.fragments.TtsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new RvcFragment();
            case 2:
                return new MenuFragment();
            default:
                return new TtsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}