/**
 * NEXUSe2e Business Messaging Open Source
 * Copyright 2000-2009, Tamgroup and X-ioma GmbH
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation version 2.1 of
 * the License.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nexuse2e.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Hex;
import org.nexuse2e.NexusException;
import org.nexuse2e.configuration.CertificateType;
import org.nexuse2e.configuration.Constants;
import org.nexuse2e.pojo.CertificatePojo;

/**
 * Utility class to work with certificates (mostly instances of <code>X509Certificate</code>).
 * Uses the BouncyCastly cryptography provider under the covers.
 * 
 * @author gesch
 * @version $ID:$
 */
public class CertificateUtil {

    private static Logger      LOG                                 = Logger.getLogger(CertificateUtil.class);

    public static final int    DEFAULT_RSA_KEY_LENGTH              = 2048;
    public static final String DEFAULT_DIGITAL_SIGNATURE_ALGORITHM = "SHA1withRSA";
    public static final String DEFAULT_KEY_ALGORITHM               = "RSA";
    public static final String DEFAULT_CERT_TYPE                   = "X.509";
    public static final String DEFAULT_KEY_STORE                   = "PKCS12";
    public static final String DEFAULT_JCE_PROVIDER                = "BC";
    private static final int   PEM_LINE_LENGTH                     = 64;

