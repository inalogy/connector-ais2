## AIS2 Connector
## Capabilities and Features
* Schema: YES
* Provisioning: No
* Live Synchronization: No
* Password: No
* Activation: No
* Script execution: No
  
  AIS2 Connector contains support for Osoba entities using SOAP API vratOsoby created for UK.
  
## Build
Generate WS classes: 

```mvn cxf-codegen:wsdl2java -X -f pom.xml```

[Download](https://github.com/inalogy/connector-ais2) and build the project:
Build without tests: 

```mvn clean install```

For running also unit tests set maven.test.skip=false in pom.xml and update test.properties content (required enabled and accessible AIS2 SOAP API for UK).

## Config
HTTPS certifikate for SOAP API calls for unit tests is stored in trust-store.jks.
  For creating new trust store, save new cert to ais2.pem and run

  ```keytool -import -alias ca -keystore trust-store.jks -storepass TrustStorePassword -trustcacerts -file ais2.pem```

If connector is running in midpoint, you need to import certificate to midpoint keystore.jceks and set

```-Djavax.net.ssl.trustStore=/opt/midpoint/var/keystore.jceks```
```-Djavax.net.ssl.trustStoreType=jceks```

For test purposes is also possible to set property midpointTrustAllCerts=true.

## License
Licensed under the [Apache License 2.0](/LICENSE).

## Status
[AIS2](https://www.ais2.sk) Connector is intended for production use. Tested with MidPoint version 4.7. The connector was introduced as a contribution to midPoint project by [Inalogy](https://www.inalogy.com) and is not officially supported by Evolveum.
If you need support, please contact info@inalogy.com.
