package org.opennms.plugin.jsontest.test.manual;

import java.io.File;

public class Main {

	 public static void main(String[] args) {
		 System.out.println("JSON tester starting up");
		 if (args.length==0) {
			 System.out.println("please supply path and name of properties file");
			 System.exit(0);
		 }
		 JSONPosTestDataTransmitter mqttTest = new JSONPosTestDataTransmitter();
		 
		 String filename=null;
		 try {
			 filename = args[0];
			 File f = new File(filename);
			 System.out.println("loading properties file: "+f.getAbsolutePath());
			 mqttTest.readProperties(filename);
			 
		 } catch (Exception ex){
			 System.out.println("cannot load properties file: "+filename);
			 ex.printStackTrace(System.out);
			 System.exit(0);
		 }
		 System.out.println("loaded the following properties from supplied filename: "+filename+"\n"+mqttTest.toString());
		 System.out.println("starting transmitter use ctrl-c to exit");
		 mqttTest.testJsonTransmitter();
		 System.out.println("JSON tester shutting down");
	 }

}
