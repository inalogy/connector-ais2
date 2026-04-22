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
				LOG.ok("returned: {0}", connectorObject);
				return true;
			}
		};

		Ais2Filter sf = new Ais2Filter();
//		sf.byId = "1441085"; //""1023736"; //GP
		sf.byId = "1449310";

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
//		sf.byInterval = new Interval(1422398, 1422398); //test


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
		String login = "test14";
		String uoc = "11111";

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue(uoc).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue(login).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("user").build());

		addWriteAttributes(attributes, uoc, login);

		LOG.ok("start creating");
		connector.create(objectClass, attributes, null);
		LOG.ok("end creating");
	}

	@Test
	public void testUpdateUser() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);
		String uidValue = "1449310";
		String login = "test1773055314577";
		String uoc = null; //"490";

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
		if (uoc!=null)
			attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue(uoc).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue("test3").build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("UserV2").build());

		addWriteAttributes(attributes, uoc, login);

		LOG.ok("start updating");
		connector.update(objectClass, new Uid(uidValue), attributes, null);
		LOG.ok("end updating");
	}

	private void addWriteAttributes(Set<Attribute> attributes, String uoc, String login) {
		String delimiter = Ais2Connector.ATTRIBUTE_DELIMITER;
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_ULOZ_ZAMESTNANCA)
				.addValue(String.join(delimiter, "2025-01-12", "2027-12-31", "27", "4", "101100", "JLF.Dek", "1.00", "0901000000"))
				.addValue(String.join(delimiter, "2025-02-01", "", "27", "4", "10110451", "LF.Dek", "0.25", "0901000002"))
				.build());

		AttributeBuilder lzIdentifKarta = new AttributeBuilder().setName(Ais2Connector.ATTR_NASTAV_OSOB_INFO);
		if (uoc!=null){
			lzIdentifKarta.addValue(String.join(delimiter, uoc, "UOC", "", "", "", "2026-01-01", "", ""));
		}
		lzIdentifKarta.addValue(String.join(delimiter, "490959", "PIK", "", "", "", "2025-01-01", "", ""));
		attributes.add(lzIdentifKarta.build());

		attributes.add(new AttributeBuilder().setName("initPasswd").addValue("3899fda10b0a9b19ce3ceebc1924b175a1ebb000").build());
		attributes.add(new AttributeBuilder().setName("liveID").addValue(login + ".live.id").build());
		attributes.add(new AttributeBuilder().setName("email").addValue(login + "@example.com").build());
		attributes.add(new AttributeBuilder().setName("emailPrivate").addValue(login + ".private@example.com").build());
		attributes.add(new AttributeBuilder().setName("urlFotky").addValue("https://example.com/photo/" + login + ".jpg").build());
	}

	@Test
	public void testAssignUoc2User() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);
		String uidValue = "1449320";
		String login = "test15";
		String uoc = "newUoc15";
		// assign UOC to user who has no UOC

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
		if (uoc!=null)
			attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue(uoc).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue(login).build());
		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("User").build());

		addUocAttribute(attributes, uoc);

		LOG.ok("start updating");
		connector.update(objectClass, new Uid(uidValue), attributes, null);
		LOG.ok("end updating");
	}

	private void addUocAttribute(Set<Attribute> attributes, String uoc) {
		String delimiter = Ais2Connector.ATTRIBUTE_DELIMITER;
		AttributeBuilder lzIdentifKarta = new AttributeBuilder().setName(Ais2Connector.ATTR_NASTAV_OSOB_INFO);
		if (uoc!=null){
			lzIdentifKarta.addValue(String.join(delimiter, uoc, "UOC", "", "", "", "", "", ""));
//			lzIdentifKarta.addValue(String.join(delimiter, uoc, "UOC", "", "", "", "2026-01-01", "", ""));
			attributes.add(lzIdentifKarta.build());
		}
	}

	@Test
	public void testUpdateUoc2User() {
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);
		String uidValue = "1449320";
		String login = "test15";
		String uoc = "newerUoc15";
		// fix UOC to user who has old UOC

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
//		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());
//		if (uoc!=null)
//			attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_UOC).addValue(uoc).build());
//		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_MENO).addValue(login).build());
//		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_PRIEZVISKO).addValue("User").build());

		addUocAttribute(attributes, uoc);

		LOG.ok("start updating");
		connector.update(objectClass, new Uid(uidValue), attributes, null);
		LOG.ok("end updating");
	}

	@Test
	public void testCreateUocOverUpdateUser() {
		// updateOp and set first time UOC
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

		// TRACE (org.identityconnectors.framework.api.operations.UpdateApiOp): method: addAttributeValues msg:instance='AIS2 zamestnanec' Enter: addAttributeValues(Object
		//Class: osoba, Attribute: {Name=__UID__, Value=[2128], NameHint=Attribute: {Name=__NAME__, Value=[tomas.csank]}},
		// [Attribute: {Name=ulozZamestnanca, Value=[2011-10-01;;67;1;1593;KaMBaI;1.00;]},
		// Attribute: {Name=nastavOsobInfo, Value=[1593;UOC;;;;;;]}], OperationOptions: {})

		// received in connector
		// [Attribute: {Name=ulozZamestnanca, Value=[2011-10-01;2020-08-31;;1;1593;ÚMBaGB;1.00;, 2020-09-01;2022-08-31;;1;1593;KaMBaI;1.00;, 2022-09-01;;;1;1593;KaMBaI;1.00;, 2011-10-01;;67;1;1593;KaMBaI;1.00;]}, Attribute: {Name=nastavOsobInfo, Value=[1593;UOC;;;;;;]}]

		String uidValue = "1422398"; // ""2128";
		String login = "test0";

		ResultsHandler rh = new ResultsHandler() {
			@Override
			public boolean handle(ConnectorObject connectorObject) {
				System.out.println(connectorObject);
				return true;
			}
		};

		Set<Attribute> attributes = new HashSet<>();
		attributes.add(new AttributeBuilder().setName(Name.NAME).addValue(login).build());

		attributes.add(new AttributeBuilder().setName(Ais2Connector.ATTR_ULOZ_ZAMESTNANCA)
//				.addValue("2011-10-01;;67;1;1593;KaMBaI;1.00;")
//				.addValue("2011-10-01;;67;1;1593;UK;1.00;")
				.addValue("2021-10-01;;67;1;15930;FMFI;1.00;")
				.build());

//		AttributeBuilder lzIdentifKarta = new AttributeBuilder().setName(Ais2Connector.ATTR_NASTAV_OSOB_INFO);
//		lzIdentifKarta.addValue("1593;UOC;;;;;;");
//		attributes.add(lzIdentifKarta.build());

		LOG.ok("start updating");
		connector.update(objectClass, new Uid(uidValue), attributes, null);
		LOG.ok("end updating");
	}

	@Test
	public void testCreateUocOverUpdateDeltaUser() {
		// updateDeltaOp and set first time UOC - delta-style version of testCreateUocOverUpdateUser
		ObjectClass objectClass = new ObjectClass(Ais2Connector.OBJECT_CLASS_OSOBA);

		String uidValue = "1422398"; // ""2128";
		String login = "test0";

		Set<AttributeDelta> modifications = new HashSet<>();
		modifications.add(AttributeDeltaBuilder.build(Name.NAME, login));

		modifications.add(new AttributeDeltaBuilder()
				.setName(Ais2Connector.ATTR_ULOZ_ZAMESTNANCA)
//				.addValueToAdd("2021-10-01;;67;1;15930;FMFI;1.00;")
				.addValueToAdd("2011-10-01;;67;1;1593;UK;1.00;")
				.build());

//		modifications.add(new AttributeDeltaBuilder()
//				.setName(Ais2Connector.ATTR_NASTAV_OSOB_INFO)
//				.addValueToReplace("1593;UOC;;;;;;")
//				.build());

		LOG.ok("start updating via delta");
		Set<AttributeDelta> sideEffects = connector.updateDelta(objectClass, new Uid(uidValue), modifications, null);
		LOG.ok("end updating via delta, sideEffects: {0}", sideEffects);
	}
}
