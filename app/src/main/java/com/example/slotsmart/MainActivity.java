package com.example.slotsmart;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.slotsmart.ui.AllocationsFragment;
import com.example.slotsmart.ui.CoursesFragment;
import com.example.slotsmart.ui.DepartmentsFragment;
import com.example.slotsmart.ui.EnrollmentsFragment;
import com.example.slotsmart.ui.FacultyFragment;
import com.example.slotsmart.ui.ProgramsFragment;
import com.example.slotsmart.ui.RoomsFragment;
import com.example.slotsmart.ui.SlotsFragment;
import com.example.slotsmart.ui.StudentsFragment;
import com.example.slotsmart.ui.TimetableFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private SessionManager session;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        // Enable home button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

        // Force toolbar to handle clicks properly
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new TimetableFragment(), getString(R.string.nav_timetable));
            navigationView.setCheckedItem(R.id.nav_timetable);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        String title = "";
        int id = item.getItemId();

        if (id == R.id.nav_timetable) {
            fragment = new TimetableFragment();
            title = getString(R.string.nav_timetable);
        } else if (id == R.id.nav_students) {
            fragment = new StudentsFragment();
            title = getString(R.string.nav_students);
        } else if (id == R.id.nav_faculty) {
            fragment = new FacultyFragment();
            title = getString(R.string.nav_faculty);
        } else if (id == R.id.nav_rooms) {
            fragment = new RoomsFragment();
            title = getString(R.string.nav_rooms);
        } else if (id == R.id.nav_slots) {
            fragment = new SlotsFragment();
            title = getString(R.string.nav_slots);
        } else if (id == R.id.nav_allocations) {
            fragment = new AllocationsFragment();
            title = getString(R.string.nav_allocations);
        } else if (id == R.id.nav_enrollments) {
            fragment = new EnrollmentsFragment();
            title = getString(R.string.nav_enrollments);
        } else if (id == R.id.nav_departments) {
            fragment = new DepartmentsFragment();
            title = getString(R.string.nav_departments);
        } else if (id == R.id.nav_programs) {
            fragment = new ProgramsFragment();
            title = getString(R.string.nav_programs);
        } else if (id == R.id.nav_courses) {
            fragment = new CoursesFragment();
            title = getString(R.string.nav_courses);
        } else if (id == R.id.nav_logout) {
            session.logout(this);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (fragment != null) {
            loadFragment(fragment, title);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Let the toggle handle the home button
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred
        toggle.syncState();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
