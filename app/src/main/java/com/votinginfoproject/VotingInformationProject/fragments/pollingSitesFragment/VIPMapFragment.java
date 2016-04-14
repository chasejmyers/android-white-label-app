package com.votinginfoproject.VotingInformationProject.fragments.pollingSitesFragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.votinginfoproject.VotingInformationProject.R;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationActivity;
import com.votinginfoproject.VotingInformationProject.activities.voterInformationActivity.VoterInformationView;
import com.votinginfoproject.VotingInformationProject.fragments.bottomNavigationFragment.BottomNavigationFragment;
import com.votinginfoproject.VotingInformationProject.models.CivicApiAddress;
import com.votinginfoproject.VotingInformationProject.models.ElectionAdministrationBody;
import com.votinginfoproject.VotingInformationProject.models.FilterLabels;
import com.votinginfoproject.VotingInformationProject.models.PollingLocation;
import com.votinginfoproject.VotingInformationProject.models.VoterInfo;
import com.votinginfoproject.VotingInformationProject.models.singletons.UserPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VIPMapFragment extends MapFragment implements Toolbar.OnMenuItemClickListener, PollingSitesView, BottomNavigationFragment, GoogleMap.OnMarkerClickListener, OnMapReadyCallback {
    private static final String LOCATION_ID = "location_id";
    private static final String POLYLINE = "polyline";
    private static final String HOME = "home";
    private static final String ARG_CURRENT_SORT = "current_sort";
    private static final String CURRENT_LOCATION = "current";
    private final String TAG = VIPMapFragment.class.getSimpleName();
    VoterInfo voterInfo;
    View mapView;
    RelativeLayout rootView;
    ArrayList<PollingLocation> allLocations;
    GoogleMap map;
    String locationId;
    PollingLocation selectedLocation;
    LatLng thisLocation;
    LatLng homeLocation;
    LatLng currentLocation;
    String homeAddress;
    String currentAddress;
    String encodedPolyline;
    LatLngBounds polylineBounds;
    boolean haveElectionAdminBody;
    HashMap<String, MarkerOptions> markers;
    // track the internally-assigned ID for each marker and map it to the location's key
    HashMap<String, String> markerIds;
    // track which location filter was last selected, and only refresh list if it changed

    boolean showPolling = true;
    boolean showEarly = true;
    boolean showDropBox = true;

    private PollingSitesListFragment.PollingSiteOnClickListener mListener;
    private PollingSitesPresenter mPresenter;

    private Toolbar mToolbar;

    public VIPMapFragment() {
        super();
    }

    /**
     * Default newInstance Method.
     *
     * @param context
     * @param tag
     * @param polyline
     * @return
     */
    public static VIPMapFragment newInstance(Context context, String tag, String polyline) {
        // instantiate with map options
        GoogleMapOptions options = new GoogleMapOptions();
        VIPMapFragment fragment = VIPMapFragment.newInstance(context, options);

        Bundle args = new Bundle();
        args.putString(LOCATION_ID, tag);
        args.putString(POLYLINE, polyline);
        fragment.setArguments(args);

        return fragment;
    }

    public static VIPMapFragment newInstance(Context context, String tag, @LayoutRes int currentSort) {
        // instantiate with map options
        GoogleMapOptions options = new GoogleMapOptions();
        VIPMapFragment fragment = VIPMapFragment.newInstance(context, options);

        Bundle args = new Bundle();
        args.putString(LOCATION_ID, tag);
        args.putInt(ARG_CURRENT_SORT, currentSort);

        fragment.setArguments(args);

        return fragment;
    }

    public static VIPMapFragment newInstance(Context context, GoogleMapOptions options) {
        Bundle args = new Bundle();
        // need to send API key to initialize map
        args.putParcelable(context.getString(R.string.google_api_android_key), options);

        VIPMapFragment fragment = new VIPMapFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof PollingSitesListFragment.PollingSiteOnClickListener) {
            mListener = (PollingSitesListFragment.PollingSiteOnClickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // programmatically add map view, so filter drop-down appears on top
        mapView = super.onCreateView(inflater, container, savedInstanceState);
        rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_map, container, false);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar);
        rootView.addView(mapView, layoutParams);

        @LayoutRes int selectedSort = R.id.sort_all;
        if (getArguments() != null) {
            selectedSort = getArguments().getInt(ARG_CURRENT_SORT);
        }

        mPresenter = new PollingSitesPresenterImpl(this, selectedSort);

        voterInfo = UserPreferences.getVoterInfo();
        allLocations = voterInfo.getAllLocations();

        homeLocation = UserPreferences.getHomeLatLong();
//        currentLocation = mActivity.getUserLocation();
//        currentAddress = mActivity.getUserLocationAddress();
        homeAddress = voterInfo.normalizedInput.toGeocodeString();

//        polylineBounds = mActivity.getPolylineBounds();

        // check if this map view is for an election administration body
        if (locationId.equals(ElectionAdministrationBody.AdminBody.STATE) ||
                locationId.equals(ElectionAdministrationBody.AdminBody.LOCAL)) {
            haveElectionAdminBody = true;
        } else {
            haveElectionAdminBody = false;
        }

        // set selected location to zoom to
        if (locationId.equals(HOME)) {
            thisLocation = homeLocation;
        } else if (haveElectionAdminBody) {
            thisLocation = voterInfo.getAdminBodyLatLng(locationId);
        } else {
            Log.d(TAG, "Have location ID: " + locationId);

            selectedLocation = voterInfo.getLocationForId(locationId);

            CivicApiAddress address = selectedLocation.address;
            thisLocation = new LatLng(address.latitude, address.longitude);
        }

        // check if already instantiated
        if (map == null) {
            getMapAsync(this);
        } else {
            map.clear();
            setupMapView(map);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view != null) {
            mToolbar = (Toolbar) view.findViewById(R.id.toolbar);

            if (mToolbar == null) {
                Log.e(TAG, "No toolbar found in class: " + getClass().getSimpleName());
            } else {
                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                mToolbar.setTitle(R.string.bottom_navigation_title_polls);
                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getActivity() instanceof VoterInformationActivity) {
                            ((VoterInformationView) getActivity()).navigateBack();
                        }
                    }
                });

                mToolbar.setOnMenuItemClickListener(this);
                mToolbar.inflateMenu(R.menu.polling_sites_map);
                mToolbar.getMenu().findItem(mPresenter.getCurrentSort()).setChecked(true);

                //Remove any sorts that are missing locations
                if (!mPresenter.hasPollingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_polling_locations);
                }

                if (!mPresenter.hasEarlyVotingLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_early_vote);
                }

                if (!mPresenter.hasDropBoxLocations()) {
                    mToolbar.getMenu().removeItem(R.id.sort_drop_boxes);
                }
            }
        }
    }

    /**
     * Helper function to add everything that isn't a polling site to the map
     */
    private void addNonPollingToMap() {
        // add marker for user-entered address
        if (homeLocation != null && markerIds != null) {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(homeLocation)
                    .title(getContext().getString(R.string.locations_map_label_user_address))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_map))
            );

            markerIds.put(marker.getId(), HOME);
        }

        if (currentLocation != null) {
            // add marker for current user location (used for directions)
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title(getContext().getString(R.string.locations_map_label_user_location))
                    .snippet(currentAddress)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation))
            );

            markerIds.put(marker.getId(), CURRENT_LOCATION);
        }

        if (haveElectionAdminBody) {
            // add marker for state or local election administration body
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(thisLocation)
                    .title(getContext().getString(R.string.locations_map_label_election_administration_body))
                    .snippet(voterInfo.getAdminAddress(locationId).toString())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_leg_body_map))
            );

            marker.showInfoWindow();
            // allow for getting directions from election admin body location
            markerIds.put(marker.getId(), locationId);
        }

        if (encodedPolyline != null && !encodedPolyline.isEmpty()) {
            // show directions line on map
            PolylineOptions polylineOptions = new PolylineOptions();
            List<LatLng> pts = PolyUtil.decode(encodedPolyline);
            polylineOptions.addAll(pts);
            polylineOptions.color(getContext().getResources().getColor(R.color.brand));
            map.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            locationId = args.getString(LOCATION_ID);
            encodedPolyline = args.getString(POLYLINE);
        }
    }

    private MarkerOptions createMarkerOptions(PollingLocation location, @DrawableRes int drawable) {
        MarkerOptions options = new MarkerOptions()
                .position(new LatLng(location.address.latitude, location.address.longitude))
                .icon(BitmapDescriptorFactory.fromResource(drawable));

        return options;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mPresenter.menuItemClicked(item.getItemId());
        item.setChecked(true);

        return false;
    }

    @Override
    public void navigateToDirections(PollingLocation pollingLocation) {

    }

    @Override
    public void navigateToErrorForm() {

    }

    @Override
    public void navigateToMap(@LayoutRes int currentSort) {
        //Not implemented
    }

    @Override
    public void navigateToList(@LayoutRes int currentSort) {
        mListener.listButtonClicked(currentSort);
    }

    @Override
    public void updateList(ArrayList<PollingLocation> locations) {

    }

    @Override
    public void resetView() {
        if (polylineBounds != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(polylineBounds, 60));
        } else if (thisLocation != null) {
            // zoom to selected location
            if (thisLocation == homeLocation) {
                // move out further when viewing general map centered on home
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 8));
            } else {
                // move to specific polling location or other point of interest
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 15));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //TODO handle layout here
        //Unselect all markers
        //select current marker

