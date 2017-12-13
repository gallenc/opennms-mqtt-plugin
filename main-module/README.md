Example post to send event

----
POST http://192.168.75.129:8980/opennms/plugin/mqtt/v1-0/receive/mqtt-events/0/
Content-Type: application/json;charset=utf-8
{"cityName":"Southampton","PM1":"0.956375928404503","PM25":"0.23909398210112576","latitude":50.9246217,"averaging":0,"PM10":"0.09563759284045031","stationName":"Common#1","time":"2017-12-13 09:03:43.004000","id":"monitorID","longitude":-1.374114}
----