# WeatherLog
<p>WeatherLog is an app for Android that shows, how changed an air temperature within previous 24 hours.<br/>
The application helps to answer, if the road was icing up after cold night; is it a time 
to change tires to/from winter ones; what kind of clothes or shoes would be well to wear today. 
Also it may be useful in any other cases, when there needs to monitor temperature changes and analyze them.</p>

<p>Here is a plot with temperature changes, from the moment at 24 hours back from now (the extreme left point), 
to the current time (the extreme right point). Also there are two horizotal lines, that represent 
0 and +5 Celsius degrees limits - freezing point of water and temperature of deterioration of road grip of tires. 
Looking at the plot it's possible to make a conclusion about how long and when temperature became lower 
than mentioned values.<br/>
Total time of temperature was being below specified limits is showed in a table. <br/>
Under the table there are 24-hours temperature span between max and min values, average and median temperatures.</p>

<p>App allows to choose city, where the temperature is registered (default is a Moscow, Russia). 
The data about weather in these cities are provided by OpenWeatherMap, so possibility of displaying them 
in the application is fully depends on availability of this service and existence of required data. 
In the list of found cities there are also geo coords (in decimal view) and OpenWeatherMap city_id. 
(It is necessary to use English name of city and enter not less than 3 letters of the name for successful search).
</p>

<p><b>About usage</b><br/>
<ul>
<li>Understanding displayed information:<br/>
1) the end of temperature curve represents the temperature now, the begin of curve represents the temperature at
24 hours before from now (e.g., if current time is 15:42, 8-th Feb., hence, this time is at right side of plot, 
and the left side of plot is at 15:42 at 7-th Feb.);<br/>
2) the total time when temperature was below 0- or 5-degrees limits is calculated as a sum of all such
time segments at whole time diapason (previous 12 and 24 hours before current time);<br/>
3) temperature span is a difference between max and min temperatures from requested array of key points, 
at previous 24 hours;<br/>
4) average temperature is calculated also using array of values of key points, that requested to draw a plot;<br/>
5) median temperature is calculated as a median value from this array too.
<li>To refresh/reload weather data use pull-to-refresh on the plot screen.</li>
<li>To change city from default to custom one open "Settings" menu and find necessary city, by entering 3 or more 
letters from its name and press search button. Then choose city from result list.</li>
<li>If you type few letters in a city name field, the gained list may be too long, and it is possible to see 
no desired city: then type more letters from the city name as a search pattern.</li>
<li>Because of OpenWeatherMap service limitations you could get no weather data, or no desired city/place, 
or have problems with requesting information at the moment. There are not WeatherLog troubles, but server-side.
<br/>Also try to use the app not very frequently, because OpenWeatherMap has limits for number of requests 
per hour/day, so you can be blocked for a day using WeatherLog. If it happens, try to wait.</li>
</ul>
</p>

<p><b>Third-party resources</b><br/>
WeatherLog is in fact a client-side app for OpenWeatherMap service. So you can use it only if this service 
is available.<br/>
WeatherLog uses these 3rd-party services and libraries:
<ul>
<li><a href="http://openweathermap.org">OpenWeatherMap API</a></li>
<li><a href="https://github.com/mcxiaoke/android-volley">Google Volley (github clone)</a></li>
<li><a href="https://github.com/chrisbanes/ActionBar-PullToRefresh">ActionBar-PullToRefresh</a></li>
<li><a href="http://www.android-graphview.org/">GraphView for Android</a></li>
</ul>
</p>

<hr/>

#License
<pre>
Copyright © 2015 Sergey Tuzlukov

WeatherLog is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

WeatherLog is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with WeatherLog. If not, see http://www.gnu.org/licenses/ .
</pre>
