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

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }
}