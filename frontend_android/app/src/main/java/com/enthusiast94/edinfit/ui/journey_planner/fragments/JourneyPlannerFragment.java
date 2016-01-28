package com.enthusiast94.edinfit.ui.journey_planner.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.enthusiast94.edinfit.R;
import com.enthusiast94.edinfit.ui.journey_planner.enums.TimeMode;
import com.enthusiast94.edinfit.utils.Helpers;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by manas on 27-01-2016.
 */
public class JourneyPlannerFragment extends Fragment {

    public static final String TAG = JourneyPlannerFragment.class.getSimpleName();
    private static final int ORIGIN_PICKER_REQUEST = 1;
    private static final int DESTINATION_PICKER_REQUEST = 2;

    private TextView originTextView;
    private TextView destinationTextView;
    private TextView dateAndTimeTextView;
    private TextView optionsTextView;
    private ImageButton swapButton;
    private Place originPlace;      // currently selected origin
    private Place destinationPlace; // currently selected destination
    private String time;            // currently selected journey time
    private TimeMode timeMode;      // currently selected journey time mode
    private String date;            // currently selected journey date
    private SimpleDateFormat sdfDay;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey_planner, container, false);
        findViews(view);

        // bind event listeners
        View.OnClickListener clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int id = v.getId();

                if (id == originTextView.getId()) {
                    showPlacePicker(ORIGIN_PICKER_REQUEST);

                } else if (id == destinationTextView.getId()) {
                    showPlacePicker(DESTINATION_PICKER_REQUEST);

                } else if (id == swapButton.getId()) {
                    // swap places and update UI
                    Place temp;
                    temp = originPlace;
                    originPlace = destinationPlace;
                    destinationPlace = temp;
                    updatePlacesUi();

                } else if (id == dateAndTimeTextView.getId()) {
                    showDateAndTimePickerDialog();
                }
            }
        };
        originTextView.setOnClickListener(clickListener);
        destinationTextView.setOnClickListener(clickListener);
        dateAndTimeTextView.setOnClickListener(clickListener);
        optionsTextView.setOnClickListener(clickListener);
        swapButton.setOnClickListener(clickListener);

        // set default journey time and date
        time = Helpers.getCurrentTime24h();
        timeMode = TimeMode.LEAVE_AFTER;
        sdfDay = new SimpleDateFormat("dd MMM EEEE", Locale.UK);
        date = sdfDay.format(new Date());
        updateDateAndTimeUi();

        return view;
    }

    private void findViews(View view) {
        originTextView = (TextView) view.findViewById(R.id.origin_textview);
        destinationTextView = (TextView) view.findViewById(R.id.destination_textview);
        dateAndTimeTextView = (TextView) view.findViewById(R.id.date_and_time_textview);
        optionsTextView = (TextView) view.findViewById(R.id.options_textview);
        swapButton = (ImageButton) view.findViewById(R.id.swap_button);
    }

    private void showPlacePicker(int requestCode) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(getActivity()), requestCode);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ORIGIN_PICKER_REQUEST) {
                originPlace = PlacePicker.getPlace(data, getActivity());
                updatePlacesUi();
            } else if (requestCode == DESTINATION_PICKER_REQUEST) {
                destinationPlace = PlacePicker.getPlace(data, getActivity());
                updatePlacesUi();
            } else {
                Log.e(TAG, "Invalid request code: " + requestCode);
            }
        }
    }

    private void updatePlacesUi() {
        originTextView.setText(originPlace == null ? getString(R.string.choose_origin) :
                originPlace.getName());
        destinationTextView.setText(destinationPlace == null ? getString(R.string.choose_destination) :
                destinationPlace.getName());
    }

    private void updateDateAndTimeUi() {
        String timeModeText;
        switch (timeMode) {
            case ARRIVE_BY:
                timeModeText = getString(R.string.arrive_by);
                break;
            case LEAVE_AFTER:
                timeModeText = getString(R.string.leave_after);
                break;
            default:
                throw new IllegalStateException("Invalid time mode: " + timeMode.toString());
        }

        dateAndTimeTextView.setText(String.format(getString(R.string.journey_date_and_time_format),
                timeModeText, date, time));
    }

    private void showDateAndTimePickerDialog() {
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_journey_time_picker, null);

        // find views
        final RadioButton arriveByRadio = (RadioButton) dialogView.findViewById(R.id.arrive_by_radio);
        final RadioButton leaveAfterRadio = (RadioButton) dialogView.findViewById(R.id.leave_after_radio);
        final NumberPicker hourPicker = (NumberPicker) dialogView.findViewById(R.id.hour_picker);
        final NumberPicker minutePicker = (NumberPicker) dialogView.findViewById(R.id.minute_picker);
        final Spinner dateSpinner = (Spinner) dialogView.findViewById(R.id.date_spinner);

        // set initial values for radio buttons
        switch (timeMode) {
            case ARRIVE_BY:
                arriveByRadio.setChecked(true);
                break;
            case LEAVE_AFTER:
                leaveAfterRadio.setChecked(true);
                break;
            default:
                throw new IllegalStateException("Invalid time mode: " + timeMode.toString());
        }

        // setup time pickers
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(Integer.parseInt(time.substring(0,
                time.indexOf(":"))));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(Integer.parseInt(time.substring(
                time.indexOf(":") + 1, time.length())));

        // collect dates for date spinner
        ArrayList<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        dates.add(sdfDay.format(today));
        calendar.setTime(today);
        for (int i=0; i<5; i++) {
            calendar.add(Calendar.DATE, 1);
            dates.add(sdfDay.format(calendar.getTime()));
        }

        // setup date spinner
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, dates);
        dateSpinner.setAdapter(spinnerAdapter);
        for (int i=0; i<spinnerAdapter.getCount(); i++) {
            if (spinnerAdapter.getItem(i).equals(date)) {
                dateSpinner.setSelection(i);
                break;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.select_date_and_time))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.label_set), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        time = hourPicker.getValue() + ":" + minutePicker.getValue();
                        timeMode = leaveAfterRadio.isChecked() ? TimeMode.LEAVE_AFTER :
                                TimeMode.ARRIVE_BY;
                        date = dateSpinner.getSelectedItem().toString();

                        updateDateAndTimeUi();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.label_cancel), null)
                .create();

        dialog.show();
    }
}
