package org.opennms.plugins.mqttclient.test.manual;

public class Main {

	 public static void main(String[] args) {
		 System.out.println("mqtt tester");
		 if (args.length==0) {
			 System.out.println("please supply path and name of properties file");
			 System.exit(0);
		 }
		 MQTTJsonTransmitterTest mqttTest = new MQTTJsonTransmitterTest();
		 
		 String filename=null;
		 try {
			 filename = args[0];
			 System.out.println("loading properties file: "+filename);	
			 mqttTest.readProperties(filename);
			 
		 } catch (Exception ex){
			 System.out.println("cannot load properties file: "+filename);
			 ex.printStackTrace(System.out);
			 System.exit(0);
		 }
		 System.out.println("loaded the following properties from supplied filename: "+filename+"\n"+mqttTest.toString());
		 System.out.println("starting transmitter use ctrl-c to exit");
		 mqttTest.testMqttJsonTransmitter();
		 
	 }

}
