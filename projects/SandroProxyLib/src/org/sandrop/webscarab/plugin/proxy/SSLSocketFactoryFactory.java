/***********************************************************************
 *
 * This file is part of SandroProxy, 
 * For details, please see http://code.google.com/p/sandrop/
 *
 * Copyright (c) 2012 supp.sandrob@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Getting Source
 * ==============
 *
 * Source for this application is maintained at
 * http://code.google.com/p/sandrop/
 *
 * Software is build from sources of WebScarab project
 * For details, please see http://www.sourceforge.net/projects/owasp
 *
 */

package org.sandrop.webscarab.plugin.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.sandrob.bouncycastle.asn1.ASN1Encodable;
import org.sandrob.bouncycastle.asn1.ASN1EncodableVector;
import org.sandrob.bouncycastle.asn1.ASN1Sequence;
import org.sandrob.bouncycastle.asn1.DEREncodableVector;
import org.sandrob.bouncycastle.asn1.DERObjectIdentifier;
import org.sandrob.bouncycastle.asn1.DEROctetString;
import org.sandrob.bouncycastle.asn1.DEROutputStream;
import org.sandrob.bouncycastle.asn1.DERSequence;
import org.sandrob.bouncycastle.asn1.x509.BasicConstraints;
import org.sandrob.bouncycastle.asn1.x509.GeneralName;
import org.sandrob.bouncycastle.asn1.x509.GeneralNames;
import org.sandrob.bouncycastle.asn1.x509.X509Extension;
import org.sandrob.bouncycastle.asn1.x509.X509Extensions;
import org.sandrob.bouncycastle.asn1.x509.X509Name;
import org.sandrob.bouncycastle.util.Arrays;
import org.sandrob.bouncycastle.x509.X509V3CertificateGenerator;
import org.sandrob.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.sandrob.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.sandroproxy.constants.Constants;

public class SSLSocketFactoryFactory {

    private static final long DEFAULT_VALIDITY = 10L * 365L * 24L * 60L * 60L
            * 1000L;

    private static Logger _logger = Logger
            .getLogger(SSLSocketFactoryFactory.class.getName());

    private static final String CA = "CA";
    private static X500Principal CA_NAME;

    static {
        try {
            
            //CA_NAME = new X500Principal("cn=OWASP Custom CA for "
            //        + java.net.InetAddress.getLocalHost().getHostName()
            //        + " at " + new Date()
            //        + ",ou=OWASP Custom CA,o=OWASP,l=OWASP,st=OWASP,c=OWASP");
            
            CA_NAME = new X500Principal("cn=SandroProxy Custom CA"
                    + ",ou=SandroProxy Custom CA,o=SandroProxy,l=SandroProxy,st=SandroProxy,c=SandroProxy");
            _logger.setLevel(Level.FINEST);
        } catch (Exception ex) {
            ex.printStackTrace();
            CA_NAME = null;
        }
    }

    private PrivateKey caKey;

    private X509Certificate[] caCerts;

    private String filenameCA;
    private String filenameCert;

    private KeyStore keystoreCert;
    private KeyStore keystoreCA;

    private char[] passwordCA;
    private char[] passwordCerts;

    private boolean reuseKeys = false;

    private Map<String, SSLContext> contextCache = new HashMap<String, SSLContext>();

    private Set<BigInteger> serials = new HashSet<BigInteger>();

