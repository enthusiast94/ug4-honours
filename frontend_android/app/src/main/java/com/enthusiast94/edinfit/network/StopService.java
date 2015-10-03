package com.enthusiast94.edinfit.network;

import android.util.Log;

import com.enthusiast94.edinfit.App;
import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Stop;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by manas on 01-10-2015.
 */
public class StopService extends BaseService {

    public static final String TAG = StopService.class.getSimpleName();

    public static void getNearbyStops(Double latitude, Double longitude, int maxDistance,
                                      Double nearDistance, boolean onlyIncludeUpcomingDepartures,
                                      int limit, final Callback<List<Stop>> callback) {

        RequestParams requestParams = new RequestParams();
        requestParams.put("latitude", latitude);
        requestParams.put("longitude", longitude);
        requestParams.put("limit", limit);
        requestParams.put("only_include_upcoming_departures", onlyIncludeUpcomingDepartures);
        requestParams.put("max_distance", maxDistance);
        requestParams.put("near_distance", nearDistance);

        AsyncHttpClient client = getAsyncHttpClient(true);
        client.get(API_BASE + "/stops/nearby", requestParams, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Gson gson = new Gson();
                    Stop[] stopsArray = gson.fromJson(
                            response.getJSONArray("data").toString(), Stop[].class
                    );
                    List<Stop> stopsList = Arrays.asList(stopsArray);

                    if (callback != null) callback.onSuccess(stopsList);

                } catch (JSONException e) {
                    if (callback != null)
                        callback.onFailure(App.getAppContext().getString(R.string.error_parsing));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                onFailureCommon(statusCode, errorResponse, callback);
            }
        });

    }
}
