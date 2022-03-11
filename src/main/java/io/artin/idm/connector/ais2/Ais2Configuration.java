package io.artin.idm.connector.ais2;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Objects;
import java.util.WeakHashMap;

import ais.vratosoby.VratOsoby;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

import javax.net.ssl.X509TrustManager;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static org.identityconnectors.common.StringUtil.isEmpty;


/**
 * Configuration of the AIS2 Connector.
 *
 * @author gpalos
 */
public class Ais2Configuration extends AbstractConfiguration implements StatefulConfiguration {

    private static final Log LOG = Log.getLog(Ais2Configuration.class);

	private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * Collection of connectors that will be notified whenever the configuration changes.
     */
    private final Collection<Ais2Connector> observingConnectors = synchronizedSet(newSetFromMap(new WeakHashMap<>()));

    private String vratOsobyUrl = "";

	private Integer filterOdId;
	private Integer filterDoId;
	private Integer pageSize;

	private String username = "";
    private GuardedString password;
    private int connectTimeout = 30_000; // milliseconds
    private int receiveTimeout = 120_000; // milliseconds
    private String soapLogBasedirStr;
    private Path soapLogBasedir;

	private Boolean ais2TrustAllCerts = true; // FIXME false

    @ConfigurationProperty(order = 1,
			displayMessageKey = "vratOsobyUrl.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "vratOsobyUrl.help",
			required = true)
	public String getVratOsobyUrl() {
        return vratOsobyUrl;
    }

    public void setVratOsobyUrl(String urlString) {
		vratOsobyUrl = trim(urlString);
		notifyObservingConnectors();
    }

	@ConfigurationProperty(order = 2,
			displayMessageKey = "filterOdId.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "filterOdId.help",
			required = true)
	public Integer getFilterOdId() {
		return filterOdId;
	}

	public void setFilterOdId(Integer filterOdId) {
		this.filterOdId = filterOdId;
		notifyObservingConnectors();
	}

	@ConfigurationProperty(order = 3,
			displayMessageKey = "filterDoId.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "filterDoId.help",
			required = true)
	public Integer getFilterDoId() {
		return filterDoId;
	}

	public void setFilterDoId(Integer filterDoId) {
		this.filterDoId = filterDoId;
		notifyObservingConnectors();
	}

	@ConfigurationProperty(order = 4,
			displayMessageKey = "pageSize.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "pageSize.help")
	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
		notifyObservingConnectors();
	}

