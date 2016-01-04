package com.enthusiast94.edinfit.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.models.Departure;

/**
 * Created by manas on 03-01-2016.
 */
public class LiveDepartureView extends LinearLayout {

    private TextView serviceNameTextView;
    private TextView destinationTextView;
    private TextView timeTextView;

    public LiveDepartureView(Context context) {
        super(context);
    }

    public LiveDepartureView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_live_departure, this, true);

        serviceNameTextView = (TextView) view.findViewById(R.id.service_name_textview);
        destinationTextView = (TextView) view.findViewById(R.id.destination_textview);
        timeTextView = (TextView) view.findViewById(R.id.time_textview);
    }

    public void bindItem(Departure departure) {
        serviceNameTextView.setText(departure.getServiceName());
        destinationTextView.setText(departure.getDestination());
        timeTextView.setText(Helpers.humanizeLiveDepartureTime(departure.getTime()));
    }
}
