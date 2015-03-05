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

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NetworkQuery {

    public static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";
    public static final String HISTORY_URL = "history/city";
    public static final String CURRENT_URL = "weather";
    public static final String FIND_URL = "find";

    private static NetworkQuery ourInstance;

    private RequestQueue queue;
    private String appId = "";


    private NetworkQuery(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public static NetworkQuery getInstance(Context context) {
        if (ourInstance == null)
            ourInstance = new NetworkQuery(context);
        return ourInstance;
    }


    public void addRequest(String url, Params params, Response.Listener<JSONObject> responseListener,
                           Response.ErrorListener errorResponseListener, Object tag) {
        queue.add(new JsonObjectRequest(BASE_URL + url + "?" + (appId.isEmpty() ? params : params.addParam(Params.APPID, appId)),
                null, responseListener, errorResponseListener)
                .setShouldCache(true).setTag(tag));
    }

    public void cancelAllRequests(Object tag) {
        queue.cancelAll(tag);
    }

    public void setAppId(String appId) {
        this.appId = appId == null ? "" : appId;
    }


    public static class Params {
        public static final String TYPE = "type";
        public static final String START = "start";
        public static final String END = "end";
        public static final String ID = "id";
        public static final String QUERY = "q";
        public static final String APPID = "APPID";

        public static final String TYPE_HOUR = "hour";
        public static final String TYPE_LIKE = "like";

        private Map<String, Object> map = new HashMap<>();

        public Params addParam(String name, Object value) {
            map.put(name, value);
            return this;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (String name : map.keySet())
                builder.append(name).append("=").append(map.get(name)).append("&");
            builder.replace(builder.lastIndexOf("&"), builder.length(), "");
            return builder.toString();
        }
    }

    public static class Defaults { //for Moscow, RU
        public static final String CITY_NAME = "Moscow";
        public static final String CITY_COUNTRY = "RU";
        public static final int CITY_ID = 524901;
        public static final float CITY_LAT = 55.75222f;
        public static final float CITY_LON = 37.615555f;
    }

}
