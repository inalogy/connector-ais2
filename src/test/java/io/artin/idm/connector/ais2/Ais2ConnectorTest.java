package io.artin.idm.connector.ais2;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class Ais2ConnectorTest {

	private static final Log LOG = Log.getLog(Ais2ConnectorTest.class);

	private Ais2Configuration configuration;

	private Ais2Connector connector;

	final Properties properties = new Properties();

	{
		String fileName = "test.properties";

		InputStream inputStream = Ais2ConnectorTest.class.getClassLoader().getResourceAsStream(fileName);
		if (inputStream == null) {
			LOG.warn("Sorry, unable to find " + fileName);
		}
		else {
			try {
				properties.load(inputStream);
			} catch (IOException e) {
				LOG.error(e, "Unable to load properties file " + fileName);
			}
		}

		// keytool -import -alias ca -keystore trust-store.jks -storepass TrustStorePassword -trustcacerts -file zora-sap-orange-sk.pem
		if (properties.containsKey("trustStore"))
			System.setProperty("javax.net.ssl.trustStore", properties.getProperty("trustStore"));
		if (properties.containsKey("trustStorePassword"))
			System.setProperty("javax.net.ssl.trustStorePassword", properties.getProperty("trustStorePassword"));
	}

	@BeforeClass
	public void initConfiguration() {
		configuration = new Ais2Configuration();

 		if (properties.containsKey("vratOsobyUrl")) {
			configuration.setVratOsobyUrl(properties.getProperty("vratOsobyUrl"));
		}

		if (properties.containsKey("username")) {
			configuration.setUsername(properties.getProperty("username"));
			configuration.setPassword(new GuardedString(properties.getProperty("password").toCharArray()));
		}

		if (properties.containsKey("soapLogBasedirStr")){
			configuration.setSoapLogBasedir(properties.getProperty("soapLogBasedirStr").replace("{user.name}", System.getProperty("user.name")));
		}

		if (properties.containsKey("receiveTimeout")) {
			configuration.setReceiveTimeout(Integer.parseInt(properties.getProperty("receiveTimeout")));
		}

		if (properties.containsKey("filterOdId")) {
			configuration.setFilterOdId(Integer.parseInt(properties.getProperty("filterOdId")));
		}

		if (properties.containsKey("filterDoId")) {
			configuration.setFilterDoId(Integer.parseInt(properties.getProperty("filterDoId")));
		}

		if (properties.containsKey("pageSize")) {
			configuration.setPageSize(Integer.parseInt(properties.getProperty("pageSize")));
		}

		if (properties.containsKey("ais2TrustAllCerts")) {
			configuration.setAis2TrustAllCerts(Boolean.parseBoolean(properties.getProperty("ais2TrustAllCerts")));
		}

		if (properties.containsKey("keepFullXml")) {
			configuration.setKeepFullXml(Boolean.parseBoolean(properties.getProperty("keepFullXml")));
		}

		if (properties.containsKey("enableNastavOsobInfo")) {
			configuration.setEnableNastavOsobInfo(Boolean.parseBoolean(properties.getProperty("enableNastavOsobInfo")));
		}

		if (properties.containsKey("ais2TrustAllCerts")) {
			configuration.setEnableUlozZamestnanca(Boolean.parseBoolean(properties.getProperty("enableUlozZamestnanca")));
		}
 	}

	@BeforeMethod
	public void initConnector() {
		connector = new Ais2Connector();
		connector.init(configuration);
	}

	@Test
	public void testConnectorTest() {
		try {
			connector.test();
		} catch (RuntimeException ex) {
			fail(ex.getMessage(), ex);
		}
	}

	@Test
	public void testCheckAlive() {
		try {
			connector.checkAlive();
		} catch (RuntimeException ex) {
			fail("not alive");
		}
	}

	@Test
	public void testfindById() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				return true;
			}
		};

		Ais2Filter sf = new Ais2Filter();
		sf.byId = "1023736"; //GP

		LOG.ok("start finding");
		connector.executeQuery(objectClass, sf, rh, null);
	}

	@Test
	public void testUnknowUidException() {
		try {
			ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

			ResultsHandler rh = new ResultsHandler() {
				@Override
				public boolean handle(ConnectorObject connectorObject) {
					return true;
				}
			};

			Ais2Filter sf = new Ais2Filter();
			sf.byId = "-1";

			LOG.ok("start finding");
			connector.executeQuery(objectClass, sf, rh, null);
			LOG.ok("end finding");
			fail("Should have thrown UnknownUidException");
		} catch (UnknownUidException ex) {
			LOG.ok("UnknownUidException caught.");
		}
	}

	@Test
	public void testfindByInterval() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				return true;
			}
		};

		Ais2Filter sf = new Ais2Filter();
		sf.byInterval = new Interval(1023736, 1023800); //GP

		LOG.ok("start finding");
		connector.executeQuery(objectClass, sf, rh, null);
	}

	@Test
	public void testFindAllOsoba() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				return true;
			}
		};

		LOG.ok("start finding all");
		connector.executeQuery(objectClass, null, rh, null);
		LOG.ok("end finding");
	}
}
