package com.inalogy.midpoint.connector.ais2;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class Ais2ConnectorTest {

	private static final Log LOG = Log.getLog(Ais2ConnectorTest.class);
	private static final String ATTRIBUTE_DELIMITER = ";";

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

		configuration.setFilterOdId(Integer.parseInt(properties.getProperty("filterOdId", "1023736")));
		configuration.setFilterDoId(Integer.parseInt(properties.getProperty("filterDoId", "1023800")));

		if (properties.containsKey("pageSize")) {
			configuration.setPageSize(Integer.parseInt(properties.getProperty("pageSize")));
		}

		if (properties.containsKey("ais2TrustAllCerts")) {
			configuration.setAis2TrustAllCerts(Boolean.parseBoolean(properties.getProperty("ais2TrustAllCerts")));
		}

		if (properties.containsKey("keepFullXml")) {
			configuration.setKeepFullXml(Boolean.parseBoolean(properties.getProperty("keepFullXml")));
		}

		configuration.setEnableNastavOsobInfo(Boolean.parseBoolean(properties.getProperty("enableNastavOsobInfo", "true")));
		configuration.setEnableUlozZamestnanca(Boolean.parseBoolean(properties.getProperty("enableUlozZamestnanca", "true")));
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
		sf.byId = "1441085"; //""1023736"; //GP

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

	@Test
	public void testCreateUser() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);
		String login = "test15";

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue("10000").build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue("test").build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("user").build());
		addWriteAttributes(attributes, "10000", login);

		LOG.ok("start creating");
		connector.create(objectClass, attributes, null);
		LOG.ok("end creating");
	}

	@Test
	public void testUpdateUser() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);
		String uidValue = "1441091";
		String login = "test1773055314577";

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue("9999").build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue("test3").build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("User").build());
		addWriteAttributes(attributes, "9999", login);

		LOG.ok("start updating");
		connector.update(objectClass, new Uid(uidValue), attributes, null);
		LOG.ok("end updating");
	}

	private void addWriteAttributes(Set<Attribute> attributes, String uoc, String login) {
		String delimiter = ATTRIBUTE_DELIMITER;
		attributes.add(new AttributeBuilder().setName("LZZamestnanec")
				.addValue(String.join(delimiter, "2026-01-12", "", "27", "4", "10110450", "LF.Dek", "0.50", "0901000001"))
				.addValue(String.join(delimiter, "2026-02-01", "", "27", "4", "10110451", "LF.Dek", "0.25", "0901000002"))
				.build());
		attributes.add(new AttributeBuilder().setName("LZIdentifKarta")
				.addValue(String.join(delimiter, uoc, "UOC", "", "", "", "2026-01-01", "", ""))
				.addValue(String.join(delimiter, "49095043", "PIK", "", "", "", "2026-01-01", "", ""))
				.build());
		attributes.add(new AttributeBuilder().setName("initPasswd").addValue("3899fda10b0a9b19ce3ceebc1924b175a1ebb000").build());
		attributes.add(new AttributeBuilder().setName("liveID").addValue(login + ".live.id").build());
		attributes.add(new AttributeBuilder().setName("email").addValue(login + "@example.com").build());
		attributes.add(new AttributeBuilder().setName("emailPrivate").addValue(login + ".private@example.com").build());
		attributes.add(new AttributeBuilder().setName("urlFotky").addValue("https://example.com/photo/" + login + ".jpg").build());
	}
}
