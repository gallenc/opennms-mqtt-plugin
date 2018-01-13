# Description
This OpenNMS plugin adds the ability for OpenNMS to received streaming measurements and events from an IoT infrastructure. 
The plugin supports MQTT messages and also asynchronous messages sent into OpenNMS from a ReST API.
OpenNMS can register with an MQTT broker to receive MQTT messages.
Several brokers can be subscribed to at the same time and several topic subscriptions can be defined on each broker. 
OpenNMS will attempt to reconnect to a broker if a connection is lost.
Each topic message can be configured to generate an OpenNMS event or to persist an OpenNMS date value (or both).
Currently the expected MQTT payloads are json messages which are parsed using a jxpath configuration in much the same way as jxpath is used in the OpenNMS XMLCollector. 
Example configurations are provided for Eclipse Kura and Solent Airwatch Sniffy messages.

(A future enhancement will be to support gzip compressed messages, xml messages and protobuf messages sent over MQTT payloads)

# Architecture
The architecture of the feature is shown in the following diagram.
![image](https://github.com/gallenc/tmforumhack2017/blob/master/TMForumVancouver2017Hack/opennms/MqttClient/diagrams1.jpg)

# Building and deploying

(Please note that the present version does not use the new OpenNMS streaming API and so is backwards compatible with Meridian 2017. It interacts with OpenNMS in much the same way.)

You need to target the build against the correct major version of OpenNMS.
To do this, edit the parent pom.xml and change the property

''''
    <org.opennms.version>20.0.2</org.opennms.version>
''''
    
To match the version you are targeting. 

to build used 
```
mvn build install
```

Io install in OpenNMS, 
1. edit the opennms/etc/custom.properties  file

add the following org.opennms.protocols.xml.config package to the property
```
org.osgi.framework.system.packages.extra=org.apache.karaf.branding,\
        sun.misc,\
        ...
        org.opennms.protocols.xml.config;version=20.0.2, \
        ...
     
     ( matching your opennms version)
```

2. Edit and place the sample mqtt-interface-config.xml in the opennms/etc directory
(the sample is in the main-module here: https://github.com/gallenc/tmforumhack2017/blob/master/TMForumVancouver2017Hack/opennms/MqttClient/main-module/mqtt-interface-config.xml)

3. Place the target kar built in kar-package into the OpenNMS/deploy directory.

4. restart opennms

If you log into the karaf consol (using ssh -p 8101 admin@localhost) and execute log:tail, you will see the feature deploy

you can send an example test message to the ReST interface using the Firefox HttpRequester plugin with the following raw (sniffy) request;
```
POST http://localhost:8980/opennms/plugin/mqtt/v1-0/receive/mqtt-events/0/
Username: admin
Content-Type: application/json;charset=utf-8
{"time":"2017-12-13 09:03:43.004000","id":"6","formula1":"PM1.constant1+PM1.constant2", "constant1":"1200.0", "constant2":"34.0"}
```


