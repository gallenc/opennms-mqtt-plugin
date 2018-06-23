 
## Docker compose test file for MQTT plugin

This project contains the docker-compose configuration to run the following components on a single VM to test the MQTT plugin.
This configuration is based upon the docker image available at https://hub.docker.com/r/opennms/horizon-core-web/
And docker configuration source at https://github.com/opennms-forge/docker-horizon-core-web and 

* OpenNMS Docker (stable 21.0.4-1)
* Cassandra / Newts Docker
* Grafana
* Service Mix (ActiveMQ MQTT broker)

## Cassandra data extraction
to extract data directly from cassandra, install the following user defined procedures
```
CREATE OR REPLACE FUNCTION newts.type (value blob)  CALLED ON NULL INPUT  RETURNS text 
LANGUAGE java AS ' 
if (value == null) {     return null;   }
Byte b = value.get();   
if(b.intValue()==1) {
  return "COUNTER";
} else if (b.intValue()==2) {
   return "ABSOLUTE";
} else if (b.intValue()==3) {
  return "DERIVE";
} else if (b.intValue()==4) {
  return "GAUGE";
} 
return "unknown type:"+b.intValue(); ' ;

CREATE OR REPLACE FUNCTION newts.valueNumber (value blob)  CALLED ON NULL INPUT  RETURNS double 
LANGUAGE java AS ' 
if (value == null) { return null; }  
Byte b = value.get();  
if(b.intValue()==1 || b.intValue()==2 || b.intValue()==3 ) {
    return Double.longBitsToDouble( value.getLong());
  } else if (b.intValue()==4) {
    return value.getDouble();
  } 
throw new IllegalArgumentException(" newts.valueNumberfunction  unknown type:"+b.intValue()); '  ;

```
This allows a cql query to access newts data in cassandra

```
select  collected_at, resource, metric_name, newts.type(value), newts.valueNumber(value) from samples;

```


