package io.artin.idm.connector.ais2;

import ais.vratosoby.VratOsoby;
import ais.vratosoby.VratOsobyRequest;
import ais.vratosoby.VratOsobyResponse;
import ais.vratosoby.reqtypy.FilterType;
import ais.vratosoby.typy.*;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.ofNullable;

/**
 * AIS2 Connector.
 */
@ConnectorClass(displayNameKey = "ais2.connector.display", configurationClass = Ais2Configuration.class)
public class Ais2Connector implements PoolableConnector, TestOp, SchemaOp, SearchOp<Ais2Filter> {

    private static final Log LOG = Log.getLog(Ais2Connector.class);

	private static final String REQUEST_LOG_FILENAME = "ais2-soap-requests.xml";

	private static final String RESPONSE_LOG_FILENAME = "ais2-soap-responses.xml";

    public static final String OBJECT_CLASS_OSOBA = "osoba";
    public static final String ATTR_AIS_ID = "aisId";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_UOC = "uoc";
    private static final String ATTR_AKTIVNY_PIK = "pik";

    private static final String ATTR_DATA = "data";
    private static final String ATTR_MOST_RECENT_ACADEMIC_YEAR = "mostRecentAcademicYear";

    private static final String ATTR_MENO = "meno";
    private static final String ATTR_PRIEZVISKO = "priezvisko";
    private static final String ATTR_POV_PRIEZVISKO = "povPriezvisko";
    private static final String ATTR_PLNE_MENO = "plneMeno";
    private static final String ATTR_COP = "cop";
    private static final String ATTR_CISLO_PASU = "cisloPasu";
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_EMAIL_PRIVATE = "emailPrivate";
    private static final String ATTR_LIVE_ID = "liveID";
    private static final String ATTR_RODNE_CISLO = "rodneCislo";
    private static final String ATTR_DATUM_NARODENIA = "datumNarodenia";
    private static final String ATTR_URL_FOTKY = "urlFotky";
    private static final String ATTR_TELEFON = "telefon";
    private static final String ATTR_KOD_NARODNOST = "kodNarodnost";
    private static final String ATTR_KOD_POHLAVIE = "kodPohlavie";
    private static final String ATTR_KOD_RODINNY_STAV = "kodRodinnyStav";
    private static final String ATTR_KOD_STAT = "kodStat";
    private static final String ATTR_SKRATKA_AKADEMICKY_TITUL = "skratkaAkademickyTitul";
    private static final String ATTR_SKRATKA_CESTNY_TITUL = "skratkaCestnyTitul";
    private static final String ATTR_SKRATKA_HODNOST = "skratkaHodnost";
    private static final String ATTR_SKRATKA_VED_PEG_HODNOST = "skratkaVedPegHodnost";
    private static final String ATTR_SKRATKA_VEDECKA_HODNOST = "skratkaVedeckaHodnost";
    private static final String ATTR_KOD_TYP_VZDELANIA = "kodTypVzdelania";
    private static final String ATTR_INIT_PASSWD = "initPasswd";
    private static final String ATTR_MIESTO_NARODENIA = "miestoNarodenia";
    private static final String ATTR_PAD_SKLONOVANIE_NAR = "padSklonovanieNar";

    private static final String ATTR_TB_ULICA = "tbUlica";
    private static final String ATTR_TB_ORIENTACNE_CISLO = "tbOrientacneCislo";
    private static final String ATTR_TB_OBEC = "tbObec";
    private static final String ATTR_TB_PSC = "tbPsc";
    private static final String ATTR_TB_STAT = "tbStat";
    private static final String ATTR_PB_ULICA = "pbUlica";
    private static final String ATTR_PB_ORIENTACNE_CISLO = "pbOrientacneCislo";
    private static final String ATTR_PB_OBEC = "pbObec";
    private static final String ATTR_PB_PSC = "pbPsc";
    private static final String ATTR_PB_STAT = "pbStat";

