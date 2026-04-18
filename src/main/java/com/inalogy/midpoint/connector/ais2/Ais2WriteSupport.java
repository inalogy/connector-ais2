package com.inalogy.midpoint.connector.ais2;

import ais.nastavosobinfo.NastavOsobInfoRequest;
import ais.nastavosobinfo.typy.LZIdentifKarta;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Ais2WriteSupport {

    static final String ULOZ_NAMESPACE = "http://ais/ulozZamestnanca/typy";
    static final String NASTAV_NAMESPACE = "http://ais/nastavOsobInfo/typy";
    static final String ATTRIBUTE_DELIMITER = ";";

    private static final String[] EMPLOYMENT_FIELDS = {
            "odDatumu",
            "doDatumu",
            "kodKategoria",
            "kodTypPPV",
            "osobneCislo",
            "skratkaOrganizacnaJednotka",
            "uvazok",
            "telefon"
    };

    private static final String[] CARD_FIELDS = {
            "cisloKarty",
            "kodTypIDCisla",
            "kodValidacia",
            "kodVizual",
            "platnostDo",
            "platnostOd",
            "prefix",
            "sufix"
    };

    private static final int EMPLOYMENT_REQUIRED_FIELDS = 7;
    private static final Map<String, Integer> EMPLOYMENT_INDEX = createIndex(EMPLOYMENT_FIELDS);
    private static final Map<String, Integer> CARD_INDEX = createIndex(CARD_FIELDS);

    private Ais2WriteSupport() {
    }

    static XMLGregorianCalendar parseXmlDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }

        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(normalized);
        } catch (Exception e) {
            throw new InvalidAttributeValueException("Error parsing date value '" + value + "': " + e.getMessage(), e);
        }
    }

    static <T> JAXBElement<T> createElement(String namespace, String elementName, Class<T> clazz, T value) {
        if (value == null) {
            return null;
        }
        return new JAXBElement<>(new QName(namespace, elementName), clazz, value);
    }

    static ais.ulozzamestnanca.typy.LZIdentifKarta createEmploymentIdentifKarta(Set<Attribute> attributes, String fallbackUoc) {
        String cisloKarty = fallbackUoc;
        String prefix = null;
        String sufix = null;

        for (String rawValue : getAttributeValues(attributes, "LZIdentifKarta")) {
            if (isXmlPayload(rawValue)) {
                continue;
            }

            String[] values = splitRecord(rawValue);
            String currentCisloKarty = valueAt(values, CARD_INDEX, "cisloKarty");
            String currentCardType = valueAt(values, CARD_INDEX, "kodTypIDCisla");
            if (currentCisloKarty == null) {
                continue;
            }

            if (cisloKarty == null || "UOC".equals(currentCardType)) {
                cisloKarty = currentCisloKarty;
                prefix = valueAt(values, CARD_INDEX, "prefix");
                sufix = valueAt(values, CARD_INDEX, "sufix");
                if ("UOC".equals(currentCardType)) {
                    break;
                }
            }
        }

        ais.ulozzamestnanca.typy.LZIdentifKarta identifKarta = new ais.ulozzamestnanca.typy.LZIdentifKarta();
        identifKarta.setCisloKarty(cisloKarty);
        identifKarta.setPrefix(createElement(ULOZ_NAMESPACE, "prefix", String.class, prefix));
        identifKarta.setSufix(createElement(ULOZ_NAMESPACE, "sufix", String.class, sufix));
        return identifKarta;
    }

    static ais.ulozzamestnanca.typy.LZOsoba.Zamestnanec createZamestnanec(Set<Attribute> attributes) {
        List<ais.ulozzamestnanca.typy.LZZamestnanec> relations = new ArrayList<>();

        for (String rawValue : getAttributeValues(attributes, "ulozZamestnanca", "LZZamestnanec")) {
            if (isXmlPayload(rawValue)) {
                continue;
            }

            String[] values = splitRecord(rawValue);
            requireMinFieldCount(values, EMPLOYMENT_REQUIRED_FIELDS, "LZZamestnanec", EMPLOYMENT_FIELDS);

            String odDatumu = valueAt(values, EMPLOYMENT_INDEX, "odDatumu");
            String kodTypPpv = valueAt(values, EMPLOYMENT_INDEX, "kodTypPPV");
            String skratkaOj = valueAt(values, EMPLOYMENT_INDEX, "skratkaOrganizacnaJednotka");
            String uvazok = valueAt(values, EMPLOYMENT_INDEX, "uvazok");

            requireField(odDatumu, "odDatumu", "LZZamestnanec");
            requireField(kodTypPpv, "kodTypPPV", "LZZamestnanec");
            requireField(skratkaOj, "skratkaOrganizacnaJednotka", "LZZamestnanec");
            requireField(uvazok, "uvazok", "LZZamestnanec");

            ais.ulozzamestnanca.typy.LZZamestnanec relation = new ais.ulozzamestnanca.typy.LZZamestnanec();
            relation.setOdDatumu(parseXmlDate(odDatumu));
            relation.setDoDatumu(createElement(ULOZ_NAMESPACE, "doDatumu", XMLGregorianCalendar.class,
                    parseXmlDate(valueAt(values, EMPLOYMENT_INDEX, "doDatumu"))));
            relation.setKodKategoria(createElement(ULOZ_NAMESPACE, "kodKategoria", Integer.class,
                    parseInteger(valueAt(values, EMPLOYMENT_INDEX, "kodKategoria"))));
            relation.setKodTypPPV(Integer.parseInt(kodTypPpv));
            relation.setOsobneCislo(createElement(ULOZ_NAMESPACE, "osobneCislo", String.class,
                    valueAt(values, EMPLOYMENT_INDEX, "osobneCislo")));
            relation.setSkratkaOrganizacnaJednotka(skratkaOj);
            relation.setUvazok(new BigDecimal(uvazok));
            relation.setTelefon(createElement(ULOZ_NAMESPACE, "telefon", String.class,
                    valueAt(values, EMPLOYMENT_INDEX, "telefon")));
            relations.add(relation);
        }

        if (relations.isEmpty()) {
            return null;
        }

        ais.ulozzamestnanca.typy.LZOsoba.Zamestnanec zamestnanec = new ais.ulozzamestnanca.typy.LZOsoba.Zamestnanec();
        zamestnanec.getLZZamestnanec().addAll(relations);
        return zamestnanec;
    }

    static NastavOsobInfoRequest createNastavOsobInfoRequest(Set<Attribute> attributes, int id, String login, String uoc) {
        List<ais.nastavosobinfo.typy.LZIdentifKarta> cards = new ArrayList<>();
        Set<String> cardTypes = new LinkedHashSet<>();
        String today = LocalDate.now().toString();

        for (String rawValue : getAttributeValues(attributes, "nastavOsobInfo", "LZIdentifKarta")) {
            if (isXmlPayload(rawValue)) {
                continue;
            }

            String[] values = splitRecord(rawValue);
            String cisloKarty = valueAt(values, CARD_INDEX, "cisloKarty");
            String kodTypIdCisla = valueAt(values, CARD_INDEX, "kodTypIDCisla");
            if (cisloKarty == null || kodTypIdCisla == null) {
                continue;
            }

            cards.add(createNastavCard(values, today));
            cardTypes.add(kodTypIdCisla);
        }

        if (!StringUtil.isBlank(uoc) && !cardTypes.contains("UOC")) {
            String[] uocValues = new String[CARD_FIELDS.length];
            uocValues[CARD_INDEX.get("cisloKarty")] = uoc;
            uocValues[CARD_INDEX.get("kodTypIDCisla")] = "UOC";
            uocValues[CARD_INDEX.get("platnostOd")] = today;
            cards.add(0, createNastavCard(uocValues, today));
            cardTypes.add("UOC");
        }

        List<String> initPasswdValues = getAttributeValues(attributes, "initPasswd");
        String initPasswd = initPasswdValues.isEmpty() ? null : initPasswdValues.get(0);
        List<String> emailValues = getAttributeValues(attributes, "email");
        String email = emailValues.isEmpty() ? null : emailValues.get(0);
        List<String> emailPrivateValues = getAttributeValues(attributes, "emailPrivate");
        String emailPrivate = emailPrivateValues.isEmpty() ? null : emailPrivateValues.get(0);
        List<String> liveIdValues = getAttributeValues(attributes, "liveID");
        String liveId = liveIdValues.isEmpty() ? null : liveIdValues.get(0);
        List<String> urlFotkyValues = getAttributeValues(attributes, "urlFotky");
        String urlFotky = urlFotkyValues.isEmpty() ? null : urlFotkyValues.get(0);

        if (cards.isEmpty()
                && StringUtil.isBlank(login)
                && StringUtil.isBlank(initPasswd)
                && StringUtil.isBlank(email)
                && StringUtil.isBlank(emailPrivate)
                && StringUtil.isBlank(liveId)
                && StringUtil.isBlank(urlFotky)) {
            return null;
        }

        NastavOsobInfoRequest request = new NastavOsobInfoRequest();
        ais.nastavosobinfo.typy.LZOsoba osoba = new ais.nastavosobinfo.typy.LZOsoba();
        osoba.setId(id);

        ais.nastavosobinfo.typy.SPPouzivatel pouzivatel = new ais.nastavosobinfo.typy.SPPouzivatel();
        pouzivatel.setLogin(login == null ? "" : login);
        pouzivatel.setInitPasswd(initPasswd == null ? "" : initPasswd);
        osoba.setPouzivatel(pouzivatel);
        osoba.setLiveID(liveId == null ? "" : liveId);
        osoba.setUrlFotky(urlFotky == null ? "" : urlFotky);
        osoba.setEmail(createElement(NASTAV_NAMESPACE, "email", String.class, email));
        osoba.setEmailPrivate(createElement(NASTAV_NAMESPACE, "emailPrivate", String.class, emailPrivate));

        if (!cards.isEmpty()) {
            ais.nastavosobinfo.typy.LZOsoba.IdentifKarta identifKarta = new ais.nastavosobinfo.typy.LZOsoba.IdentifKarta();
            identifKarta.getLZIdentifKarta().addAll(cards);
            osoba.setIdentifKarta(identifKarta);

            ais.nastavosobinfo.typy.LZOsoba.IdentifKartaTyp identifKartaTyp = new ais.nastavosobinfo.typy.LZOsoba.IdentifKartaTyp();
            for (String cardTypeValue : cardTypes) {
                ais.nastavosobinfo.typy.LZIdentifKartaTyp cardType = new ais.nastavosobinfo.typy.LZIdentifKartaTyp();
                cardType.setTypCislaKarty(cardTypeValue);
                identifKartaTyp.getLZIdentifKartaTyp().add(cardType);
            }
            osoba.setIdentifKartaTyp(identifKartaTyp);
        }

        request.getOsoby().add(osoba);
        return request;
    }

    static NastavOsobInfoRequest createVymazUocRequest(int id) {

        NastavOsobInfoRequest request = new NastavOsobInfoRequest();
        ais.nastavosobinfo.typy.LZOsoba osoba = new ais.nastavosobinfo.typy.LZOsoba();
        osoba.setId(id);

        ais.nastavosobinfo.typy.LZOsoba.IdentifKartaTyp identifKartaTyp = new ais.nastavosobinfo.typy.LZOsoba.IdentifKartaTyp();
        List<String> cardTypes = Arrays.asList("UOC", "PIK");
        for (String cardTypeValue : cardTypes) {
            ais.nastavosobinfo.typy.LZIdentifKartaTyp cardType = new ais.nastavosobinfo.typy.LZIdentifKartaTyp();
            cardType.setTypCislaKarty(cardTypeValue);
            identifKartaTyp.getLZIdentifKartaTyp().add(cardType);
        }
        osoba.setIdentifKartaTyp(identifKartaTyp);

        request.getOsoby().add(osoba);
        return request;
    }

    private static ais.nastavosobinfo.typy.LZIdentifKarta createNastavCard(String[] values, String defaultPlatnostOd) {
        String platnostOd = valueAt(values, CARD_INDEX, "platnostOd");

        ais.nastavosobinfo.typy.LZIdentifKarta card = new ais.nastavosobinfo.typy.LZIdentifKarta();
        card.setCisloKarty(valueAt(values, CARD_INDEX, "cisloKarty"));
        card.setKodTypIDCisla(valueAt(values, CARD_INDEX, "kodTypIDCisla"));
        card.setKodValidacia(valueAt(values, CARD_INDEX, "kodValidacia"));
        card.setKodVizual(valueAt(values, CARD_INDEX, "kodVizual"));
        card.setPlatnostDo(createElement(NASTAV_NAMESPACE, "platnostDo", XMLGregorianCalendar.class,
                parseXmlDate(valueAt(values, CARD_INDEX, "platnostDo"))));
        card.setPlatnostOd(parseXmlDate(platnostOd == null ? defaultPlatnostOd : platnostOd));
        card.setPrefix(createElement(NASTAV_NAMESPACE, "prefix", String.class, valueAt(values, CARD_INDEX, "prefix")));
        card.setSufix(createElement(NASTAV_NAMESPACE, "sufix", String.class, valueAt(values, CARD_INDEX, "sufix")));
        return card;
    }

    private static List<String> getAttributeValues(Set<Attribute> attributes, String... names) {
        Attribute attribute = findAttribute(attributes, names);
        if (attribute == null || attribute.getValue() == null) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (Object value : attribute.getValue()) {
            String normalized = normalize(value == null ? null : value.toString());
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String[] splitRecord(String rawValue) {
        return rawValue.split(java.util.regex.Pattern.quote(ATTRIBUTE_DELIMITER), -1);
    }

    private static Attribute findAttribute(Set<Attribute> attributes, String... names) {
        if (attributes == null || names == null || names.length == 0) {
            return null;
        }

        Set<String> allowedNames = new HashSet<>(Arrays.asList(names));
        for (Attribute attribute : attributes) {
            if (allowedNames.contains(attribute.getName())) {
                return attribute;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isXmlPayload(String rawValue) {
        return rawValue.startsWith("<");
    }

    private static String valueAt(String[] values, Map<String, Integer> index, String fieldName) {
        Integer position = index.get(fieldName);
        if (position == null) {
            throw new IllegalArgumentException("Unknown field '" + fieldName + "'");
        }
        return valueAt(values, position);
    }

    private static String valueAt(String[] values, Integer index) {
        return index < values.length ? normalize(values[index]) : null;
    }

    private static Map<String, Integer> createIndex(String[] fields) {
        Map<String, Integer> index = new java.util.LinkedHashMap<>();
        for (int i = 0; i < fields.length; i++) {
            index.put(fields[i], i);
        }
        return Collections.unmodifiableMap(index);
    }

    private static Integer parseInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }

    private static void requireField(String value, String fieldName, String recordName) {
        if (value == null) {
            throw new InvalidAttributeValueException(recordName + "." + fieldName + " is mandatory");
        }
    }

    private static void requireMinFieldCount(String[] values, int requiredFields, String recordName, String[] fieldOrder) {
        if (values.length >= requiredFields) {
            return;
        }

        throw new InvalidAttributeValueException(recordName + " value must contain at least " + requiredFields
                + " fields: " + String.join(ATTRIBUTE_DELIMITER, Arrays.copyOf(fieldOrder, requiredFields)));
    }
}