//        marker.setIcon();

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        //TODO enable my location in M
//            map.setMyLocationEnabled(true);

        setupMapView(googleMap);

        mPresenter.getSortedLocations();

        // start asynchronous task to add markers to map
        new AddMarkersTask().execute(locationId);

        //This will be the same as reset View, but not animated
        if (polylineBounds != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(polylineBounds, 60));
        } else if (thisLocation != null) {
            // zoom to selected location
            if (thisLocation == homeLocation) {
                // move out further when viewing general map centered on home
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 8));
            } else {
                // move to specific polling location or other point of interest
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(thisLocation, 15));
            }
        }
    }

    // set click handler for info window (to go to directions list)
    // info window is just a bitmap, so can't listen for clicks on elements within it.
    private void setupMapView(GoogleMap map) {
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setMapToolbarEnabled(false);

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // get location key for this marker's ID
                String key = markerIds.get(marker.getId());

                // do nothing for taps on user address or current location info window
                if (key.equals(HOME) || key.equals((CURRENT_LOCATION))) {
                    return;
                }

                Log.d(TAG, "Clicked marker for " + key);
//                mActivity.showDirections(key);
            }
        });
    }

    private class AddMarkersTask extends AsyncTask<String, Integer, String> {

        /**
         * Helper function to add collection of polling locations to map.
         *
         * @param locations
         * @param drawable
         */
        private void addLocationsToMap(List<PollingLocation> locations, @DrawableRes int drawable) {
            for (PollingLocation location : locations) {
                if (location.address.latitude == 0) {
                    Log.d(TAG, "Skipping adding to map location " + location.name);
                    continue;
                }

                markers.put(location.address.toGeocodeString(), createMarkerOptions(location, drawable));
            }
        }

        @Override
        protected String doInBackground(String... select_locations) {
            markers = new HashMap<>(allLocations.size());
            markerIds = new HashMap<>(allLocations.size());

            // use red markers for early voting sites
            if (showEarly) {
                addLocationsToMap(voterInfo.getOpenEarlyVoteSites(), R.drawable.ic_website_active);
            }

            // use blue markers for polling locations
            if (!voterInfo.getPollingLocations().isEmpty() && showPolling) {
                addLocationsToMap(voterInfo.getPollingLocations(), R.drawable.ic_website_active);
            }

            // use green markers for drop boxes
            if (showDropBox) {
                addLocationsToMap(voterInfo.getOpenDropOffLocations(), R.drawable.ic_website_active);
            }

            return locationId;
        }

        @Override
        protected void onPostExecute(String checkId) {
            for (String key : markers.keySet()) {
                if (map != null) {

                    Marker marker = map.addMarker(markers.get(key));
                    markerIds.put(marker.getId(), key);

                    if (key.equals(locationId)) {
                        // show popup for marker at selected location
                        marker.showInfoWindow();
                    }

                    addNonPollingToMap();
                }
            }
        }
    }
}