    private static final List<String> ATTRIBUTES_FOR_REMOVAL = Arrays.asList(
            "/vratOsobyResponse/id",
            "/vratOsobyResponse/meno",
            "/vratOsobyResponse/priezvisko",
            "/vratOsobyResponse/povPriezvisko",
            "/vratOsobyResponse/plneMeno",
            "/vratOsobyResponse/cop",
            "/vratOsobyResponse/cisloPasu",
            "/vratOsobyResponse/email",
            "/vratOsobyResponse/emailPrivate",
            "/vratOsobyResponse/liveID",
            "/vratOsobyResponse/rodneCislo",
            "/vratOsobyResponse/datumNarodenia",
            "/vratOsobyResponse/urlFotky",
            "/vratOsobyResponse/telefon",
            "/vratOsobyResponse/kodNarodnost",
            "/vratOsobyResponse/kodPohlavie",
            "/vratOsobyResponse/kodRodinnyStav",
            "/vratOsobyResponse/kodStat",
            "/vratOsobyResponse/skratkaAkademickyTitul",
            "/vratOsobyResponse/skratkaCestnyTitul",
            "/vratOsobyResponse/skratkaHodnost",
            "/vratOsobyResponse/skratkaVedPegHodnost",
            "/vratOsobyResponse/skratkaVedeckaHodnost",
            "/vratOsobyResponse/kodTypVzdelania",
            "/vratOsobyResponse/pouzivatel/login",
            "/vratOsobyResponse/pouzivatel/initPasswd",
            "/vratOsobyResponse/sklonovanieNar");

    private static final ThreadLocal<DocumentBuilder> DOCUMENT_BUILDER_THREAD_LOCAL;
    private static final ThreadLocal<XPathFactory> XPATH_FACTORY_THREAD_LOCAL;
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY_THREAD_LOCAL;

    static {
        DOCUMENT_BUILDER_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setIgnoringElementContentWhitespace(true);
                return documentBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
        XPATH_FACTORY_THREAD_LOCAL = ThreadLocal.withInitial(XPathFactory::newInstance);
        TRANSFORMER_FACTORY_THREAD_LOCAL = ThreadLocal.withInitial(TransformerFactory::newInstance);
    }

    private Ais2Configuration configuration;

    private VratOsoby vratOsobyService;

    /**
     * flag indicating that the configuration has changed since the last
     * call of test()
     */
    private final AtomicBoolean configurationChanged = new AtomicBoolean();

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        LOG.info("Initializing connector (connecting to the WS)...");

        this.configuration = (Ais2Configuration) configuration;
        this.configuration.addObservingConnector(this);

