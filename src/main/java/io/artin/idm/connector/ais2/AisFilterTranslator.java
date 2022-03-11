package io.artin.idm.connector.ais2;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.*;

import static io.artin.idm.connector.ais2.Ais2Connector.ATTR_AIS_ID;

/**
 * Ais Filter Translator.
 *
 */
public class AisFilterTranslator extends AbstractFilterTranslator<Ais2Filter> {
    private static final Log LOG = Log.getLog(AisFilterTranslator.class);

    @Override
    protected Ais2Filter createEqualsExpression(EqualsFilter filter, boolean not) {
        LOG.ok("createEqualsExpression, filter: {0}, not: {1}", filter, not);

        return createByIdFilter(filter.getAttribute(), not);
    }

    @Override
    protected Ais2Filter createContainsExpression(ContainsFilter filter, boolean not) {
        LOG.ok("createContainsExpression, filter: {0}, not: {1}", filter, not);

        return createByIdFilter(filter.getAttribute(), not);
    }

    private Ais2Filter createByIdFilter(Attribute attr, boolean not){
        if (not) {
            return null;
        }

        LOG.ok("attr.getName: {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName()) || ATTR_AIS_ID.equals(attr.getName())) {
            if (attr.getValue() != null && attr.getValue().get(0) != null) {
                Ais2Filter sf = new Ais2Filter();
                sf.byId = String.valueOf(attr.getValue().get(0));
                LOG.ok("sf.byAisId: {0}, attr.getValue().get(0): {1}", sf.byId, attr.getValue().get(0));
                return sf;
            }
        }

        return null;
    }

    @Override
    protected Ais2Filter createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        LOG.ok("createGreaterThanExpression, filter: {0}, not: {1}", filter, not);

        if (not) {
            return null;
        }

        Attribute attr = filter.getAttribute();
        LOG.ok("attr.getName: {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName()) || ATTR_AIS_ID.equals(attr.getName())) {
            if (attr.getValue() != null && attr.getValue().get(0) != null) {
                Ais2Filter sf = new Ais2Filter();
                sf.byInterval = new Interval(Integer.parseInt(String.valueOf(attr.getValue().get(0))) + 1, null);
                LOG.ok("sf.byInterval createGreaterThanExpression: {0}, attr.getValue().get(0): {1}", sf.byInterval, attr.getValue().get(0));
                return sf;
            }
        }

        return null;
    }

    @Override
    protected Ais2Filter createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        LOG.ok("createGreaterThanOrEqualExpression, filter: {0}, not: {1}", filter, not);

        if (not) {
            return null;
        }

        Attribute attr = filter.getAttribute();
        LOG.ok("attr.getName: {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName()) || ATTR_AIS_ID.equals(attr.getName())) {
            if (attr.getValue() != null && attr.getValue().get(0) != null) {
                Ais2Filter sf = new Ais2Filter();
                sf.byInterval = new Interval(Integer.parseInt(String.valueOf(attr.getValue().get(0))), null);
                LOG.ok("sf.byInterval createGreaterThanOrEqualExpression: {0}, attr.getValue().get(0): {1}", sf.byInterval, attr.getValue().get(0));
                return sf;
            }
        }

        return null;
    }

    @Override
    protected Ais2Filter createLessThanExpression(LessThanFilter filter, boolean not) {
        LOG.ok("createLessThanExpression, filter: {0}, not: {1}", filter, not);

        if (not) {
            return null;
        }

        Attribute attr = filter.getAttribute();
        LOG.ok("attr.getName: {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName()) || ATTR_AIS_ID.equals(attr.getName())) {
            if (attr.getValue() != null && attr.getValue().get(0) != null) {
                Ais2Filter sf = new Ais2Filter();
                sf.byInterval = new Interval(null, Integer.parseInt(String.valueOf(attr.getValue().get(0))) - 1);
                LOG.ok("sf.byInterval createLessThanExpression: {0}, attr.getValue().get(0): {1}", sf.byInterval, attr.getValue().get(0));
                return sf;
            }
        }

        return null;
    }

    @Override
    protected Ais2Filter createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        LOG.ok("createLessThanOrEqualExpression, filter: {0}, not: {1}", filter, not);

        if (not) {
            return null;
        }

        Attribute attr = filter.getAttribute();
        LOG.ok("attr.getName: {0}, attr.getValue: {1}, Uid.NAME: {2}, Name.NAME: {3}", attr.getName(), attr.getValue(), Uid.NAME, Name.NAME);
        if (Uid.NAME.equals(attr.getName()) || ATTR_AIS_ID.equals(attr.getName())) {
            if (attr.getValue() != null && attr.getValue().get(0) != null) {
                Ais2Filter sf = new Ais2Filter();
                sf.byInterval = new Interval(null, Integer.parseInt(String.valueOf(attr.getValue().get(0))));
                LOG.ok("sf.byInterval createLessThanOrEqualExpression: {0}, attr.getValue().get(0): {1}", sf.byInterval, attr.getValue().get(0));
                return sf;
            }
        }

        return null;
    }

    @Override
    protected Ais2Filter createAndExpression(Ais2Filter leftExpression, Ais2Filter rightExpression) {
        LOG.ok("createAndExpression, leftExpression: {0}, rightExpression: {1}", leftExpression, rightExpression);
        if (leftExpression.byInterval == null || rightExpression.byInterval == null)
            return null; // not supported filter

        Ais2Filter andFiltered  = new Ais2Filter();
        andFiltered.byInterval = new Interval(leftExpression.byInterval.from, rightExpression.byInterval.to);

        LOG.ok("returning filter {0}", andFiltered);

        return andFiltered;
    }
}