    public SSLSocketFactoryFactory(String fileNameCA, String fileNameCert, String type,
            char[] password)
            throws GeneralSecurityException, IOException {
        _logger.setLevel(Level.FINEST);
        this.filenameCA = fileNameCA;
        this.passwordCA = password;
        this.passwordCerts = password;
        this.filenameCert = fileNameCert;
        boolean haveNewCA = false;
        String keyStoreProvider = "BC";
        keystoreCA = KeyStore.getInstance(type, keyStoreProvider);
        File fileCA = new File(filenameCA);
        if (filenameCA == null) {
            _logger.info("No keystore provided, keys and certificates will be transient!");
        }
        String caAliasValue = "";
        // ca stuff
        if (fileCA.exists() && fileCA.canRead()) {
            _logger.fine("Loading keys from " + filenameCA);
            InputStream is = new FileInputStream(fileCA);
            keystoreCA.load(is, passwordCA);
            is.close();
            String storeAlias;
            Enumeration<String> enAliases = keystoreCA.aliases();
            Date lastStoredAliasDate = null;
            // it should be just one 
            while(enAliases.hasMoreElements()){
                storeAlias = enAliases.nextElement();
                Date lastStoredDate = keystoreCA.getCreationDate(storeAlias);
                if (lastStoredAliasDate == null || lastStoredDate.after(lastStoredAliasDate)){
                    lastStoredAliasDate = lastStoredDate;
                    caAliasValue = storeAlias;
                }
            }
            caKey = (PrivateKey) keystoreCA.getKey(caAliasValue, passwordCA);
            if (caKey == null) {
                _logger.warning("Keystore does not contain an entry for '" + caAliasValue
                        + "'");
            }
            caCerts = cast(keystoreCA.getCertificateChain(caAliasValue));
        } else {
            _logger.info("Generating CA key");
            keystoreCA.load(null, passwordCA);
            generateCA(CA_NAME);
            haveNewCA = true;
            saveKeystore(keystoreCA, filenameCA, passwordCA);
            caAliasValue = keystoreCA.aliases().nextElement();
        }
        // store ca cert to be used for export
        {
            FileOutputStream fos = null;
            try{
                X509Certificate caCert = (X509Certificate) keystoreCA.getCertificate(caAliasValue);
                byte[] caByteArray = caCert.getEncoded();
                String exportFilename = filenameCA + Constants.CA_FILE_EXPORT_POSTFIX;
                fos = new FileOutputStream(exportFilename);
                fos.write(caByteArray);
                fos.close();
                _logger.fine("CA cert exported to " + exportFilename);
            }catch (Exception ex){
                ex.printStackTrace();
                if (fos != null){
                    fos.close();
                }
            }
        }
        // cert stuff
        File fileCert = new File(filenameCert);
        if (haveNewCA || fileCert == null || !fileCert.exists()){
            keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
            keystoreCert.load(null, passwordCerts);
            saveKeystore(keystoreCert, filenameCert, passwordCerts);
        }else{
            InputStream is = new FileInputStream(fileCert);
            try{
                keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
                keystoreCert.load(is, passwordCerts);
            }catch(Exception ex){
                // problems opening exisiting so we create new one
                _logger.fine("problems opening exisiting cert keystore so we create new one");
                keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
                keystoreCert.load(null, passwordCerts);
                saveKeystore(keystoreCert, filenameCert, passwordCerts);
            }
            is.close();
            initSerials();
        }
    }

