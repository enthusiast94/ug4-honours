package com.enthusiast94.edinfit.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.activities.StopActivity;
import com.enthusiast94.edinfit.events.OnStopSelectedEvent;
import com.enthusiast94.edinfit.models.Stop;
import com.enthusiast94.edinfit.services.BaseService;
import com.enthusiast94.edinfit.services.LocationProviderService;
import com.enthusiast94.edinfit.services.StopService;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by manas on 18-10-2015.
 */
public class SelectStopFragment extends Fragment implements LocationProviderService.LocationCallback {

    public static final String TAG = SelectStopFragment.class.getSimpleName();
    private static final String MAPVIEW_SAVE_STATE = "mapViewSaveState";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView stopsRecyclerView;
    private StopsAdapter stopsAdapter;
    private MapView mapView;
    private GoogleMap map;
    private TextView selectedStopTextView;
    private int currentlySelectedStopIndex = -1;
    private List<Stop> stops = new ArrayList<>();

    // nearby stops api endpoint params
    private static final int NEARBY_STOPS_LIMIT = 5;
    private static final int MAX_DISTANCE = 3;
    private static final double NEAR_DISTANCE = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_stop, container, false);

        /**
         * Setup app bar
         */

        setHasOptionsMenu(true);

        ActionBar appBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (appBar != null) {
            appBar.setTitle(getString(R.string.action_select_stop));
        }

        /**
         * Find views
         */

        stopsRecyclerView = (RecyclerView) view.findViewById(R.id.stops_recyclerview);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mapView = (MapView) view.findViewById(R.id.map_view);
        selectedStopTextView = (TextView) view.findViewById(R.id.selected_stop_textview);

        /**
         * Create MapView, get GoogleMap from it and then configure the GoogleMap
         */

        Bundle mapViewSavedInstanceState = savedInstanceState != null ?
                savedInstanceState.getBundle(MAPVIEW_SAVE_STATE) : null;
        mapView.onCreate(mapViewSavedInstanceState);

        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(Helpers.getEdinburghLatLng(getActivity()), 12));

        /**
         * Setup swipe refresh layout
         */

        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                LocationProviderService.getInstance().requestLastKnownLocationInfo(SelectStopFragment.this);
            }
        });

        /**
         * Setup stops recycler view
         */

        stopsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        stopsAdapter = new StopsAdapter();
        stopsRecyclerView.setAdapter(stopsAdapter);


        /**
         * Finally, request user location in order to get things started
         */

        LocationProviderService.getInstance().requestLastKnownLocationInfo(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mapView.onResume();

        // update app bar title
        ActionBar appBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (appBar != null) {
            appBar.setTitle(getString(R.string.action_select_stop));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //This MUST be done before saving any of your own or your base class's variables
        Bundle mapViewSaveState = new Bundle(outState);
        mapView.onSaveInstanceState(mapViewSaveState);
        outState.putBundle(MAPVIEW_SAVE_STATE, mapViewSaveState);
        //Add any other variables here.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        mapView.onLowMemory();
    }

    private void setRefreshIndicatorVisiblity(final boolean visiblity) {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(visiblity);
            }
        });
    }

    private void loadStops(final LatLng userLocationLatLng) {
        setRefreshIndicatorVisiblity(true);

        StopService.getInstance().getNearbyStops(userLocationLatLng.latitude,
                userLocationLatLng.longitude, MAX_DISTANCE, NEAR_DISTANCE, null,
                NEARBY_STOPS_LIMIT, new BaseService.Callback<List<Stop>>() {

                    @Override
                    public void onSuccess(List<Stop> data) {
                        stops = data;

                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            stopsAdapter.notifyStopsChanged();

                            updateSlidingMapPanel();
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        if (getActivity() != null) {
                            setRefreshIndicatorVisiblity(false);

                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateSlidingMapPanel() {
        // remove all existing markers
        map.clear();

        if (stops.size() > 0) {
            // update selected stop textview
            selectedStopTextView.setText(stops.get(currentlySelectedStopIndex).getName());

            // add stop markers to map
            for (int i=0; i< stops.size(); i++) {
                Stop stop = stops.get(i);

                Bitmap stopMarkerIcon = Helpers.getStopMarkerIcon(getActivity());

                LatLng stopLatLng = new LatLng(stop.getLocation().get(1), stop.getLocation().get(0));

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(stopLatLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(stopMarkerIcon))
                        .title(stop.getName());

                Marker stopMarker = map.addMarker(markerOptions);

                // move map focus to selected stop and show its info window
                if (i == currentlySelectedStopIndex) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(stopLatLng, 16));
                    stopMarker.showInfoWindow();
                }
            }
        }
    }

    @Override
    public void onLocationSuccess(LatLng latLng, String placeName) {
        if (getActivity() != null) {
            loadStops(latLng);
        }
    }

    @Override
    public void onLocationFailure(String error) {
        if (getActivity() != null) {
            setRefreshIndicatorVisiblity(false);
            Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_select_stop, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                EventBus.getDefault()
                        .post(new OnStopSelectedEvent(stops.get(currentlySelectedStopIndex)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class StopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private LayoutInflater inflater;
        private int previouslySelectedStopIndex;
        private static final int HINT_VIEW_TYPE = 0;
        private static final int HEADING_VIEW_TYPE = 1;
        private static final int STOP_VIEW_TYPE = 2;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (inflater == null) {
                inflater = LayoutInflater.from(getActivity());
            }

            if (viewType == HINT_VIEW_TYPE) {
                return new HintViewHolder(inflater.inflate(R.layout.row_hint_stop_selection,
                        parent, false));
            } else if (viewType == HEADING_VIEW_TYPE) {
                return new HeadingViewHolder(inflater.inflate(R.layout.row_heading, parent, false));
            } else {
                return new SelectionStopViewHolder(inflater.inflate(R.layout.row_selection_stop,
                        parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Object item = getItem(position);

            if (item instanceof String) {
                ((HeadingViewHolder) holder).bindItem((String) getItem(position));
            } else if (item instanceof Stop) {
                ((SelectionStopViewHolder) holder).bindItem((Stop) getItem(position));
            }
        }

        @Override
        public int getItemCount() {
            if (stops.size() == 0) {
                return 0;
            } else {
                return stops.size() + 2 /* 1 hint + 1 heading */;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HINT_VIEW_TYPE;
            } else if (position == 1) {
                return HEADING_VIEW_TYPE;
            } else {
                return STOP_VIEW_TYPE;
            }
        }

        private Object getItem(int position) {
            int viewType = getItemViewType(position);

            if (viewType == HINT_VIEW_TYPE) {
                return null;
            } else if (viewType == HEADING_VIEW_TYPE) {
                return getString(R.string.label_where_are_you_waiting);
            } else {
                return stops.get(position - 2);
            }
        }

        public void notifyStopsChanged() {
            currentlySelectedStopIndex = 0;
            previouslySelectedStopIndex = 0;

            notifyDataSetChanged();
        }

        private class SelectionStopViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener, View.OnLongClickListener {

            private TextView stopNameTextView;
            private TextView distanceAwayTextView;
            private Stop stop;

            public SelectionStopViewHolder(View itemView) {
                super(itemView);

                // find views
                stopNameTextView = (TextView) itemView.findViewById(R.id.stop_name_textview);
                distanceAwayTextView = (TextView) itemView.findViewById(R.id.distance_away_textview);

                // bind event listeners
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            public void bindItem(Stop stop) {
                this.stop = stop;

                stopNameTextView.setText(stop.getName());
                distanceAwayTextView.setText(Helpers.humanizeDistance(stop.getDistanceAway()));

                if (getAdapterPosition() - 2 == currentlySelectedStopIndex) {
                    itemView.setBackgroundResource(R.color.green_selection);
                } else {
                    itemView.setBackgroundResource(android.R.color.transparent);
                }
            }

            @Override
            public void onClick(View v) {
                currentlySelectedStopIndex = getAdapterPosition() - 2;

                notifyItemChanged(currentlySelectedStopIndex + 2);
                notifyItemChanged(previouslySelectedStopIndex + 2);

                updateSlidingMapPanel();

                previouslySelectedStopIndex = currentlySelectedStopIndex;
            }

            @Override
            public boolean onLongClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                Intent startActivityIntent = new Intent(getActivity(), StopActivity.class);
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_ID, stop.getId());
                startActivityIntent.putExtra(StopActivity.EXTRA_STOP_NAME, stop.getName());
                startActivity(startActivityIntent);

                return true;
            }
        }

        private class HeadingViewHolder extends RecyclerView.ViewHolder {

            private TextView headingTextView;

            public HeadingViewHolder(View itemView) {
                super(itemView);

                headingTextView = (TextView) itemView.findViewById(R.id.heading_textview);
            }

            public void bindItem(String heading) {
                headingTextView.setText(heading);
            }
        }

        private class HintViewHolder extends RecyclerView.ViewHolder {

            public HintViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}