	public int getEffectivePageSize() {
		return Objects.requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);
	}

    @ConfigurationProperty(order = 5,
			displayMessageKey = "username.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "username.help")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
		this.username = trimToNull(username);
        notifyObservingConnectors();
    }

    @ConfigurationProperty(order = 6,
			displayMessageKey = "password.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "password.help",
			confidential = true)
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
        notifyObservingConnectors();
    }

	@ConfigurationProperty(order = 10,
			displayMessageKey = "soapLogBasedir.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "soapLogBasedir.help")
	public String getSoapLogBasedir() {
		return soapLogBasedirStr;
	}

	public void setSoapLogBasedir(final String soapLogBasedir) {
		soapLogBasedirStr = trimToNull(soapLogBasedir);

		if (soapLogBasedirStr == null) {
			this.soapLogBasedir = null;
		} else {
			try {
				this.soapLogBasedir = Paths.get(soapLogBasedirStr);
				soapLogBasedirStr = this.soapLogBasedir.toString(); //normalized

				File dir = new File(soapLogBasedirStr);
				if (!dir.exists()){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdirs();
				}
			} catch (InvalidPathException ex) {
				LOG.info(ex, "The SOAP log basedir is not a valid path.");
				this.soapLogBasedir = null;
			}
		}

		notifyObservingConnectors();
	}

	@ConfigurationProperty(order = 11,
			displayMessageKey = "ais2TrustAllCerts.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "ais2TrustAllCerts.help",
			required = true)
	public Boolean getAis2TrustAllCerts() {
		return ais2TrustAllCerts;
	}

	@SuppressWarnings("unused")
	public void setAis2TrustAllCerts(Boolean ais2TrustAllCerts) {
		ais2TrustAllCerts = ais2TrustAllCerts;
	}

	public boolean isAis2TrustAllCerts() {
		return Boolean.TRUE.equals(getAis2TrustAllCerts());
	}

	@ConfigurationProperty(order = 12,
			displayMessageKey = "connectTimeout.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "connectTimeout.help")
	public int getConnectTimeout() {
		return connectTimeout;
	}

	@SuppressWarnings("unused")
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		notifyObservingConnectors();
	}

	@ConfigurationProperty(order = 13,
			displayMessageKey = "receiveTimeout.display",
			groupMessageKey = "basic.group",
			helpMessageKey = "receiveTimeout.help")
	public int getReceiveTimeout() {
		return receiveTimeout;
	}

	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
		notifyObservingConnectors();
	}

	Path getSoapLogTargetPath() {
		return soapLogBasedir;
	}

	void addObservingConnector(final Ais2Connector connector) {
		observingConnectors.add(connector);
	}

	void removeObservingConnector(final Ais2Connector connector) {
		observingConnectors.remove(connector);
	}

	private void notifyObservingConnectors() {
		for (Ais2Connector connector : observingConnectors.toArray(new Ais2Connector[0])) {
			connector.configurationChanged();
		}
	}

	@Override
	public void release() {
		vratOsobyUrl = null;
		username = null;
		password = null;
		soapLogBasedir = null;
		soapLogBasedirStr = null;
		observingConnectors.clear();
	}

	@Override
    public void validate() {
		if (isEmpty(vratOsobyUrl)) {
			throw new ConfigurationException("WSDL is not specified.");
		}

		try {
			new URL(vratOsobyUrl);
			new URI(vratOsobyUrl);
			LOG.ok("The URL is valid: {0}", vratOsobyUrl);
		} catch (MalformedURLException | URISyntaxException urlException) {
			LOG.info("The URL of the WS is INVALID: {0}", vratOsobyUrl);
			throw new ConfigurationException("WSDL URL is invalid." + vratOsobyUrl, urlException);
		}

		if (connectTimeout < 0) {
			throw new ConfigurationException("The connection timeout must be positive.");
		}

		if (receiveTimeout < 0) {
			throw new ConfigurationException("The receive timeout must be positive.");
		}

		if (soapLogBasedirStr != null) {
			if (soapLogBasedir == null) {
				throw new ConfigurationException("The SOAP log basedir is not a valid path.");
			}

			if (!soapLogBasedir.isAbsolute()) {
				throw new ConfigurationException("The SOAP log basedir is not an absolute path.");
			}

			if (!isDirectory(soapLogBasedir)) {
				if (exists(soapLogBasedir)) {
					throw new ConfigurationException("The path to the SOAP log basedir (" + getSoapLogBasedir() + ") points to an existing file.");
				} else {
					throw new ConfigurationException("The SOAP log basedir (" + getSoapLogBasedir() + ") doesn't exist.");
				}
			}
		}
    }

	private static String trim(String str) {
		return str != null ? str.trim() : null;
	}

	private static String trimToNull(String str) {
		if (str == null) {
			return null;
		} else {
			String trimmed = str.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}

	public String getWsUrl(@SuppressWarnings("rawtypes") Class seiClass) {
    	if (VratOsoby.class.equals(seiClass)) {
			return vratOsobyUrl;
		}

		throw new ConfigurationException("not supported class type");
	}

	public String getSslDisableCnCheck()  {
		return isAis2TrustAllCerts() ? Boolean.TRUE.toString() : null;
	}

	public String getSslTrustManager()  {
		return isAis2TrustAllCerts() ? "NonValidatingTM" : null;
	}

	protected boolean tamperSsl() {
		return getSslTrustManager() != null || getSslDisableCnCheck() != null;
	}

	protected static class NonValidatingTM implements X509TrustManager {

		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	}
}