    /**
     * Determines whether the public and private key generated for the CA will
     * be reused for other hosts as well.
     * 
     * This is mostly just a performance optimisation, to save time generating a
     * key pair for each host. Paranoid clients may have an issue with this, in
     * theory.
     * 
     * @param reuse
     *            true to reuse the CA key pair, false to generate a new key
     *            pair for each host
     */
    public void setReuseKeys(boolean reuse) {
        reuseKeys = reuse;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.owasp.proxy.daemon.CertificateProvider#getSocketFactory(java.lang
     * .String, int)
     */
    public synchronized SSLSocketFactory getSocketFactory(SiteData hostData)
            throws IOException, GeneralSecurityException {
        SSLContext sslContext = (SSLContext) contextCache.get(hostData.name);
        if (sslContext == null) {
            X509KeyManager km;
            if (!keystoreCert.containsAlias(hostData.name)) {
                km = createKeyMaterial(hostData);
            } else {
                km = loadKeyMaterial(hostData);
            }
            
            // here, trust managers is a single trust-all manager
            TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkClientTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                            }
                        }
                    }

                    public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkServerTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkServerTrusted:" + certs[i]);
                            }
                        }
                    }
                }
            };
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[] { km }, trustManagers, null);
            // sslcontext.init(new KeyManager[] { km }, null, null);
            contextCache.put(hostData.name, sslContext);
        }
        return sslContext.getSocketFactory();
    }

    private X509Certificate[] cast(Certificate[] chain) {
        X509Certificate[] certs = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            certs[i] = (X509Certificate) chain[i];
        }
        return certs;
    }
    
    private X509KeyManager loadKeyMaterial(SiteData hostData) throws GeneralSecurityException, IOException {
        X509Certificate[] certs = null;
        Certificate[] chain = keystoreCert.getCertificateChain(hostData.name);
        if (chain != null) {
            certs = cast(chain);
        } else {
            throw new GeneralSecurityException(
                    "Internal error: certificate chain for " + hostData.name
                            + " not found!");
        }

        PrivateKey pk = (PrivateKey) keystoreCert.getKey(hostData.name, passwordCerts);
        if (pk == null) {
            throw new GeneralSecurityException(
                    "Internal error: private key for " + hostData.name + " not found!");
        }
        return new HostKeyManager(hostData, pk, certs);
    }

    private void saveKeystore(KeyStore keystore, String filename, char[] password) {
        if (filename == null)
            return;
        try {
            OutputStream out = new FileOutputStream(filename);
            keystore.store(out, password);
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
        }
    }

    private void generateCA(X500Principal caName)
            throws GeneralSecurityException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair caPair = keyGen.generateKeyPair();
        caKey = caPair.getPrivate();
        PublicKey caPubKey = caPair.getPublic();
        Date begin = new Date();
        Date ends = new Date(begin.getTime() + DEFAULT_VALIDITY);

        
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        // X509v3CertificateBuilder   certGen = new X509v3CertificateBuilder();
        certGen.setSerialNumber(BigInteger.ONE);
        certGen.setIssuerDN(caName);
        certGen.setNotBefore(begin);
        certGen.setNotAfter(ends);
        certGen.setSubjectDN(caName);
        certGen.setPublicKey(caPubKey);
        certGen.setSignatureAlgorithm("SHA256withRSA");
        BasicConstraints bc = new BasicConstraints(true);
        certGen.addExtension(new DERObjectIdentifier("2.5.29.19"), true, bc.toASN1Object().getEncoded());
        X509Certificate cert = certGen.generate(caKey, "BC");

        caCerts = new X509Certificate[] { cert };

        keystoreCA.setKeyEntry(CA, caKey, passwordCA, caCerts);
    }

    private void initSerials() throws GeneralSecurityException {
        Enumeration<String> e = keystoreCert.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate cert = (X509Certificate) keystoreCert
                    .getCertificate(alias);
            BigInteger serial = cert.getSerialNumber();
            serials.add(serial);
        }
    }

    protected X500Principal getSubjectPrincipal(String host) {
        return new X500Principal("cn=" + host + ",ou=UNTRUSTED SandroProxy,o=UNTRUSTED SandroProxy");
    }

    protected BigInteger getNextSerialNo() {
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        while (serials.contains(serial))
            serial.add(BigInteger.ONE);
        serials.add(serial);
        return serial;
    }

    private X509KeyManager createKeyMaterial(SiteData hostData)
            throws GeneralSecurityException {
        KeyPair keyPair;

        if (reuseKeys) {
            keyPair = new KeyPair(caCerts[0].getPublicKey(), caKey);
        } else {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(1024);
            keyPair = keygen.generateKeyPair();
        }

        X500Principal subject = getSubjectPrincipal(hostData.name);
        Date begin = new Date();
        Date ends = new Date(begin.getTime() + DEFAULT_VALIDITY);
        
        
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(getNextSerialNo());
        certGen.setIssuerDN(caCerts[0].getSubjectX500Principal());
        certGen.setNotBefore(begin);
        certGen.setNotAfter(ends);
        certGen.setSubjectDN(subject);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256withRSA");
        
        // TODO is this is a wildcard cert check database for alternative names
        
        // generate alternative names
        if (hostData.certs != null && hostData.certs.length > 0){
            Collection<List<?>> coll = hostData.certs[0].getSubjectAlternativeNames();
            if (coll != null && coll.size() > 0){
                Iterator<List<?>> iter = coll.iterator();
                final int SUBALTNAME_DNSNAME = 2;
                DEREncodableVector derVector = new ASN1EncodableVector();
                while (iter.hasNext()) {
                    List<?> next = (List<?>) iter.next();
                    int OID = ((Integer) next.get(0)).intValue();
                    switch (OID) {
                        case SUBALTNAME_DNSNAME:
                            GeneralName gn = new GeneralName(GeneralName.dNSName, (String) next.get(1));
                            derVector.add(gn);
                            break;
                    }
                }
                DERSequence sequence = new DERSequence((ASN1EncodableVector)derVector);
                GeneralNames subjectAltName = new GeneralNames(sequence);
                certGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
            }
        }
        
        // TODO store alternative names for this wildcard in database
        
        
//        if (hostData.alternativeNames != null && hostData.alternativeNames.size() > 0){
//            DEREncodableVector derVector = new ASN1EncodableVector();
//            GeneralName[] san = new GeneralName[hostData.alternativeNames.size()];
//            for(int i = 0; i < san.length ; i++){
//                san[i] = new GeneralName(GeneralName.dNSName, hostData.alternativeNames.get(i));
//                derVector.add(san[i]);
//            }
//            DERSequence sequence = new DERSequence((ASN1EncodableVector)derVector);
//            GeneralNames subjectAltName = new GeneralNames(sequence);
//            certGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
//            
////            for (String alternativeName : hostData.alternativeNames) {
////                GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, alternativeName));
////                certGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName); 
////            }
//        }
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                              new AuthorityKeyIdentifierStructure(caCerts[0]));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                             new SubjectKeyIdentifierStructure(keyPair.getPublic()));
        X509Certificate cert = certGen.generate(caKey, "BC");

        X509Certificate[] chain = new X509Certificate[caCerts.length + 1];
        System.arraycopy(caCerts, 0, chain, 1, caCerts.length);
        chain[0] = cert;

        PrivateKey pk = keyPair.getPrivate();

        keystoreCert.setKeyEntry(hostData.name, pk, passwordCerts, chain);
        saveKeystore(keystoreCert, filenameCert, passwordCerts);
        return new HostKeyManager(hostData, pk, chain);
    }

    private class HostKeyManager implements X509KeyManager {

        private SiteData hostData;

        private PrivateKey pk;

        private X509Certificate[] certs;

        public HostKeyManager(SiteData hostData, PrivateKey pk,
                X509Certificate[] certs) {
            this.hostData = hostData;
            this.pk = pk;
            this.certs = certs;
        }

        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                Socket socket) {
            return null;
            // throw new UnsupportedOperationException("Not implemented");
        }

        public String chooseServerAlias(String keyType, Principal[] issuers,
                Socket socket) {
            return hostData.name;
        }

        public X509Certificate[] getCertificateChain(String alias) {
            return certs;
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
            //throw new UnsupportedOperationException("Not implemented");
        }

        public PrivateKey getPrivateKey(String alias) {
            return pk;
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {
            
//            if (hostData.alternativeNames == null || hostData.alternativeNames.size() == 0){
//                return new String[]{hostData.name};
//            }
//            
//            return (String[]) hostData.alternativeNames.toArray();
            return new String[]{hostData.name};
        }

    }

}
