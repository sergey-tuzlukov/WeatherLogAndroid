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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SettingsActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener,
        TextView.OnEditorActionListener, Response.Listener<JSONObject>, Response.ErrorListener {

    private static final String SAVED_CITY_NAME_REQUESTED = "SAVED_CITY_NAME_REQUESTED";
    private static final String SAVED_CITY_LIST = "SAVED_CITY_LIST";
    private static final String SAVED_LIST_VISIBILITY = "SAVED_LIST_VISIBILITY";
    private static final String SAVED_MESSAGE_VISIBILITY = "SAVED_MESSAGE_VISIBILITY";
    private static final String SAVED_RESULT_CODE = "SAVED_RESULT_CODE";
    private static final String SAVED_LOADER_VISIBILITY = "SAVED_LOADER_VISIBILITY";
    private static final String SAVED_DIALOG_APPID = "SAVED_DIALOG_APPID";
    private static final String SAVED_DIALOG_VISIBILITY = "SAVED_DIALOG_VISIBILITY";

    private SharedPreferences preferences;
    private NetworkQuery networkQuery;
    private int resultCode = RESULT_CANCELED;
    private static boolean refreshWasCancelled;
    private RegisterDialog registerDialog;

    private List<City> cityList = new ArrayList<>();

    private TextView tvRegisterAppId;
    private TextView tvSelectedCityInfo;
    private EditText etCityName;
    private ImageButton ibSearch;
    private ListView lvVariants;
    private TextView tvErrorMessage;
    private LinearLayout llLoader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvRegisterAppId = (TextView) findViewById(R.id.tvRegisterAppId);
        tvSelectedCityInfo = (TextView) findViewById(R.id.tvSelectedCityInfo);
        etCityName = (EditText) findViewById(R.id.etCityName);
        ibSearch = (ImageButton) findViewById(R.id.ibSearch);
        lvVariants = (ListView) findViewById(R.id.lvVariants);
        tvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);
        llLoader = (LinearLayout) findViewById(R.id.llLoader);

        preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        networkQuery = NetworkQuery.getInstance(getApplicationContext());
        registerDialog = new RegisterDialog(this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String appId = registerDialog.getEtAppId().getText().toString().trim();
                networkQuery.setAppId(appId);
                preferences.edit().putString(NetworkQuery.Params.APPID, appId).apply();
                resultCode = RESULT_OK;
                setResult(resultCode);
                if (!appId.isEmpty())
                    tvRegisterAppId.setText(getString(R.string.current_appid_caption) + " " + appId);
                else
                    tvRegisterAppId.setText(getString(R.string.register_appid_label));
            }
        });

        String appId = preferences.getString(NetworkQuery.Params.APPID, "");
        if (!appId.isEmpty()) {
            tvRegisterAppId.setText(getString(R.string.current_appid_caption) + " " + appId);
            registerDialog.getEtAppId().setText(appId);
            //networkQuery.setAppId();
        }
        tvSelectedCityInfo.setText(String.format(getString(R.string.current_city_selected_label),
                preferences.getString("city", NetworkQuery.Defaults.CITY_NAME),
                preferences.getString("country", NetworkQuery.Defaults.CITY_COUNTRY),
                preferences.getInt("id", NetworkQuery.Defaults.CITY_ID),
                preferences.getFloat("lat", NetworkQuery.Defaults.CITY_LAT),
                preferences.getFloat("lon", NetworkQuery.Defaults.CITY_LON)));
        ibSearch.setOnClickListener(this);
        lvVariants.setOnItemClickListener(this);
        etCityName.setOnEditorActionListener(this);
        tvRegisterAppId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerDialog.getEtAppId().setText(preferences.getString(NetworkQuery.Params.APPID, ""));
                registerDialog.show();
            }
        });

        if (savedInstanceState != null) {
            etCityName.setText(savedInstanceState.getString(SAVED_CITY_NAME_REQUESTED));
            cityList = savedInstanceState.getParcelableArrayList(SAVED_CITY_LIST);
            lvVariants.setVisibility(savedInstanceState.getBoolean(SAVED_LIST_VISIBILITY) ? View.VISIBLE : View.GONE);
            tvErrorMessage.setVisibility(savedInstanceState.getBoolean(SAVED_MESSAGE_VISIBILITY) ? View.VISIBLE : View.GONE);
            lvVariants.setAdapter(makeAdapter());
            resultCode = savedInstanceState.getInt(SAVED_RESULT_CODE, RESULT_CANCELED);
            setResult(resultCode);
            boolean refreshWasRun = savedInstanceState.getBoolean(SAVED_LOADER_VISIBILITY) || refreshWasCancelled;
            if (refreshWasRun)
                onClick(etCityName);
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
        outState.putString(SAVED_CITY_NAME_REQUESTED, etCityName.getText().toString());
        outState.putParcelableArrayList(SAVED_CITY_LIST, (ArrayList<? extends Parcelable>) cityList);
        outState.putBoolean(SAVED_LIST_VISIBILITY, lvVariants.getVisibility() == View.VISIBLE);
        outState.putBoolean(SAVED_MESSAGE_VISIBILITY, tvErrorMessage.getVisibility() == View.VISIBLE);
        outState.putInt(SAVED_RESULT_CODE, resultCode);
        outState.putBoolean(SAVED_LOADER_VISIBILITY, llLoader.getVisibility() == View.VISIBLE);
        outState.putString(SAVED_DIALOG_APPID, registerDialog.getEtAppId().getText().toString());
        outState.putBoolean(SAVED_DIALOG_VISIBILITY, registerDialog.isVisible());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (refreshWasCancelled)
            onClick(etCityName);
    }

    @Override
    protected void onStop() {
        super.onStop();
        networkQuery.cancelAllRequests(SettingsActivity.this);
        if (llLoader.getVisibility() == View.VISIBLE) {
            refreshWasCancelled = true;
            llLoader.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        registerDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        //avoid auto-refreshing after pressing Back while doing search and then returning to Settings:
        refreshWasCancelled = false;
        llLoader.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            onClick(v);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        refreshWasCancelled = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        llLoader.setVisibility(View.VISIBLE);
        networkQuery.addRequest(NetworkQuery.FIND_URL, new NetworkQuery.Params()
                .addParam(NetworkQuery.Params.TYPE, NetworkQuery.Params.TYPE_LIKE)
                .addParam(NetworkQuery.Params.QUERY, etCityName.getText().toString().trim()),
                this, this, SettingsActivity.this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        City city = cityList.get(position);
        tvSelectedCityInfo.setText(String.format(getString(R.string.current_city_selected_label),
                city.getName(), city.getCountry(), city.getId(), city.getLat(), city.getLon()));
        lvVariants.setVisibility(View.GONE);
        Toast.makeText(this, getString(R.string.city_changed_message), Toast.LENGTH_SHORT).show();
        preferences.edit().putString("city", city.getName()).putString("country", city.getCountry()).
                putInt("id", city.getId()).putFloat("lat", city.getLat()).putFloat("lon", city.getLon())
                .apply();
        resultCode = RESULT_OK;
        setResult(resultCode);
    }

    @Override
    public void onResponse(JSONObject response) {
        llLoader.setVisibility(View.GONE);
        JSONArray jsonArray = response.optJSONArray("list");
        if (jsonArray == null || jsonArray.length() == 0) {
            showVariants(false);
            return;
        }
        try {
            cityList.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                cityList.add(new City(
                        jsonObject.getString("name"),
                        jsonObject.getJSONObject("sys").getString("country"),
                        jsonObject.getInt("id"),
                        (float) jsonObject.getJSONObject("coord").getDouble("lat"),
                        (float) jsonObject.getJSONObject("coord").getDouble("lon")));
            }
            lvVariants.setAdapter(makeAdapter());
            showVariants(true);
        } catch (JSONException e) {
            Toast.makeText(SettingsActivity.this, getString(R.string.response_error_message), Toast.LENGTH_SHORT).show();
            showVariants(false);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        llLoader.setVisibility(View.GONE);
        Toast.makeText(SettingsActivity.this, getString(R.string.response_error_message), Toast.LENGTH_SHORT).show();
//            showVariants(false);
    }

    private void showVariants(boolean visible) {
        lvVariants.setVisibility(visible ? View.VISIBLE : View.GONE);
        tvErrorMessage.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private SimpleAdapter makeAdapter() {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map;
        for (City city : cityList) {
            map = new HashMap<>();
            map.put("Fullname", String.format("%1$s [%2$s]", city.getName(), city.getCountry()));
            map.put("Info", String.format("lat=%1$.6f; lon=%2$.6f\nid=%3$d", city.getLat(), city.getLon(), city.getId()));
            list.add(map);
        }
        return new SimpleAdapter(this, list, android.R.layout.simple_list_item_2,
                new String[] {"Fullname", "Info"}, new int[] {android.R.id.text1, android.R.id.text2});
    }


    private class City implements Parcelable {
        private String name;
        private String country;
        private int id;
        private float lat;
        private float lon;

        public City(String name, String country, int id, float lat, float lon) {
            this.name = name;
            this.country = country;
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }

        public String getName() { return name; }
        public String getCountry() { return country; }
        public int getId() { return id; }
        public float getLat() { return lat; }
        public float getLon() { return lon; }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeString(country);
            dest.writeInt(id);
            dest.writeFloat(lat);
            dest.writeFloat(lon);
        }

        public void readFromParcel(Parcel src) {
            name = src.readString();
            country = src.readString();
            id = src.readInt();
            lat = src.readFloat();
            lon = src.readFloat();
        }

        private City(Parcel in) {
            readFromParcel(in);
        }

        public /*static*/ final Parcelable.Creator<City> CREATOR = new Parcelable.Creator<City>() {
            @Override
            public City createFromParcel(Parcel source) {
                return new City(source);
            }
            @Override
            public City[] newArray(int size) {
                return new City[size];
            }
        };
    }


    public static class RegisterDialog {
        private AlertDialog dialog;
        private EditText etAppId;

        public RegisterDialog(Activity activity, DialogInterface.OnClickListener positiveClickListener) {
            View view = activity.getLayoutInflater().inflate(R.layout.dialog_register, null);
            TextView tvRegisterProcedureDescription = (TextView) view.findViewById(R.id.tvRegisterProcedureDescription);
            Linkify.addLinks(tvRegisterProcedureDescription, Linkify.WEB_URLS);
            tvRegisterProcedureDescription.setMovementMethod(LinkMovementMethod.getInstance());
            etAppId = (EditText) view.findViewById(R.id.etAppId);
            dialog = new AlertDialog.Builder(activity).setTitle(R.string.title_dialog_register).setView(view)
                    .setPositiveButton(R.string.button_dialog_save, positiveClickListener)
                    .setNegativeButton(R.string.button_dialog_skip, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .create();
        }

        public void show() {
            dialog.show();
        }

        public void dismiss() {
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        }

        public EditText getEtAppId() {
            return etAppId;
        }

        public boolean isVisible() {
            return dialog.isShowing();
        }
    }

}
