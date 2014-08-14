package com.votinginfoproject.VotingInformationProject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.fragments.HomeFragment;
import com.votinginfoproject.VotingInformationProject.models.VIPApp;
import com.votinginfoproject.VotingInformationProject.models.VIPAppContext;
import com.votinginfoproject.VotingInformationProject.models.VoterInfo;

public class HomeActivity extends FragmentActivity implements HomeFragment.OnInteractionListener {
    VIPApp app;

    public String getSelectedParty() {
        return selectedParty;
    }

    public void setSelectedParty(String selectedParty) {
        this.selectedParty = selectedParty;
        app.setSelectedParty(selectedParty);
    }

    String selectedParty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        app = new VIPAppContext((VIPApp) getApplicationContext()).getVIPApp();
        selectedParty = "";
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onGoButtonPressed(View view) {
        Intent intent = new Intent(this, VIPTabBarActivity.class);
        startActivity(intent);
    }

    public void searchedAddress(VoterInfo voterInfo) {
        // set VoterInfo object on app singleton
        app.setVoterInfo(voterInfo, selectedParty);
    }
}
