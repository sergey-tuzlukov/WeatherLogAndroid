/*
 * WeatherLog is an app for logging air temperature changes and calculating
 * total time of preset limits exceeding in the nearest past. The project
 * began as a way to help moto-bikers decide if it is safe to drive in the morning
 * after cold night, or for automobile owners to decide if it is time to change tires
 * to/from winter ones. It also may be useful when man thinks about clothes/shoes
 * to dress in this time, or in all other cases when there needs to analyze
 * temperature's behaviour.
 *
 * Copyright © 2015 Sergey Tuzlukov <s.tuzlukov@ya.ru>.
 *
 *
 * This file is part of WeatherLog.
 *
 * WeatherLog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WeatherLog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WeatherLog.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.moscow.tuzlukov.sergey.weatherlog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;


public class MainActivity extends ActionBarActivity {

    private static final String SAVED_CURRENT_TIME = "SAVED_CURRENT_TIME";
    private static final String SAVED_CURRENT_TIME_MINUS_12 = "SAVED_CURRENT_TIME_MINUS_12";
    private static final String SAVED_CURRENT_TIME_MINUS_24 = "SAVED_CURRENT_TIME_MINUS_24";
    private static final String SAVED_TIME_ARRAY = "SAVED_TIME_ARRAY";
    private static final String SAVED_TEMP_ARRAY = "SAVED_TEMP_ARRAY";
    private static final String SAVED_CURRENT_GAINED = "SAVED_CURRENT_GAINED";
    private static final String SAVED_HISTORY_GAINED = "SAVED_HISTORY_GAINED";
    private static final String SAVED_LOADER_VISIBILITY = "SAVED_LOADER_VISIBILITY";
    private static final String SAVED_DIALOG_APPID = "SAVED_DIALOG_APPID";
    private static final String SAVED_DIALOG_VISIBILITY = "SAVED_DIALOG_VISIBILITY";
    private static final String IS_FIRST_LAUNCH = "IS_FIRST_LAUNCH";
    private static final String CACHE_TIMESTAMP = "CACHE_TIMESTAMP";
    private static final String CACHED_MAP_FILE = "cached_map_file.dat";
    private static final long CACHE_EXPIRE_TIME = 15 * 60 * 1000; //15 min.
    private static final int FAKE_REFRESH_DELAY = 300; //should be less, than CACHE_QUICK_REFRESH_TIME
    private static final int REQUEST_SETTINGS = 0;

    private final double temperatureLimit1 = +5.0;
    private final double temperatureLimit2 =  0.0;

    private SharedPreferences preferences;
    private NetworkQuery networkQuery;
    private int cityId;
    private static boolean refreshWasCancelled;
    private SettingsActivity.RegisterDialog registerDialog;

    private long currentTime, currentTimeMinus12h, currentTimeMinus24h;
    private boolean currentIsGained = false, historyIsGained = false;
    private Map<Long, Double> temperatureMap = new TreeMap<>();
    private long cachingTimestamp;

    private TextView tvTemperatureLimit1;
    private TextView tvTemperatureLimit2;
    private TextView tvTime1Limit1;
    private TextView tvTime1Limit2;
    private TextView tvTime2Limit1;
    private TextView tvTime2Limit2;
    private TextView tvDayTemperatureSpan;
    private TextView tvDayAverageTemperature;
    private TextView tvDayMedianTemperature;
    private GraphView graphView;
    private LinearLayout llLoader;
    private PullToRefreshLayout ptrLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        cityId = preferences.getInt(NetworkQuery.Params.ID, NetworkQuery.Defaults.CITY_ID);
        cachingTimestamp = preferences.getLong(CACHE_TIMESTAMP, 0L);

        tvTemperatureLimit1 = (TextView) findViewById(R.id.tvTemperatureLimit1);
        tvTemperatureLimit2 = (TextView) findViewById(R.id.tvTemperatureLimit2);
        tvTime1Limit1 = (TextView) findViewById(R.id.tvTime1Limit1);
        tvTime1Limit2 = (TextView) findViewById(R.id.tvTime1Limit2);
        tvTime2Limit1 = (TextView) findViewById(R.id.tvTime2Limit1);
        tvTime2Limit2 = (TextView) findViewById(R.id.tvTime2Limit2);
        tvDayTemperatureSpan = (TextView) findViewById(R.id.tvDayTemperatureSpan);
        tvDayAverageTemperature = (TextView) findViewById(R.id.tvDayAverageTemperature);
        tvDayMedianTemperature = (TextView) findViewById(R.id.tvDayMedianTemperature);
        graphView = (GraphView) findViewById(R.id.graph);
        llLoader = (LinearLayout) findViewById(R.id.llLoader);
        ptrLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);

        resetValues();

        ActionBarPullToRefresh.from(this).allChildrenArePullable().listener(
                new OnRefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        networkQuery.cancelAllRequests(MainActivity.this);
                        refreshWeatherData();
                    }
                }
        ).setup(ptrLayout);

        String appId = preferences.getString(NetworkQuery.Params.APPID, "");
        networkQuery = NetworkQuery.getInstance(getApplicationContext());
        networkQuery.setAppId(appId);

        registerDialog = new SettingsActivity.RegisterDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String appId = registerDialog.getEtAppId().getText().toString().trim();
                networkQuery.setAppId(appId);
                preferences.edit().putString(NetworkQuery.Params.APPID, appId).apply();
                cachingTimestamp = 0;
                refreshWeatherData();
            }
        });

        if (savedInstanceState == null) {
            refreshWeatherData();
            if (preferences.getBoolean(IS_FIRST_LAUNCH, true)) {
                registerDialog.show();
                preferences.edit().putBoolean(IS_FIRST_LAUNCH, false).apply();
            }
        }
        else {
            currentTime = savedInstanceState.getLong(SAVED_CURRENT_TIME);
            currentTimeMinus12h = savedInstanceState.getLong(SAVED_CURRENT_TIME_MINUS_12);
            currentTimeMinus24h = savedInstanceState.getLong(SAVED_CURRENT_TIME_MINUS_24);
            currentIsGained = savedInstanceState.getBoolean(SAVED_CURRENT_GAINED);
            historyIsGained = savedInstanceState.getBoolean(SAVED_HISTORY_GAINED);
            long[] timeArray = savedInstanceState.getLongArray(SAVED_TIME_ARRAY);
            double[] tempArray = savedInstanceState.getDoubleArray(SAVED_TEMP_ARRAY);
            boolean refreshWasRun = savedInstanceState.getBoolean(SAVED_LOADER_VISIBILITY) || refreshWasCancelled;
            if (refreshWasRun || (timeArray == null || tempArray == null))
                refreshWeatherData();
            else {
                temperatureMap.clear();
                for (int i = 0; i < timeArray.length && i < tempArray.length; i++)
                    temperatureMap.put(timeArray[i], tempArray[i]);
                processValues(true);
            }
            if (savedInstanceState.getBoolean(SAVED_DIALOG_VISIBILITY)) {
                registerDialog.getEtAppId().setText(savedInstanceState.getString(SAVED_DIALOG_APPID));
                registerDialog.show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        List<Long> baseLongList = new ArrayList<>(temperatureMap.keySet());
        List<Double> baseDoubleList = new ArrayList<>(temperatureMap.values());
        long[] timeArray = new long[baseLongList.size()];
        double[] tempArray = new double[baseDoubleList.size()];
        for (int i = 0; i < temperatureMap.size(); i++) {
            timeArray[i] = baseLongList.get(i);
            tempArray[i] = baseDoubleList.get(i);
        }
        outState.putLong(SAVED_CURRENT_TIME, currentTime);
        outState.putLong(SAVED_CURRENT_TIME_MINUS_12, currentTimeMinus12h);
        outState.putLong(SAVED_CURRENT_TIME_MINUS_24, currentTimeMinus24h);
        outState.putLongArray(SAVED_TIME_ARRAY, timeArray);
        outState.putDoubleArray(SAVED_TEMP_ARRAY, tempArray);
        outState.putBoolean(SAVED_CURRENT_GAINED, currentIsGained);
        outState.putBoolean(SAVED_HISTORY_GAINED, historyIsGained);
        outState.putBoolean(SAVED_LOADER_VISIBILITY, llLoader.getVisibility() == View.VISIBLE);
        outState.putString(SAVED_DIALOG_APPID, registerDialog.getEtAppId().getText().toString());
        outState.putBoolean(SAVED_DIALOG_VISIBILITY, registerDialog.isVisible());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (refreshWasCancelled)
            refreshWeatherData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        networkQuery.cancelAllRequests(MainActivity.this);
        if (llLoader.getVisibility() == View.VISIBLE) {
            refreshWasCancelled = true;
            llLoader.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        preferences.edit().putLong(CACHE_TIMESTAMP, cachingTimestamp).apply();
        registerDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), REQUEST_SETTINGS);
            return true;
        }
        else if (id == R.id.action_about) {
            startActivity(new Intent(MainActivity.this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            cityId = preferences.getInt(NetworkQuery.Params.ID, NetworkQuery.Defaults.CITY_ID);
            cachingTimestamp = 0;
            refreshWeatherData();
        }
    }

    private void resetValues() {
        String degreeSign = getString(R.string.celsius_degree_caption);
        String format1 = "%" + (temperatureLimit1 == 0 ? "" : "+") + ".0f%s";
        String format2 = "%" + (temperatureLimit2 == 0 ? "" : "+") + ".0f%s";
        tvTemperatureLimit1.setText(String.format(format1, temperatureLimit1, degreeSign));
        tvTemperatureLimit2.setText(String.format(format2, temperatureLimit2, degreeSign));
        tvDayTemperatureSpan.setText(getString(R.string.day_temperature_span_label).replace("%.1f", "- "));
        tvDayAverageTemperature.setText(getString(R.string.day_average_temperature_label).replace("%.1f", "- "));
        tvDayMedianTemperature.setText(getString(R.string.day_median_temperature_label).replace("%.1f", "- "));
        graphView.removeAllSeries();
    }

    private void refreshWeatherData() {
        refreshWasCancelled = false;
        resetValues();
        temperatureMap.clear();

        Calendar calendar = new GregorianCalendar();
        currentTime = calendar.getTimeInMillis() / 1000;
        calendar.add(Calendar.HOUR_OF_DAY, -12);
        currentTimeMinus12h = calendar.getTimeInMillis() / 1000;
        calendar.add(Calendar.HOUR_OF_DAY, -12);
        currentTimeMinus24h = calendar.getTimeInMillis() / 1000;
        calendar.add(Calendar.HOUR_OF_DAY, -24); //getting redundant data (previous 48 hours) because of some troubles on service
        long startTime = calendar.getTimeInMillis() / 1000;

        llLoader.setVisibility(View.VISIBLE);
        if (!isCacheExpired())
            try {
                ObjectInputStream inputStream = new ObjectInputStream(openFileInput(CACHED_MAP_FILE));
                Object loadedMap = inputStream.readObject();
                inputStream.close();
                temperatureMap.putAll((Map<Long, Double>) loadedMap);
                currentIsGained = historyIsGained = true;
                // Do "fake update": use delay to show progress animation correctly
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        processValues(true);
                    }
                }, FAKE_REFRESH_DELAY);
                return;
            }
            catch (Exception e) {
                temperatureMap.clear();
                e.printStackTrace();
            }
        currentIsGained = historyIsGained = false;
        networkQuery.addRequest(NetworkQuery.CURRENT_URL, new NetworkQuery.Params()
                .addParam(NetworkQuery.Params.ID, cityId),
                responseCurrentListener, errorResponseListener, MainActivity.this);
        networkQuery.addRequest(NetworkQuery.HISTORY_URL, new NetworkQuery.Params()
                .addParam(NetworkQuery.Params.ID, cityId)
                .addParam(NetworkQuery.Params.TYPE, NetworkQuery.Params.TYPE_HOUR)
                .addParam(NetworkQuery.Params.START, startTime)
                .addParam(NetworkQuery.Params.END, currentTime),
                responseHistoryListener, errorResponseListener, MainActivity.this);
    }

    private void processValues(boolean useCached) {
        if (!(currentIsGained && historyIsGained))
            return;
        if (temperatureMap.size() >= 2 && !useCached)
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(openFileOutput(CACHED_MAP_FILE, MODE_PRIVATE));
                outputStream.writeObject(temperatureMap);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        llLoader.setVisibility(View.GONE);
        ptrLayout.setRefreshComplete();

        if (temperatureMap.size() < 2) {
            Toast.makeText(MainActivity.this, getString(R.string.no_data_error_message), Toast.LENGTH_SHORT).show();
            cachingTimestamp = 0;
            return;
        }

        // remove all redundant values, that are older than 24 hours ago,
        // and calculate temperature at accurate 24-hours-back limit (first value in timeline)
        List<Long> sortedTimeList = new ArrayList<>(temperatureMap.keySet());
        int indexOfMinus24h = Collections.binarySearch(sortedTimeList, currentTimeMinus24h);
        double temperatureAtMinus24h;
        if (indexOfMinus24h < 0) {
            indexOfMinus24h = -indexOfMinus24h - 1;
            temperatureAtMinus24h = temperatureByTime(sortedTimeList, indexOfMinus24h, currentTimeMinus24h);
        }
        else {
            temperatureAtMinus24h = temperatureMap.get(currentTimeMinus24h);
        }
        for (int n = 0; n < indexOfMinus24h; n++)
            temperatureMap.remove(sortedTimeList.get(n));
        temperatureMap.put(currentTimeMinus24h, temperatureAtMinus24h);

        List<Double> sortedTempList = new ArrayList<>(temperatureMap.values());
        Collections.sort(sortedTempList);
        // statistical indicators
        double average = averageInList(sortedTempList), median = medianInList(sortedTempList), span = spanInList(sortedTempList);

        // total time, at which temperature was below 0 and below +5, in intervals of previous 12 and 24 hours
        long totalTimeFirstLimit12h, totalTimeSecondLimit12h, totalTimeFirstLimit24h, totalTimeSecondLimit24h;
        totalTimeFirstLimit12h = calculateTotalTimeLessThan(temperatureLimit1, currentTimeMinus12h);
        totalTimeSecondLimit12h = calculateTotalTimeLessThan(temperatureLimit2, currentTimeMinus12h);
        totalTimeFirstLimit24h = calculateTotalTimeLessThan(temperatureLimit1, currentTimeMinus24h);
        totalTimeSecondLimit24h = calculateTotalTimeLessThan(temperatureLimit2, currentTimeMinus24h);

        // display calculated values on screen
        String format = "%.1f " + getString(R.string.hours_caption);
        tvTime1Limit1.setText(String.format(format, timeRangeApproxHours(totalTimeFirstLimit12h)));
        tvTime1Limit2.setText(String.format(format, timeRangeApproxHours(totalTimeSecondLimit12h)));
        tvTime2Limit1.setText(String.format(format, timeRangeApproxHours(totalTimeFirstLimit24h)));
        tvTime2Limit2.setText(String.format(format, timeRangeApproxHours(totalTimeSecondLimit24h)));
        tvDayTemperatureSpan.setText(String.format(getString(R.string.day_temperature_span_label), span));
        tvDayAverageTemperature.setText(String.format(getString(R.string.day_average_temperature_label), average));
        tvDayMedianTemperature.setText(String.format(getString(R.string.day_median_temperature_label), median));

        makePlot(sortedTempList.get(0), sortedTempList.get(sortedTempList.size() - 1));

        if (!useCached)
            cachingTimestamp = System.currentTimeMillis();
    }

    private void makePlot(double minTemp, double maxTemp) {
        //all graphics should have a room, to be visible in plot viewport, i.e. max from greatest and min from the least ordinates:
        double lowLimit = Math.min(Math.round(minTemp), temperatureLimit2);
        double highLimit = Math.max(Math.round(maxTemp), temperatureLimit1);
        //round for drawing scale with 5-degrees step:
        lowLimit = Math.floor(lowLimit / 5.0) * 5.0;
        highLimit = Math.ceil(highLimit / 5.0) * 5.0;

        //fill in the plot with all data series:
        List<DataPoint> dataPoints = new ArrayList<>();
        for (Long time : temperatureMap.keySet())
            dataPoints.add(new DataPoint(time, temperatureMap.get(time)));
        dataPoints.add(new DataPoint(currentTime, dataPoints.get(dataPoints.size() - 1).getY())); //fix for using data from cache
        LineGraphSeries<DataPoint> temperatureSeries = new LineGraphSeries<>(dataPoints.toArray(new DataPoint[dataPoints.size()]));
        LineGraphSeries<DataPoint> temperatureLimit1Series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(currentTimeMinus24h, temperatureLimit1),
                new DataPoint(currentTime, temperatureLimit1)
        });
        LineGraphSeries<DataPoint> temperatureLimit2Series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(currentTimeMinus24h, temperatureLimit2),
                new DataPoint(currentTime, temperatureLimit2)
        });
        graphView.addSeries(temperatureSeries);
        graphView.addSeries(temperatureLimit1Series);
        graphView.addSeries(temperatureLimit2Series);

        //lay out the plot:
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        Viewport viewport = graphView.getViewport();
        //adjust grid settings:
        gridLabelRenderer.setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        gridLabelRenderer.setHighlightZeroLines(true);
        gridLabelRenderer.setHorizontalLabelsVisible(false);
        gridLabelRenderer.setVerticalLabelsVisible(true);
        //tune view of lines:
        viewport.setBackgroundColor(getResources().getColor(R.color.plot_viewport_background_color));
        temperatureSeries.setColor(getResources().getColor(R.color.temperature_series_color));
        temperatureSeries.setThickness(2);
        temperatureLimit1Series.setColor(getResources().getColor(R.color.limit1_series_color));
        temperatureLimit1Series.setThickness(2);
        temperatureLimit2Series.setColor(getResources().getColor(R.color.limit2_series_color));
        temperatureLimit2Series.setThickness(2);
        //set viewport bounds and set the scale:
        //...in horizontal:
        gridLabelRenderer.setNumHorizontalLabels(2);
        viewport.setMinX(currentTimeMinus24h);
        viewport.setMaxX(currentTime);
        viewport.setXAxisBoundsManual(true);
        //...in vertical:
        int numVerticalLabels = (int) (highLimit - lowLimit) / 5 + 1;
        numVerticalLabels = numVerticalLabels < 2 ? 2 : numVerticalLabels;
        gridLabelRenderer.setNumVerticalLabels(numVerticalLabels);
        viewport.setMinY(lowLimit);
        viewport.setMaxY(highLimit);
        viewport.setYAxisBoundsManual(true);
        //set horizontal labels:
        LinearLayout llHorizontalLabels = (LinearLayout) findViewById(R.id.llHorizontalLabels);
        if (llHorizontalLabels.getChildCount() == 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llHorizontalLabels.getLayoutParams();
            params.bottomMargin = 0;
            llHorizontalLabels.setLayoutParams(params);
            gridLabelRenderer.setTextSize(gridLabelRenderer.getTextSize() - 2); //make text a bit smaller
            for (int n = -24; n < 0; n += 3) {
                TextView textView = new TextView(MainActivity.this);
                textView.setText(String.valueOf(n));
                textView.setGravity(Gravity.START);
                textView.setSingleLine();
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f / 8.0f);
                textView.setLayoutParams(params);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, gridLabelRenderer.getTextSize());
                textView.setTextColor(gridLabelRenderer.getVerticalLabelsColor());
                llHorizontalLabels.addView(textView);
            }
            TextView tvZeroLabel = (TextView) findViewById(R.id.tvZeroLabel);
            tvZeroLabel.setText("-0 " + getString(R.string.hours_caption));
            tvZeroLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, gridLabelRenderer.getTextSize());
            tvZeroLabel.setTextColor(gridLabelRenderer.getVerticalLabelsColor());
            tvZeroLabel.setVisibility(View.VISIBLE);
            gridLabelRenderer.setPadding(gridLabelRenderer.getPadding() + 2); //make plot a bit smaller
        }
    }

    private long calculateTotalTimeLessThan(double temperatureLimit, long timeLeftLimit) {
        long totalTime = 0L;

        // calculating temperature at given left time limit:
        double temperatureAtLeftLimit;
        List<Long> timeList = new ArrayList<>(temperatureMap.keySet());
        int indexLeftLimit = Collections.binarySearch(timeList, timeLeftLimit);
        if (indexLeftLimit < 0) {
            indexLeftLimit = -indexLeftLimit - 1;
            temperatureAtLeftLimit = temperatureByTime(timeList, indexLeftLimit, timeLeftLimit);
            for (int n = indexLeftLimit; n > 0; n--)
                timeList.remove(0);
            timeList.add(0, timeLeftLimit);
        }
        else {
            temperatureAtLeftLimit = temperatureMap.get(timeLeftLimit);
            for (int n = indexLeftLimit; n > 0; n--)
                timeList.remove(0);
        }

        // count total time when temperature was less than given limit, on all timeline
        for (int i = 1; i < timeList.size(); i++) {
            long t1 = timeList.get(i - 1), t2 = timeList.get(i);
            double y1 = i == 1 ? temperatureAtLeftLimit : temperatureMap.get(t1), y2 = temperatureMap.get(t2);
            if (y2 < temperatureLimit && y1 < temperatureLimit)
                totalTime += t2 - t1;
            else if (y2 > temperatureLimit && y1 < temperatureLimit)
                totalTime += timeByTemperature(t1, y1, t2, y2, temperatureLimit) - t1;
            else if (y2 < temperatureLimit && y1 > temperatureLimit)
                totalTime += t2 - timeByTemperature(t1, y1, t2, y2, temperatureLimit);
        }

        return totalTime;
    }

    private double kelvinToCelsius(double tempInKelvin) {
        return tempInKelvin - 273.15;
    }

    private double timeRangeApproxHours(long timeRange) {
        timeRange /= 60;
        return Math.round(timeRange / 60.0 * 2) / 2.0; //rounding with step 0.5 hours
        //return Math.round(timeRange / 60.0 * 10) / 10.0; //to rounding with step 0.1 hours
    }

    private double averageInList(List<Double> list) {
        double avg = 0.0;
        for (Double d : list) avg += d;
        return avg / list.size();
    }

    private double medianInList(List<Double> sortedList) {
        int size = sortedList.size();
        return size % 2 == 0 ? (sortedList.get(size / 2) + sortedList.get(size / 2 - 1)) / 2.0 : sortedList.get(size / 2);
    }

    private double spanInList(List<Double> sortedList) {
        return sortedList.get(sortedList.size() - 1) - sortedList.get(0);
    }

    private double temperatureByTime(List<Long> timeList, int index, long time) {
        long t1 = timeList.get(index - 1), t2 = timeList.get(index);
        double y1 = temperatureMap.get(t1), y2 = temperatureMap.get(t2);
        double k = (y2 - y1) / (t2 - t1);
        double b = y2 - k * t2;
        return k * time + b;
    }

    private double timeByTemperature(long t1, double y1, long t2, double y2, double temperature) {
        double k = (y2 - y1) / (t2 - t1);
        double b = y2 - k * t2;
        return (temperature - b) / k;
    }

    private boolean isCacheExpired() {
        return System.currentTimeMillis() > cachingTimestamp + CACHE_EXPIRE_TIME;
    }


    private Response.Listener<JSONObject> responseCurrentListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            JSONObject jsonMainSection = response.optJSONObject("main");
            if (jsonMainSection == null)
                return;
            double currentTemp = kelvinToCelsius(jsonMainSection.optDouble("temp"));
            long dt = response.optLong("dt"); //time of temperature registration can be not synchronized with time of request done
            temperatureMap.put(currentTime, currentTemp);
            currentIsGained = true;
            processValues(false);
        }
    };

    private Response.Listener<JSONObject> responseHistoryListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            JSONArray jsonArray = response.optJSONArray("list");
            if (jsonArray == null)
                return;
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    double temp = kelvinToCelsius(jsonObject.getJSONObject("main").optDouble("temp"));
                    long time = jsonObject.optLong("dt");
                    temperatureMap.put(time, temp);
                }
                historyIsGained = true;
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, getString(R.string.response_error_message), Toast.LENGTH_SHORT).show();
                historyIsGained = false;
            }
            processValues(false);
        }
    };

    private Response.ErrorListener errorResponseListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Toast.makeText(MainActivity.this, getString(R.string.response_error_message), Toast.LENGTH_SHORT).show();
            currentIsGained = historyIsGained = false;
            llLoader.setVisibility(View.GONE);
            ptrLayout.setRefreshComplete();
        }
    };

}
