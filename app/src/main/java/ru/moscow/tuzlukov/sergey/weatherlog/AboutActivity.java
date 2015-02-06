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

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;


public class AboutActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView tvAboutLicense = (TextView) findViewById(R.id.tvAboutLicense);
        Linkify.addLinks(tvAboutLicense, Linkify.WEB_URLS);
        tvAboutLicense.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvAboutWeatherProvider = (TextView) findViewById(R.id.tvAboutWeatherProvider);
        Linkify.addLinks(tvAboutWeatherProvider, Linkify.WEB_URLS);
        tvAboutWeatherProvider.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvCopyrghtInfo = (TextView) findViewById(R.id.tvCopyrghtInfo);
        Linkify.addLinks(tvCopyrghtInfo, Linkify.EMAIL_ADDRESSES);
        tvCopyrghtInfo.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
