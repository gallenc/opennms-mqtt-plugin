README.txt

GENERATED PROJECT
-----------------
project groupId: org.opennms.plugins
project name:    MqttReceiver
version:         0.0.1-SNAPSHOT

NEXT STEPS
----------

Once the archetype has completed you must remove the <modules> section from the parent pom.
This is generated automatically by the archetype but is not needed as modules sections are
included in the default profile.

The first time you build the project you should generate a new licence using
mvn clean install -P generateLicence

Once a licence is generated you only need to build the project using
mvn clean install

This avoids having to install new licences with every build.

open karaf command prompt using
ssh -p 8101 admin@localhost

to install the feature in karaf use
karaf@root> features:addurl mvn:org.opennms.plugins/MqttReceiver/0.0.1-SNAPSHOT/xml/features
karaf@root> features:install MqttReceiver

(or features:install MqttReceiver/0.0.1-SNAPSHOT for a specific version of the feature)

to install the licence specification in karaf use
karaf@root>  osgi:install -s  mvn:org.opennms.plugins/MqttReceiver.licence-spec/0.0.1-SNAPSHOT

To install on OpenNMS
---------------------
You need to add the repo where the feature is installed to the opennms karaf configuration.
Obviously this could point at a remote repository
However if you have built on your local machine, add the local repo as follows;

sudo vi /opt/opennms/org.ops4j.pax.url.mvn.cfg

change the following property to add file:/home/admin/.m2/repository@snapshots@id=localrepo 
where /home/admin/.m2/repository is the location of local maven repository

org.ops4j.pax.url.mvn.repositories= \
    http://repo1.maven.org/maven2@id=central, \
    http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix, \
    http://repository.springsource.com/maven/bundles/release@id=springsource.release, \
    http://repository.springsource.com/maven/bundles/external@id=springsource.external, \
    https://oss.sonatype.org/content/repositories/releases/@id=sonatype, \
    file:/home/admin/.m2/repository@snapshots@id=localrepo