        initByConfiguration();
    }

    @Override
    public void dispose() {
		LOG.info("Disposing connector...");

        closeWsClient(vratOsobyService);
        vratOsobyService = null;

		configuration.removeObservingConnector(this);
        configuration = null;
    }

    /**
     * Called by the configuration object when any of its attributes changes.
     */
    void configurationChanged() {
        LOG.info("configurationChanged()");
        configurationChanged.set(true);
    }

    private void reinitByConfiguration() {
        LOG.info("The connector is being re-initialized.");
        closeWsClient(vratOsobyService);

        initByConfiguration();
    }

    private void initByConfiguration() {
        vratOsobyService = createService(VratOsoby.class);
    }

	@Override
	public void checkAlive() {
	}

	@Override
	public void test() {
		LOG.info("test() ...");
		configuration.validate();
        if (configurationChanged.compareAndSet(true, false)) {
            LOG.ok(" - the configuration has changed - the connector will be reinitialized before the actual test");
            reinitByConfiguration();
        }

        VratOsobyRequest vratOsobyRequest = createVratOsobyRequest(configuration.getFilterOdId(), configuration.getFilterOdId());

        VratOsobyResponse vratOsobyResponse = vratOsobyService.vratOsoby(vratOsobyRequest);
        if (vratOsobyResponse != null && vratOsobyResponse.getOsoby().size() > 0) {
            LOG.ok("found first Osoba with Id: " + vratOsobyResponse.getOsoby().get(0).getId());
        }

		LOG.info("test() finished successfully");
	}

    private VratOsobyRequest createVratOsobyRequest(Integer filterOdId, Integer filterDoId) {
        FilterType filter = new FilterType();
        filter.setOdId(filterOdId);
        filter.setDoId(filterDoId);

        VratOsobyRequest vratOsobyRequest = new VratOsobyRequest();
        vratOsobyRequest.setFilter(filter);

        return vratOsobyRequest;
    }

    @Override
	public Schema schema() {
		LOG.info("schema()");

        SchemaBuilder schemaBuilder = new SchemaBuilder(Ais2Connector.class);
        buildOsobaClass(schemaBuilder);
		return schemaBuilder.build();
	}

    private void buildOsobaClass(SchemaBuilder schemaBuilder) {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();

        objClassBuilder.setType(OBJECT_CLASS_OSOBA);

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_AIS_ID, Integer.class)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_LOGIN)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_UOC)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_AKTIVNY_PIK)
                        .setUpdateable(true)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_DATA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_MOST_RECENT_ACADEMIC_YEAR)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_MENO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PRIEZVISKO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_POV_PRIEZVISKO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PLNE_MENO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_COP)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_CISLO_PASU)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_EMAIL)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_EMAIL_PRIVATE)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_LIVE_ID)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_RODNE_CISLO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_DATUM_NARODENIA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_URL_FOTKY)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TELEFON)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_KOD_NARODNOST)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_KOD_POHLAVIE)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_KOD_RODINNY_STAV)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_KOD_STAT)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_SKRATKA_AKADEMICKY_TITUL)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_SKRATKA_CESTNY_TITUL)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_SKRATKA_HODNOST)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_SKRATKA_VED_PEG_HODNOST)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_SKRATKA_VEDECKA_HODNOST)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_KOD_TYP_VZDELANIA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_INIT_PASSWD)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_MIESTO_NARODENIA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PAD_SKLONOVANIE_NAR)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TB_ULICA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TB_ORIENTACNE_CISLO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TB_OBEC)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TB_PSC)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_TB_STAT)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PB_ULICA)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PB_ORIENTACNE_CISLO)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PB_OBEC)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PB_PSC)
                        .setUpdateable(false)
                        .build());

        objClassBuilder.addAttributeInfo(
                new AttributeInfoBuilder(ATTR_PB_STAT)
                        .setUpdateable(false)
                        .build());

        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(), SearchOp.class);

        schemaBuilder.defineObjectClass(objClassBuilder.build());
    }

    @Override
    public FilterTranslator<Ais2Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        return new AisFilterTranslator();
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Ais2Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQuery on {0}, query: {1}, options: {2}", objectClass, query, options);
        if (objectClass.is(OBJECT_CLASS_OSOBA)) {
            if (query != null && query.byId != null) {
                executeQueryById(Integer.parseInt(query.byId), handler);
            } else {
                Interval interval = getIntervalFromConfigurationOrQuery(query);
                executeQueryByInterval(interval, handler, options);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }
    }

    private Interval getIntervalFromConfigurationOrQuery(Ais2Filter query) {
        if (query == null) {
            return new Interval(configuration.getFilterOdId(), configuration.getFilterDoId());
        }
        else if (query.byInterval != null)
            return query.byInterval;

        throw new UnsupportedOperationException("FIXME");
    }

    private void executeQueryById(int byId, ResultsHandler handler) {
        LOG.info("Osoba searching for id: {0}", byId);
        try {
            if (executeIntervalQuery(byId, byId, handler) == 0) {
                throw new UnknownUidException("Person with ID " + byId + " was not found");
            }
        } catch (SearchStopException e) {
            // Just ignore. The object was already handled.
        }
    }

    // May be paged or not
    private void executeQueryByInterval(Interval interval, ResultsHandler handler, OperationOptions options) {

        LOG.info("Osoba searching within interval of {0}", interval);

        int globalFrom;
        int globalTo;
        int pageSize;

        if (options != null && options.getPagedResultsOffset() != null && options.getPageSize() != null) {
            LOG.info("Paging specified: offset {0}, pageSize: {1} ", options.getPagedResultsOffset(), options.getPageSize());

            globalFrom = interval.from + options.getPagedResultsOffset() - 1;
            globalTo = globalFrom + options.getPageSize() - 1;
            pageSize = options.getPageSize();
        } else {
            globalFrom = interval.from;
            globalTo = interval.to;
            pageSize = configuration.getEffectivePageSize();
        }

        // iterating over globalFrom - globalTo interval
        int currentFrom = globalFrom;

        while (currentFrom <= globalTo) {
            int remainingPersons = globalTo - currentFrom + 1;
            int currentTo = remainingPersons > pageSize ?
                    currentFrom + pageSize - 1 : globalTo;

            try {
                executeIntervalQuery(currentFrom, currentTo, handler);
            } catch (SearchStopException e) {
                return;
            }

            currentFrom = currentTo + 1;
        }
    }

    private int executeIntervalQuery(int filterOdId, int filterDoId, ResultsHandler handler) throws SearchStopException {
        LOG.info("Osoba process data from {0} to {1}", filterOdId, filterDoId);
        VratOsobyRequest vratOsobyRequest = createVratOsobyRequest(filterOdId, filterDoId);

        int count = 0;
        VratOsobyResponse vratOsobyResponse = vratOsobyService.vratOsoby(vratOsobyRequest);
        if (vratOsobyResponse != null && vratOsobyResponse.getOsoby() != null) {
            for (LZOsoba osoba : vratOsobyResponse.getOsoby()) {
                count++;
                if (!handler.handle(
                        convertOsobaToConnectorObject(osoba))) {
                    throw new SearchStopException();
                }
            }
        }
        return count;
    }

    /** Search was stopped by the client. */
    private static class SearchStopException extends Exception {
    }

    private ConnectorObject convertOsobaToConnectorObject(LZOsoba osoba) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        String idAsString = String.valueOf(osoba.getId());

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setUid(new Uid(idAsString));
        builder.setName(
                StringUtil.isBlank(osoba.getPouzivatel().getLogin()) ?
                        idAsString : osoba.getPouzivatel().getLogin());

        builder.addAttribute(ATTR_AIS_ID, osoba.getId());  // Integer attribute

        addAttr(builder, ATTR_UOC, getUoc(osoba.getIdentifKarta()));
        addAttr(builder, ATTR_AKTIVNY_PIK, getAktivnyPik(osoba.getIdentifKarta()));

        addAttr(builder, ATTR_MENO, osoba.getMeno());
        addAttr(builder, ATTR_PRIEZVISKO, osoba.getPriezvisko());
        addAttr(builder, ATTR_POV_PRIEZVISKO, osoba.getPovPriezvisko());
        addAttr(builder, ATTR_PLNE_MENO, osoba.getPlneMeno());
        addAttr(builder, ATTR_COP, osoba.getCop());
        addAttr(builder, ATTR_CISLO_PASU, osoba.getCisloPasu());
        addAttr(builder, ATTR_EMAIL, osoba.getEmail());
        addAttr(builder, ATTR_EMAIL_PRIVATE, osoba.getEmailPrivate());
        addAttr(builder, ATTR_LIVE_ID, osoba.getLiveID());
        addAttr(builder, ATTR_RODNE_CISLO, osoba.getRodneCislo());
        addAttr(builder, ATTR_DATUM_NARODENIA,
                osoba.getDatumNarodenia() != null ? sdf.format(osoba.getDatumNarodenia().toGregorianCalendar().getTime()) : null);
        addAttr(builder, ATTR_URL_FOTKY, osoba.getUrlFotky());
        addAttr(builder, ATTR_TELEFON, osoba.getTelefon());
        addAttr(builder, ATTR_KOD_NARODNOST, osoba.getKodNarodnost());
        addAttr(builder, ATTR_KOD_POHLAVIE, osoba.getKodPohlavie());
        addAttr(builder, ATTR_KOD_RODINNY_STAV, osoba.getKodRodinnyStav());
        addAttr(builder, ATTR_KOD_STAT, osoba.getKodStat());
        addAttr(builder, ATTR_SKRATKA_AKADEMICKY_TITUL, osoba.getSkratkaAkademickyTitul());
        addAttr(builder, ATTR_SKRATKA_CESTNY_TITUL, osoba.getSkratkaCestnyTitul());
        addAttr(builder, ATTR_SKRATKA_HODNOST, osoba.getSkratkaHodnost());
        addAttr(builder, ATTR_SKRATKA_VED_PEG_HODNOST, osoba.getSkratkaVedPegHodnost());
        addAttr(builder, ATTR_SKRATKA_VEDECKA_HODNOST, osoba.getSkratkaVedeckaHodnost());
        addAttr(builder, ATTR_KOD_TYP_VZDELANIA, osoba.getKodTypVzdelania());
        addAttr(builder, ATTR_LOGIN, osoba.getPouzivatel().getLogin());
        addAttr(builder, ATTR_INIT_PASSWD, osoba.getPouzivatel().getInitPasswd());
        if (osoba.getSklonovanieNar() != null &&  osoba.getSklonovanieNar().getLZSklonMNarod() != null && osoba.getSklonovanieNar().getLZSklonMNarod().size()>0) {
            addAttr(builder, ATTR_MIESTO_NARODENIA, osoba.getSklonovanieNar().getLZSklonMNarod().get(0).getMiestoNarodenia());
            addAttr(builder, ATTR_PAD_SKLONOVANIE_NAR, osoba.getSklonovanieNar().getLZSklonMNarod().get(0).getPadSklonovanieNar());
        } else {
            addAttr(builder, ATTR_MIESTO_NARODENIA, null);
            addAttr(builder, ATTR_PAD_SKLONOVANIE_NAR, null);
        }

        parseAdresa(osoba, builder);

        // Change whole account into a xml string /throws exception while importing
        StringWriter sw = new StringWriter();
        JAXBContext jc;
        Marshaller marshaller;
        try {
            jc = JAXBContext.newInstance(osoba.getClass());

            marshaller = jc.createMarshaller();
            //noinspection unchecked,rawtypes
            marshaller.marshal(
                    new JAXBElement(
                            new QName("vratOsobyResponse"), osoba.getClass(), osoba), sw);

            // Finds out most recent academic year
            addAttr(builder, ATTR_MOST_RECENT_ACADEMIC_YEAR, getMostRecentAcademicYear(osoba.getStudium()));

            String xmlDataString = sw.toString();
            try {
                Document document = DOCUMENT_BUILDER_THREAD_LOCAL.get()
                        .parse(new InputSource((new StringReader(xmlDataString))));
                addAttr(builder, ATTR_DATA, removeUnnecessaryAttributes(document, configuration.keepFullXml()));
            } catch (Exception e) {
                throw new InvalidAttributeValueException("Unknown error while parsing xml file: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            // TODO throw more specific/better exception (?)
            throw new InvalidAttributeValueException("Error converting account to connector object: " + e.getMessage(), e);
        }
        return builder.build();
    }

    private String getUoc(LZOsoba.IdentifKarta identifKarta) {
        if (identifKarta != null && identifKarta.getLZIdentifKarta() != null) {
            for (LZIdentifKarta identifKart : identifKarta.getLZIdentifKarta()){
                if ("UOC".equals(identifKart.getKodTypIDCisla()))
                    return identifKart.getCisloKarty();
            }
        }

        return null;
    }

    private String getAktivnyPik(LZOsoba.IdentifKarta identifKarta) {
        if (identifKarta != null && identifKarta.getLZIdentifKarta() != null) {
            for (LZIdentifKarta identifKart : identifKarta.getLZIdentifKarta()){
                if ("PIK".equals(identifKart.getKodTypIDCisla()) && "A".equals(identifKart.getKodValidacia()))
                    return identifKart.getCisloKarty();
            }
        }

        return null;
    }

    private void parseAdresa(LZOsoba osoba, ConnectorObjectBuilder builder) {
        if (osoba.getAdresaOsoby() == null || osoba.getAdresaOsoby().getLZAdresaOsoby() == null)
            return;

        for (LZAdresaOsoby adresaOsoby : osoba.getAdresaOsoby().getLZAdresaOsoby()) {
            if ("T".equals(adresaOsoby.getKodTypAdresy())) {
                addAttr(builder, ATTR_TB_ULICA, adresaOsoby.getUlica());
                addAttr(builder, ATTR_TB_ORIENTACNE_CISLO, adresaOsoby.getOrientacneCislo());

                if (adresaOsoby.getKodStat() == null || "703".equals(adresaOsoby.getKodStat())) {
                    if (adresaOsoby.getObec() == null || adresaOsoby.getObec().getPopis() == null)
                        addAttr(builder, ATTR_TB_OBEC, adresaOsoby.getPosta());
                    else
                        addAttr(builder, ATTR_TB_OBEC, adresaOsoby.getObec().getPopis());
                }
                else {
                    //zahranicny
                    addAttr(builder, ATTR_TB_OBEC, adresaOsoby.getPosta());
                }

                if (adresaOsoby.getPSC() == null && adresaOsoby.getObec() != null)
                    addAttr(builder, ATTR_TB_PSC, adresaOsoby.getObec().getPsc());
                else
                    addAttr(builder, ATTR_TB_PSC, adresaOsoby.getPSC());

                addAttr(builder, ATTR_TB_STAT, adresaOsoby.getKodStat());
            }
            else if ("P".equals(adresaOsoby.getKodTypAdresy())) {
                addAttr(builder, ATTR_PB_ULICA, adresaOsoby.getUlica());
                addAttr(builder, ATTR_PB_ORIENTACNE_CISLO, adresaOsoby.getOrientacneCislo());

                if (adresaOsoby.getKodStat() == null || "703".equals(adresaOsoby.getKodStat())) {
                    if (adresaOsoby.getObec() == null || adresaOsoby.getObec().getPopis() == null)
                        addAttr(builder, ATTR_PB_OBEC, adresaOsoby.getPosta());
                    else
                        addAttr(builder, ATTR_PB_OBEC, adresaOsoby.getObec().getPopis());
                }
                else {
                    //zahranicny
                    addAttr(builder, ATTR_PB_OBEC, adresaOsoby.getPosta());
                }

                if (adresaOsoby.getPSC() == null && adresaOsoby.getObec() != null)
                    addAttr(builder, ATTR_PB_PSC, adresaOsoby.getObec().getPsc());
                else
                    addAttr(builder, ATTR_PB_PSC, adresaOsoby.getPSC());

                addAttr(builder, ATTR_PB_STAT, adresaOsoby.getKodStat());
            }
        }
    }

    public static String getMostRecentAcademicYear(LZOsoba.Studium studia) {
        String ret = "";
        int highest = 0;
        if (studia.getESStudium() != null) {
            for (ESStudium studium : studia.getESStudium()) {
                if (studium.getZapisnyList() != null) {
                    for (ESZapisnyList zapisnyList : studium.getZapisnyList().getESZapisnyList()) {
                        // Taking 2017 from 2016/2017
                        try {
                            int current = Integer.parseInt(
                                    zapisnyList.getPopisAkadRok().substring(zapisnyList.getPopisAkadRok().indexOf('/') + 1));
                            if (current > highest) {
                                highest = current;
                                ret = zapisnyList.getPopisAkadRok();
                            }
                        } catch (NumberFormatException e) {
                            throw new InvalidAttributeValueException(
                                    "Error parsing school year number '" + zapisnyList.getPopisAkadRok() + "': " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /** These attributes are in the "parsed data" anyway, so let's spare the space and remove them from the full version. */
    public static String removeUnnecessaryAttributes(Document doc, Boolean keepFullXml)
            throws Exception {
        XPath xPath = XPATH_FACTORY_THREAD_LOCAL.get().newXPath();

        if (!keepFullXml){
            for (String attributeForRemoval : ATTRIBUTES_FOR_REMOVAL) {
                NodeList nodes = (NodeList) xPath.compile(attributeForRemoval).evaluate(doc, XPathConstants.NODESET);
                for (int i = nodes.getLength() - 1; i >= 0; i--) {
                    Node node = nodes.item(i);
                    node.getParentNode().removeChild(node);
                }
            }
        }

        Transformer transformer;
        try {
            transformer = TRANSFORMER_FACTORY_THREAD_LOCAL.get().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (Exception e) {
            throw new Exception("Error transforming data to xml: " + e.getMessage(), e);
        }
    }

    private void addAttr(ConnectorObjectBuilder builder, String attributeName, String data) {
        if (data != null && !data.isEmpty()) {
            builder.addAttribute(attributeName, data);
        }
    }

    private static void closeWsClient(Object service) {
        if (service != null) {
            Client client = ClientProxy.getClient(service);
            if (client != null) {
                client.destroy();
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <S> S createService(Class<S> seiClass) {
        ClientProxyFactoryBean factory = new JaxWsProxyFactoryBean(); // a new instance must be used for each service

        Path soapLogTargetPath = configuration.getSoapLogTargetPath();
        if (soapLogTargetPath != null) {
            try {
                final Path targetForRequests  = soapLogTargetPath.resolve(REQUEST_LOG_FILENAME);
                final Path targetForResponses = soapLogTargetPath.resolve(RESPONSE_LOG_FILENAME);
                final URL targetPathURLForRequests  = targetForRequests.toUri().toURL();
                final URL targetPathURLForResponses = targetForResponses.toUri().toURL();
                factory.getFeatures().add(new LoggingFeature(targetPathURLForResponses.toString(),
                        targetPathURLForRequests.toString(),
                        100_000,
                        true));
            } catch (MalformedURLException ex) {
                LOG.warn(ex, "Couldn't initialize logging of SOAP messages.");
            }
        }
        List<String> passwordList = new ArrayList<>(1);
        GuardedString guardedPassword = configuration.getPassword();
        if (guardedPassword != null) {
            guardedPassword.access(chars -> passwordList.add(new String(chars)));
        }
        String password = null;
        if (!passwordList.isEmpty()) {
            password = passwordList.get(0);
        }

        factory.setAddress(configuration.getWsUrl(seiClass));
        if (configuration.getUsername() != null && !"".equals(configuration.getUsername())) {
            factory.setUsername(configuration.getUsername());
            factory.setPassword(password);
        }
        factory.setServiceClass(seiClass);
        //noinspection unchecked
        final S result = (S) factory.create();

        /* disable chunking: */
        final Client client = ClientProxy.getClient(result);
        final HTTPConduit http = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy httpClientPolicy;
        {
            httpClientPolicy = new HTTPClientPolicy();
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setConnectionTimeout(configuration.getConnectTimeout());
            httpClientPolicy.setReceiveTimeout(configuration.getReceiveTimeout());

            if (configuration.tamperSsl()) {
                // HTTPS settings - you can override jre trust settings here
                final TLSClientParameters tlsParameters = ofNullable(http.getTlsClientParameters()).orElse(new TLSClientParameters());
                {
                    tlsParameters.setKeyManagers(new KeyManager[0]);
                    tlsParameters.setTrustManagers(new TrustManager[] { new Ais2Configuration.NonValidatingTM() });
                    tlsParameters.setDisableCNCheck(Boolean.parseBoolean(configuration.getSslDisableCnCheck()));
                }
                http.setTlsClientParameters(tlsParameters);
            }
        }
        http.setClient(httpClientPolicy);

        return result;
    }

}
