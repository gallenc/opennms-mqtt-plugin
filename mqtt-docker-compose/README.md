 
## Docker compose test file for MQTT plugin

This project contains the docker-compose configuration to run the following components on a single VM to test the MQTT plugin.
This configuration is based upon the docker image available at https://hub.docker.com/r/opennms/horizon-core-web/
And docker configuration source at https://github.com/opennms-forge/docker-horizon-core-web and 

* OpenNMS Docker (stable 21.0.4-1)
* Cassandra / Newts Docker
* Grafana
* Service Mix (ActiveMQ MQTT broker)