    /**
     * Gets a private key from the given <code>KeyStore</code>.
     * 
     * @param keyStore
     *            The key store, not <code>null</code>.
     * @return The private key.
     */
    public static Key getPrivateKey(KeyStore keyStore) throws IllegalArgumentException {

        String keyAlias = null;
        PrivateKey returnPrivateKey = null;

        try {
            Enumeration<String> enumeration = keyStore.aliases();

            while (enumeration.hasMoreElements()) {
                String temp = enumeration.nextElement();

                if (keyStore.isKeyEntry(temp)) {
                    keyAlias = temp;
                }
            }
            returnPrivateKey = (PrivateKey) keyStore.getKey(keyAlias, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error finding private key!", e);
        }
        return returnPrivateKey;
    }

    // /**
    // * @param data
    // * @param password
    // * @return
    // * @throws IllegalArgumentException
    // */
    // public static Key getPrivateKey( byte[] data, String password ) throws IllegalArgumentException {
    //
    // return getPrivateKey( getPKCS12KeyStore( data, password ) );
    // }
    //
    // /**
    // * @param pojo
    // * @return
    // * @throws IllegalArgumentException
    // */
    // public static Key getPrivateKey( CertificatePojo pojo ) throws IllegalArgumentException {
    //
    // if ( !pojo.isPKCS12() ) {
    // throw new IllegalArgumentException( "pojo doesn't contain pkcs12 keystore" );
    // }
    // return getPrivateKey( pojo.getBinaryData(), EncryptionUtil.decryptString( pojo.getPassword() ) );
    // }

    /**
     * @param privateKeyPojo
     * @return
     */
    public static KeyPair getKeyPair(final CertificatePojo privateKeyPojo) {

        if (privateKeyPojo == null) {
            return null;
        }
        StringReader sr = new StringReader(new String(privateKeyPojo.getBinaryData()));
        PEMReader pemReader = new PEMReader(sr, new PasswordFinder() {

            public char[] getPassword() {

                return EncryptionUtil.decryptString(privateKeyPojo.getPassword()).toCharArray();
            }
        });
        try {
            KeyPair kp = (KeyPair) pemReader.readObject();
            return kp;
        } catch (IOException e) {
            LOG.error("Error while reading Private Key from Pojo: " + e);
        } finally {
            try {
                pemReader.close();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    /**
     * @param pojo
     * @return
     * @throws IllegalArgumentException
     */
    public static X509Certificate getX509Certificate(CertificatePojo pojo) throws IllegalArgumentException {

        if (pojo == null) {
            throw new IllegalArgumentException("Pojo must not null!");
        }
        if (!pojo.isX509()) {
            throw new IllegalArgumentException("Pojo contains no X509");
        }
        return getX509Certificate(pojo.getBinaryData());
    }

    /**
     * @param certData
     * @return
     * @throws IllegalArgumentException
     */
    public static X509Certificate getX509Certificate(byte[] certData) throws IllegalArgumentException {

        X509Certificate x509Certificate = null;
        CertificateFactory certificateFactory = null;

        try {
            if (certData != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(certData);
                try {
                    certificateFactory = CertificateFactory.getInstance(DEFAULT_CERT_TYPE, DEFAULT_JCE_PROVIDER);
                } catch (NoSuchProviderException e) {
                    LOG.error("Could not create CertificateFactory for Bouncy Castle provider!");
                    certificateFactory = CertificateFactory.getInstance(DEFAULT_CERT_TYPE);
                }
                x509Certificate = (X509Certificate) certificateFactory.generateCertificate(bais);
            }
        } catch (CertificateException e) {
            throw new IllegalArgumentException(e);
        }

        return x509Certificate;
    }

    /**
     * Not tested
     * 
     * @param certs
     * @return
     */
    public static Collection<? extends Certificate> getX509Certificates(byte[] certs) throws CertificateException {

        Collection<? extends Certificate> certCollection = null;

        CertificateFactory certificateFactory;
        try {
            try {
                certificateFactory = CertificateFactory.getInstance(DEFAULT_CERT_TYPE, DEFAULT_JCE_PROVIDER);
            } catch (NoSuchProviderException e) {
                LOG.error("Could not create CertificateFactory for Bouncy Castle provider!");
                certificateFactory = CertificateFactory.getInstance(DEFAULT_CERT_TYPE);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(certs);
            certCollection = certificateFactory.generateCertificates(bais);
            // if ( LOG.isDebugEnabled() ) {
            // Iterator i = certCollection.iterator();
            // while ( i.hasNext() ) {
            // Certificate cert = (Certificate) i.next();
            // LOG.debug( cert );
            // }
            // }
        } catch (CertificateException e) {
            e.printStackTrace();
        }

        return certCollection;
    }

    public static KeyStore getPKCS12KeyStore(CertificatePojo pojo) throws IllegalArgumentException {

        if (!pojo.isPKCS12()) {
            throw new IllegalArgumentException("Pojo contains no PKCS12");
        }
        return getPKCS12KeyStore(pojo.getBinaryData(), EncryptionUtil.decryptString(pojo.getPassword()));
    }

    /**
     * @param data
     * @param password
     * @return
     * @throws IllegalArgumentException
     */
    public static KeyStore getPKCS12KeyStore(byte[] data, String password) throws IllegalArgumentException {

        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(Constants.DEFAULT_KEY_STORE, Constants.DEFAULT_JCE_PROVIDER);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            keyStore.load(bais, password.toCharArray());
            if (LOG.isDebugEnabled()) {
                Enumeration<String> enumeration = keyStore.aliases();
                if (enumeration.hasMoreElements()) {
                    String alias = enumeration.nextElement();
                    LOG.debug("getPKCS12KeyStoreFromByteArray - alias: " + alias);
                    Certificate certificate = keyStore.getCertificate(alias);
                    LOG.trace("getPKCS12KeyStoreFromByteArray: " + certificate);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return keyStore;
    }

    /**
     * Get the public certificate chain pkcs12 KeyStore
     * 
     * @return The public certificate chain
     */
    public static Certificate[] getCertificateChain(KeyStore pkcs12) throws IllegalArgumentException {

        Certificate[] certs = null;

        try {
            if (pkcs12 != null) {
                Enumeration<String> enumeration = pkcs12.aliases();
                while (enumeration.hasMoreElements()) {
                    String alias = enumeration.nextElement();
                    certs = (Certificate[]) pkcs12.getCertificateChain(alias);
                    if (certs != null && certs.length > 0) {
                        break;

                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return certs;
    }

    /**
     * Gets the head of the certificate chain which is the public certificate matching the private key. For CA
     * certificates use {@link CertificateUtil#getCertificateChain(KeyStore)} .
     * 
     * 
     * @param pkcs12
     * @return the head certificate
     * @throws IllegalArgumentException
     */
    public static X509Certificate getHeadCertificate(KeyStore pkcs12) throws IllegalArgumentException {

        Certificate[] certs = null;
        certs = getCertificateChain(pkcs12);
        if (certs != null && certs.length > 0) {
            return (X509Certificate) certs[0];
        }
        return null;
    }

    /**
     * @param pojo
     * @return
     * @throws IllegalArgumentException
     */
    public static X509Certificate getHeadCertificate(CertificatePojo pojo) throws IllegalArgumentException {

        KeyStore keyStore = getPKCS12KeyStore(pojo);
        return getHeadCertificate(keyStore);
    }

    /**
     * @param x509Certificate
     * @param subject
     *            used to differ between subject and issuer entries
     * @return
     */
    public static X509Principal getPrincipalFromCertificate(X509Certificate x509Certificate, boolean subject) {

        X509Principal x509Principal = null;
        try {
            if (subject) {
                x509Principal = PrincipalUtil.getSubjectX509Principal(x509Certificate);
            } else {
                x509Principal = PrincipalUtil.getIssuerX509Principal(x509Certificate);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return x509Principal;
    }

    /**
     * @param x509Principal
     * @param id
     * @return
     */
    public static String getCertificateInfo(X509Name x509Principal, DERObjectIdentifier id) {

        String result = null;
        try {
            Vector<?> ids = x509Principal.getOIDs();
            Vector<?> values = x509Principal.getValues();
            for (int i = 0; i < ids.size(); i++) {
                DERObjectIdentifier innerId = (DERObjectIdentifier) ids.elementAt(i);
                if (innerId.equals(id)) {
                    result = (String) values.elementAt(i);
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * @param cert
     * @param id
     *            constants are defined on X509Name
     * @return
     */
    public static String getIssuer(X509Certificate cert, DERObjectIdentifier id) {

        if (cert == null || id == null) {
            return "";
        }
        return getCertificateInfo(getPrincipalFromCertificate(cert, false), id);
    }

    /**
     * @param cert
     * @param id
     *            constants are defined on X509Name
     * @return
     */
    public static String getSubject(X509Certificate cert, DERObjectIdentifier id) {

        if (cert == null || id == null) {
            return "";
        }
        return getCertificateInfo(getPrincipalFromCertificate(cert, true), id);
    }

    /**
     * @param certPojos
     * @return
     * @throws IllegalArgumentException
     */
    public static KeyStore generateKeyStoreFromPojos(List<CertificatePojo> certPojos) throws IllegalArgumentException {

        Map<String, X509Certificate> certificates = new HashMap<String, X509Certificate>();
        for (CertificatePojo certPojo : certPojos) {
            X509Certificate cert = CertificateUtil.getX509Certificate(certPojo);
            certificates.put(certPojo.getName(), cert);
        }
        return generateKeyStore(certificates);
    }

    /**
     * @param certificates
     *            Map containing Alias/X509Certificate pairs.
     * @return
     * @throws IllegalArgumentException
     */
    public static KeyStore generateKeyStore(Map<String, X509Certificate> certificates) throws IllegalArgumentException {

        KeyStore jks = null;
        try {
            jks = KeyStore.getInstance("JKS");
            jks.load(null, null);
            if (certificates != null) {
                for (String alias : certificates.keySet()) {
                    if (!StringUtils.isEmpty(alias)) {
                        X509Certificate certificate = certificates.get(alias);
                        if (certificate != null) {
                            jks.setCertificateEntry(alias, certificate);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jks;
    }

    /**
     * Return the remaining time till certificate expires as rounded human readable String.
     * 
     * @param x509certificate
     *            The <code>X509Certificate</code> to inspect
     * @return remaining time till certificate expires as rounded human readable string
     */
    public static String getRemainingValidity(X509Certificate x509certificate) {

        long nowMils = System.currentTimeMillis();
        long expMils = x509certificate.getNotAfter().getTime();
        String remaining = "";
        if (nowMils < expMils) {
            long div = expMils - nowMils;
            long day = 1000 * 60 * 60 * 24;
            long remDays = div / day;
            if (remDays > 14) {
                long week = day * 7;
                long remWeeks = div / week;
                if (remWeeks > 8) {
                    long month = day * 30;
                    long remMonth = div / month;
                    if (remMonth > 24) {
                        long year = month * 12;
                        long remYears = div / year;
                        remaining = "(~ " + remYears + " Years remaining)";
                    } else {
                        remaining = "(~ " + remMonth + " Month remaining)";
                    }
                } else {
                    remaining = "(~ " + remWeeks + " weeks remaining)";
                }
            } else {
                remaining = "(~ " + remDays + " days remaining)";
            }
        }
        return remaining;
    } // getRemainingValidity

    /**
     * @param x509Certificate
     * @return
     * @throws CertificateEncodingException
     */
    public static byte[] getDERData(X509Certificate x509Certificate) throws CertificateEncodingException {

        if (x509Certificate != null) {
            return x509Certificate.getEncoded();

        }
        return null;

    }

    /**
     * @param request
     * @return
     * @throws CertificateEncodingException
     */
    public static byte[] getDERData(PKCS10CertificationRequest request) throws CertificateEncodingException {

        // needs to be checked: Encoded or DEREncoded

        if (request != null) {
            return request.getDEREncoded();
        }
        return null;
    }

    /**
     * Get a <code>X509Certificate</code> as a PEM encoded byte array
     * 
     * @param x509Certificate
     *            The <code>X509Certificate</code>
     * @return The <code>X509Certificate</code> as a PEM encoded byte array
     * @throws CertificateEncodingException
     */
    public static String getPemData(X509Certificate x509Certificate) throws CertificateEncodingException {

        String pem = new String(Base64.encodeBase64(x509Certificate.getEncoded()));
        byte[] pembytes = pem.getBytes();
        StringBuffer buffer = new StringBuffer();
        buffer.append("-----BEGIN CERTIFICATE-----\n");
        int offset = 0;
        while (offset < pembytes.length) {
            byte[] temp;
            if (pembytes.length >= offset + PEM_LINE_LENGTH) {
                temp = new byte[PEM_LINE_LENGTH];
            } else {
                temp = new byte[pembytes.length - offset];
            }
            System.arraycopy(pembytes, offset, temp, 0, temp.length);
            for (int i = 0; i < temp.length; i++) {
                buffer.append((char) temp[i]);
            }
            buffer.append("\n");

            offset += PEM_LINE_LENGTH;
        }
        buffer.append("-----END CERTIFICATE-----");

        return buffer.toString();
    }

    /**
     * @param privateKey
     * @param password
     * @return
     */
    public static String getPemData(KeyPair privateKey, String password) {

        StringWriter sw = new StringWriter();
        PEMWriter writer = new PEMWriter(sw, Constants.DEFAULT_JCE_PROVIDER);
        try {
            SecureRandom sr = new SecureRandom();
            writer.writeObject(privateKey.getPrivate(), "DES-EDE3-CBC", password.toCharArray(), sr);
            writer.flush();
            sw.flush();
            sw.close();
        } catch (IOException e) {
            LOG.error("error while creating PrivateKey Pojo: " + e);
            return null;
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        return sw.getBuffer().toString();
    }

    /**
     * @param x509Certificate
     * @return
     */
    public static String createCertificateId(X509Certificate x509Certificate) {

        return createCertificateId(x509Certificate, true);
    }

    /**
     * @param x509Certificate
     * @param replaceNonPrintable
     * @return
     */
    public static String createCertificateId(X509Certificate x509Certificate, boolean replaceNonPrintable) {

        String certId = null;
        PKCS12BagAttributeCarrier bagAttr = null;
        DERBMPString friendlyName = null;

        try {

            if (x509Certificate instanceof PKCS12BagAttributeCarrier) {
                bagAttr = (PKCS12BagAttributeCarrier) x509Certificate;

                friendlyName = (DERBMPString) bagAttr.getBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName);
                if (friendlyName != null) {
                    LOG.debug("createCertificateIdFromCertificate - Found friendly name: " + friendlyName.getString());
                    certId = friendlyName.getString();
                    // Remove all non-printable characters
                    if (replaceNonPrintable) {
                        certId = certId.replaceAll("[\\P{Alpha}]+", "");
                    }
                }

            } else {
                LOG.error("X509Certificate not an instance of PKCS12BagAttributeCarrier!");
            }

            if (certId == null) {
                X509Name x509Name = PrincipalUtil.getSubjectX509Principal(x509Certificate);
                certId = getCertificateInfo(x509Name, X509Name.CN);
                String o = getCertificateInfo(x509Name, X509Name.O);
                String ou = getCertificateInfo(x509Name, X509Name.OU);

                if (certId == null) {
                    LOG.debug("createCertificateIdFromCertificate - CN not found, trying O");
                    certId = o;
                }
                if (certId == null) {
                    LOG.debug("createCertificateIdFromCertificate - O not found, trying OU");
                    certId = ou;
                }
            }
        } catch (Exception e) {
            LOG.error("Error retrieving principal from certificate! (" + e + ")");
        }
        LOG.debug("createCertificateIdFromCertificate - cert ID: '" + certId + "'");

        return certId;
    }

    /**
     * @param pojo
     * @return
     */
    static public PKCS10CertificationRequest getPKCS10Request(CertificatePojo pojo) throws IllegalArgumentException {

        if (pojo == null || !pojo.isPKCS10() || pojo.getBinaryData() == null || pojo.getBinaryData().length == 0) {
            throw new IllegalArgumentException("Pojo doesn't contain a PKCS10");
        }
        return new PKCS10CertificationRequest(pojo.getBinaryData());

    }

    /**
     * @param requestData
     * @return
     */
    public static String getPemData(PKCS10CertificationRequest request) throws CertificateEncodingException {

        StringWriter sw = new StringWriter();
        PEMWriter writer = new PEMWriter(sw);
        try {
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            LOG.error("Error while creating pem data: " + e);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }

        return sw.toString();
    }

    /**
     * creates an PKCS10 Certificate Request using the given keys and parameters
     * Use java.security.KeyPairGenerator for key creation. see {@link CertificateUtil.generateKeyPair()};
     * 
     * @param keyPair
     * @param commonName
     * @param organization
     * @param organizationUnit
     * @param location
     * @param country
     * @param state
     * @param email
     * @return
     * @throws IllegalArgumentException
     */
    public static PKCS10CertificationRequest generatePKCS10CertificateRequest(KeyPair keyPair, String commonName, String organization, String organizationUnit,
            String location, String country, String state, String email) throws IllegalArgumentException {

        try {

            Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
            Vector<String> values = new Vector<String>();

            oids.add(X509Name.CN);
            oids.add(X509Name.C);
            oids.add(X509Name.O);
            oids.add(X509Name.OU);
            oids.add(X509Name.L);
            oids.add(X509Name.ST);

            values.add(commonName);
            values.add(country);
            values.add(organization);
            values.add(organizationUnit);
            values.add(location);
            values.add(state);

            if ((email != null) && (email.length() != 0)) {
                oids.add(X509Principal.E);
                oids.add(X509Principal.EmailAddress);
                values.add(email);
                values.add(email);
            }

            X509Name subject = new X509Name(oids, values);

            PKCS10CertificationRequest certificationRequest = new PKCS10CertificationRequest(DEFAULT_DIGITAL_SIGNATURE_ALGORITHM, subject, keyPair.getPublic(),
                    null, keyPair.getPrivate());

            return certificationRequest;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }

    }

    /**
     * Generate a <code>KeyPair</code> with default settings (<code>DEFAULT_KEY_ALGORITHM</code>, <code>DEFAULT_RSA_KEY_LENGTH</code>).
     * 
     * @return The generated key pair.
     * @throws NoSuchAlgorithmException
     *             If the JCE provider is not set up correctly
     * @throws NoSuchProviderException
     *             If the JCE provider is not set up correctly
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {

        return generateKeyPair(DEFAULT_RSA_KEY_LENGTH);
    }

    /**
     * Generate a <code>KeyPair</code> with given key length and <code>DEFAULT_KEY_ALGORITHM</code>.
     * 
     * @param keyLength
     *            The key length.
     * @return The generated key pair.
     * @throws NoSuchAlgorithmException
     *             If the JCE provider is not set up correctly
     * @throws NoSuchProviderException
     *             If the JCE provider is not set up correctly
     */
    public static KeyPair generateKeyPair(int keyLength) throws NoSuchAlgorithmException, NoSuchProviderException {

        return generateKeyPair(keyLength, DEFAULT_KEY_ALGORITHM);
    }

    /**
     * Generate a <code>KeyPair</code> with given key length and algorithm.
     * 
     * @param keyLength
     *            The key length.
     * @return The generated key pair.
     * @throws NoSuchAlgorithmException
     *             If the JCE provider is not set up correctly
     * @throws NoSuchProviderException
     *             If the JCE provider is not set up correctly
     */
    public static KeyPair generateKeyPair(int keyLength, String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm, DEFAULT_JCE_PROVIDER);
        kpg.initialize(keyLength);
        return kpg.generateKeyPair();
    }

    /**
     * Creates a <code>CertificatePojo</code> from the given <code>KeyStore</code>.
     * 
     * @param type
     *            The type, e.g. CertificateType.STAGING.getOrdinal().
     * @param keyStore
     *            The KeyStore.
     * @param password
     *            The password the key store is encrypted with.
     * @return The certificate chain found in the key store, as a <code>CertificatePojo</code>.
     * @throws NexusException
     */
    public static CertificatePojo createPojoFromPKCS12(int type, KeyStore keyStore, String password) throws NexusException {

        try {
            // Fix MS IIS exported PKCS12 structures (.pfx) (remove all aliases, create new keyStore with friendly named alias)
            Key pk = getPrivateKey(keyStore);
            Certificate[] tempCerts = getCertificateChain(keyStore);
            KeyStore newKs = KeyStore.getInstance(DEFAULT_KEY_STORE, DEFAULT_JCE_PROVIDER);
            newKs.load(null, null);

            if ((tempCerts != null) && (tempCerts.length != 0)) {

                String id = createCertificateId((X509Certificate) tempCerts[0]);

                newKs.setKeyEntry(id, pk, password.toCharArray(), tempCerts);

                CertificatePojo certificatePojo = new CertificatePojo();
                certificatePojo.setType(type);
                certificatePojo.setName(id);

                System.out.println("dn: " + id);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                newKs.store(baos, password.toCharArray());
                byte[] certData = baos.toByteArray();
                certificatePojo.setBinaryData(certData);

                certificatePojo.setPassword(EncryptionUtil.encryptString(password));
                certificatePojo.setCreatedDate(new Date());
                certificatePojo.setModifiedDate(new Date());

                return certificatePojo;

            } else {
                throw new NexusException("No certificate chain found, can't import certificate");
            }
        } catch (Exception e) {
            if (e instanceof NexusException) {
                throw (NexusException) e;
            }
            throw new NexusException("Error importing key store", e);
        }
    }

    /**
     * @param keyPair
     * @return
     */
    public static CertificatePojo createPojoFromKeyPair(KeyPair keyPair, String name, String password) {

        CertificatePojo privateKeyPojo = new CertificatePojo();
        privateKeyPojo.setType(CertificateType.PRIVATE_KEY.getOrdinal());

        privateKeyPojo.setBinaryData(getPemData(keyPair, password).getBytes());
        privateKeyPojo.setName(name);
        privateKeyPojo.setPassword(EncryptionUtil.encryptString(password));

        return privateKeyPojo;
    }

    /**
     * @param request
     * @return
     */
    public static CertificatePojo createPojoFromPKCS10(PKCS10CertificationRequest request) {

        CertificatePojo requestPojo = new CertificatePojo();

        requestPojo.setBinaryData(request.getEncoded());
        requestPojo.setType(CertificateType.REQUEST.getOrdinal());
        requestPojo.setName(request.getCertificationRequestInfo().getSubject().toString());
        return requestPojo;
    }

    /**
     * @param certificate
     * @param type
     * @return
     */
    public static CertificatePojo createPojoFromX509(X509Certificate certificate, int type) {

        CertificatePojo certPojo = new CertificatePojo();

        try {
            certPojo.setBinaryData(certificate.getEncoded());
            certPojo.setType(type);
            certPojo.setName(createCertificateId(certificate));
            certPojo.setCreatedDate(new Date());
            certPojo.setModifiedDate(new Date());
            return certPojo;
        } catch (CertificateEncodingException e) {
            LOG.error("error while creating certificate pojo: " + e);

        }

        return null;

    }

    public static PKIXCertPathBuilderResult getCertificateChain(X509Certificate head, List<X509Certificate> certs, List<X509Certificate> trustedCerts) {

        BigInteger requestModulus = ((RSAPublicKey) head.getPublicKey()).getModulus();
        BigInteger requestExponent = ((RSAPublicKey) head.getPublicKey()).getPublicExponent();

        List<X509Certificate> list = new ArrayList<X509Certificate>();
        Set<TrustAnchor> trust = new HashSet<TrustAnchor>();
        for (X509Certificate trustedCert : trustedCerts) {
            trust.add(new TrustAnchor(trustedCert, null));
        }

        for (X509Certificate cert : certs) {
            list.add(cert);
            BigInteger modulus = ((RSAPublicKey) cert.getPublicKey()).getModulus();
            BigInteger exponent = ((RSAPublicKey) cert.getPublicKey()).getPublicExponent();

            if (modulus.equals(requestModulus) && exponent.equals(requestExponent)) {
                // headcert
            } else if (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
                // Certificate is self-signed
                trust.add(new TrustAnchor(cert, null));
            }
        }

        try {
            // Add certificate chain
            CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(list);
            CertStore store = CertStore.getInstance("Collection", ccsp, Constants.DEFAULT_JCE_PROVIDER);

            // Set certificate to verify
            X509CertSelector targetConstraints = new X509CertSelector();
            targetConstraints.setCertificate(head);

            // Trust should contain only trusted certificates, see for-loop above. The second parameter is the certificate to check.
            PKIXBuilderParameters params = new PKIXBuilderParameters(trust, targetConstraints);
            params.setRevocationEnabled(false);
            params.addCertStore(store);
            params.setDate(new Date());

            CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX", Constants.DEFAULT_JCE_PROVIDER);
            try {
                return (PKIXCertPathBuilderResult) cpb.build(params);
            } catch (CertPathBuilderException e) {
                LOG.error("Error while building cert chain: " + e);
            }
        } catch (Exception e) {
            LOG.error("creating Certificate Chain: " + e);
        }
        return null;
    }

    /**
     * @param cert
     * @return
     * @throws CertificateEncodingException
     */
    public static String getMD5Fingerprint(X509Certificate cert) throws CertificateEncodingException {

        byte[] resBuf;
        Digest digest = new MD5Digest();
        resBuf = new byte[digest.getDigestSize()];
        digest.update(cert.getEncoded(), 0, cert.getEncoded().length);

        digest.doFinal(resBuf, 0);
        return new String(Hex.encode(resBuf));

    }

    /**
     * @param cert
     * @return
     * @throws CertificateEncodingException
     */
    public static String getSHA1Fingerprint(X509Certificate cert) throws CertificateEncodingException {

        byte[] resBuf;
        Digest digest = new SHA1Digest();
        resBuf = new byte[digest.getDigestSize()];
        digest.update(cert.getEncoded(), 0, cert.getEncoded().length);

        digest.doFinal(resBuf, 0);
        return new String(Hex.encode(resBuf));

    }

    /**
     * @param certChain
     * @return
     */
    public static boolean isCertChainComplete(Object[] certChain) {

        if (certChain != null && certChain.length > 0) {
            X509Certificate x509Certificate = (X509Certificate) certChain[certChain.length - 1];
            X509Principal subject = getPrincipalFromCertificate(x509Certificate, true);
            X509Principal issuer = getPrincipalFromCertificate(x509Certificate, false);
            return subject.equals(issuer);
        }

        return false;
    } // isCertChainComplete

    /**
     * Determines if the given certificate is self-signed.
     * 
     * @param cert
     *            The certificate to check.
     * @return <code>true</code> if and only if the given certificate is self-signed.
     */
    public static boolean isSelfSigned(X509Certificate cert) {
        X509Principal subject = CertificateUtil.getPrincipalFromCertificate(cert, true);
        X509Principal issuer = CertificateUtil.getPrincipalFromCertificate(cert, false);
        return (subject.equals(issuer));
    }

    public static Certificate[] getCertificateChain(CertificatePojo certificate, List<X509Certificate> certificates) {
        byte[] partnerCertBytes = null;
        X509Certificate partnerCert = null;
        if (certificate != null) {
            partnerCertBytes = certificate.getBinaryData();
            if (partnerCertBytes != null) {
                partnerCert = getX509Certificate(partnerCertBytes);
                return getCertificateChain(partnerCert, certificates);
            }
        }

        return null;
    }

    /**
     * @param certificate
     * @param certificates
     * @return
     */
    public static Certificate[] getCertificateChain(X509Certificate head, List<X509Certificate> certificates) {

        List<X509Certificate> chain = new ArrayList<X509Certificate>();
        if (head != null) {
            chain.add(head);
            complementCertificateChain(chain, certificates);
        }

        return chain.toArray(new Certificate[chain.size()]);
    }

    /**
     * Recursively searched the certificate pool for a valid issuer certificate of the last chain link.
     * 
     * @param chain
     *            The chain of trust so far. This is an in/out parameter.
     *            The method recursively adds certificates to the end of the chain,
     *            if such can be bound in the <code>certPool</code>. Otherwise the method terminates.
     * @param certPool
     *            The pool of trusted certificates.
     */
    private static boolean complementCertificateChain(List<X509Certificate> chain, List<X509Certificate> certPool) {
        if (chain != null && chain.size() > 0 && certPool != null && certPool.size() > 0) {
            X509Certificate lastChainLink = chain.get(chain.size() - 1);
            if (lastChainLink != null) {
                X500Principal issuer = lastChainLink.getIssuerX500Principal();
                // only try to find issuer cert, if cert is not self-signed; otherwise we would end up with an infinite loop
                if (issuer != null && !issuer.equals(lastChainLink.getSubjectX500Principal())) {
                    List<X509Certificate> candidates = new ArrayList<>();
                    for (X509Certificate currCert : certPool) {

                        // the issuer of the last chain link must be the subject of the next chain link
                        if (currCert != null && issuer.equals(currCert.getSubjectX500Principal())) {
                            try {
                                // unfortunately a verification failure is indicated by an exception
                                lastChainLink.verify(currCert.getPublicKey());
                                // if no exception was thrown, the cert is a candidate to be the right ca cert
                                candidates.add(currCert);
                            } catch (Exception e) {
                                // current certificate is not the issuer
                            }
                        }
                    }
                    if (candidates.size() > 0) {
                        boolean completed = false;
                        for (X509Certificate candidate : candidates) {
                            List<X509Certificate> possibleChain = new ArrayList<>();
                            possibleChain.addAll(chain);
                            possibleChain.add(candidate); // add issuer to chain and make next recursion step
                            // recursion
                            completed = complementCertificateChain(possibleChain, certPool);
                            if (completed) {
                                chain.clear();
                                chain.addAll(possibleChain);
                                break;
                            }
                        }
                        return completed;
                    } else {
                        return false;
                    }

                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return
     */
    public static HashMap<X509Principal, X509Certificate> getX509CertificateHashMap(List<X509Certificate> certificateParts) throws NexusException {

        HashMap<X509Principal, X509Certificate> hashMap = new HashMap<X509Principal, X509Certificate>();

        if (certificateParts != null) {

            for (X509Certificate x509Certificate : certificateParts) {

                // String id = certificatePojo.getCertificateId();
                X509Principal id = getPrincipalFromCertificate(x509Certificate, true);

                hashMap.put(id, x509Certificate);
            }
        }
        return hashMap;
    }

    /**
     * Creates the <code>KeyManager</code>s for the given keystore and encryption password.
     * 
     * @param keystore
     * @param password
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    public static KeyManager[] createKeyManagers(final KeyStore keystore, final String password) throws KeyStoreException, NoSuchAlgorithmException,
            UnrecoverableKeyException {

        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }

        KeyManager[] managers = new KeyManager[1];
        managers[0] = new NexusKeyManager(keystore, password);
        return managers;
    }

    /**
     * Creates the <code>TrustManager</code>s for the given trusted keystore.
     * 
     * @param keystore
     *            The keystore.
     * @param leafCertificate
     *            The expected leaf certificate. Can be <code>null</code> if any valid certificate
     *            with a trusted chain is expected, this argument can be <code>null</code>.
     * @return An array of <code>TrustManager</code> objects.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    public static TrustManager[] createTrustManagers(final KeyStore keystore, CertificatePojo leafCertificate) throws KeyStoreException,
            NoSuchAlgorithmException {

        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        LOG.debug("Initializing trust manager");
        X509Certificate leaf = null;
        if (leafCertificate != null) {
            leaf = getX509Certificate(leafCertificate);
        }
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        for (int i = 0; i < trustmanagers.length; i++) {
            if (trustmanagers[i] instanceof X509TrustManager) {
                trustmanagers[i] = new AuthSSLX509TrustManager((X509TrustManager) trustmanagers[i], keystore, leaf);
            }
        }
        return trustmanagers;
    }
}