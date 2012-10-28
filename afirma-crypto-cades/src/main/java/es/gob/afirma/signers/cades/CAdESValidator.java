/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.signers.cades;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.EncryptedContentInfo;
import org.bouncycastle.asn1.cms.EnvelopedData;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.signers.pkcs7.DigestedData;
import es.gob.afirma.signers.pkcs7.SignedAndEnvelopedData;

/** Agrupa distintos m&eacute;todos de verificaci&oacute;n estructural de datos CAdES. Es importante rese&ntilde;ar que las
 * validaciones son &uacute;nicamente a nivel de estructura, y no a nivel de validez de la propia firma electr&oacute;ca o
 * los firmantes. */
public final class CAdESValidator {

    private static final Logger LOGGER = Logger.getLogger("es.gob.afima"); //$NON-NLS-1$

    private CAdESValidator() {
        // No permitimos la instanciacion
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>Data</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>Data</i>,
     * <code>false</code> en caso contrario. */
    @SuppressWarnings("unused")
	static boolean isCAdESData(final byte[] data) throws IOException {

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
    	final ASN1InputStream is = new ASN1InputStream(data);
    	final Enumeration<?> e;
    	try {
    		e = ((ASN1Sequence) is.readObject()).getObjects();
    	}
    	catch(final ClassCastException ex) {
        	// No es una secuencia
        	return false;
        }
        finally {
        	is.close();
        }

        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (!doi.equals(PKCSObjectIdentifiers.data)) {
            return false;
        }
        // Contenido de Data
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();

        try {
            /* Los valores de retorno no se usan, solo es para verificar que la
             * conversion ha sido correcta. De no ser asi, se pasaria al manejo
             * de la excepcion.
             */
            new DEROctetString(doj.getObject());

        }
        catch (final Exception ex) {
        	LOGGER.info("Lo datos proporcionados no son de tipo CAdESData: " + ex); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>SignedData</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>SignedData</i>,
     * <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas leyendo los datos */
    public static boolean isCAdESSignedData(final byte[] data) throws IOException {
        boolean isValid = false;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
    	final ASN1InputStream is = new ASN1InputStream(data);
    	final ASN1Sequence dsq;
    	try {
    		dsq = (ASN1Sequence) is.readObject();
    	}
    	catch(final ClassCastException e) {
    		// No es una secuencia
    		return false;
    	}
    	finally {
    		is.close();
    	}
        final Enumeration<?> e = dsq.getObjects();
        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (doi.equals(PKCSObjectIdentifiers.signedData)) {
            isValid = true;
        }
        // Contenido de SignedData
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();
        final ASN1Sequence datos = (ASN1Sequence) doj.getObject();
        final SignedData sd = SignedData.getInstance(datos);

        final ASN1Set signerInfosSd = sd.getSignerInfos();

        for (int i = 0; i < signerInfosSd.size(); i++) {
            final SignerInfo si = new SignerInfo((ASN1Sequence) signerInfosSd.getObjectAt(i));
            isValid = verifySignerInfo(si);
        }

        return isValid;
    }

    /** Verifica que los <code>SignerInfos</code> tengan el par&aacute;metro
     * que identifica que los datos son de tipo CAdES.
     * @param si <code>SignerInfo</code> para la verificaci&oacute;n del par&aacute;metro
     *        adecuado.
     * @return si contiene el par&aacute;metro. */
    private static boolean verifySignerInfo(final SignerInfo si) {
        boolean isSignerValid = false;
        final ASN1Set attrib = si.getAuthenticatedAttributes();
        final Enumeration<?> e = attrib.getObjects();
        Attribute atribute;
        while (e.hasMoreElements()) {
        	final ASN1Sequence seq = (ASN1Sequence) e.nextElement();
            atribute = new Attribute(
        		(ASN1ObjectIdentifier)seq.getObjectAt(0),
        		(ASN1Set)seq.getObjectAt(1)
    		);
            // si tiene la pol&iacute;tica es CADES.
            if (atribute.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signingCertificate) ||
                atribute.getAttrType().equals(PKCSObjectIdentifiers.id_aa_signingCertificateV2)) {
                	isSignerValid = true;
            }
        }
        return isSignerValid;
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>DigestedData</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>DigestedData</i>,
     * <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas relacionados con la lectura de los datos */
    @SuppressWarnings("unused")
	static boolean isCAdESDigestedData(final byte[] data) throws IOException {
        boolean isValid = false;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
        final ASN1InputStream is = new ASN1InputStream(data);
        final ASN1Sequence dsq;
        try {
        	dsq = (ASN1Sequence) is.readObject();
        }
        catch(final ClassCastException e) {
        	// No es una secuencia
        	return false;
        }
        finally {
        	is.close();
        }
        final Enumeration<?> e = dsq.getObjects();
        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (doi.equals(PKCSObjectIdentifiers.digestedData)) {
            isValid = true;
        }
        // Contenido de Data
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();

        try {
            /* Los resultados no se usan, solo es para verificar que la
             * conversion ha sido correcta. De no ser asi, se pasaria al manejo
             * de la excepcion. */
            new DigestedData((ASN1Sequence) doj.getObject());

        }
        catch (final Exception ex) {
        	LOGGER.info("Los datos proporcionados no son de tipo CAdESDigestedData: " + ex); //$NON-NLS-1$
            return false;
        }

        return isValid;
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>EncryptedData</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>EncryptedData</i>,
     * <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas relacionados con la lectura de los datos */
    static boolean isCAdESEncryptedData(final byte[] data) throws IOException {
        boolean isValid = false;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
        final ASN1InputStream is = new ASN1InputStream(data);
        final ASN1Sequence dsq;
        try {
        	dsq = (ASN1Sequence) is.readObject();
        }
        catch(final ClassCastException e) {
        	// No es una secuencia
        	return false;
        }
        finally {
        	is.close();
        }
        final Enumeration<?> e = dsq.getObjects();

        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (doi.equals(PKCSObjectIdentifiers.encryptedData)) {
            isValid = true;
        }
        // Contenido de Data
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();

        final ASN1Sequence asq = (ASN1Sequence) doj.getObject();

        try {

            /* Los resultados de las llamadas no se usan, solo es para verificar que la
             * conversion ha sido correcta. De no ser asi, se pasaria al manejo
             * de la excepcion. */

            DERInteger.getInstance(asq.getObjectAt(0));
            EncryptedContentInfo.getInstance(asq.getObjectAt(1));

            if (asq.size() == 3) {
                asq.getObjectAt(2);
            }

        }
        catch (final Exception ex) {
        	LOGGER.info("Lo datos proporcionados no son de tipo CAdESEncryptedData: " + ex); //$NON-NLS-1$
            return false;
        }

        return isValid;
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>EnvelopedData</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>EnvelopedData</i>,
     * <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas relacionados con la lectura de los datos */
    @SuppressWarnings("unused")
	static boolean isCAdESEnvelopedData(final byte[] data) throws IOException {
        boolean isValid = false;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
        final ASN1InputStream is = new ASN1InputStream(data);
        final ASN1Sequence dsq;
        try {
        	dsq = (ASN1Sequence) is.readObject();
        }
        catch(final ClassCastException e) {
        	// No es una secuencia
        	return false;
        }
        finally {
        	is.close();
        }
        final Enumeration<?> e = dsq.getObjects();
        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (doi.equals(PKCSObjectIdentifiers.envelopedData)) {
            isValid = true;
        }
        // Contenido de Data
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();

        try {
            /* los retornos no se usan, solo es para verificar que la conversion
             * ha sido correcta. De no ser asi, se pasaria al manejo de la
             * excepcion. */
            new EnvelopedData((ASN1Sequence) doj.getObject());
        }
        catch (final Exception ex) {
        	LOGGER.info("Lo datos proporcionados no son de tipo CAdESEnvelopedData: " + ex); //$NON-NLS-1$
            return false;
        }

        return isValid;
    }

    /** Verifica si los datos proporcionados se corresponden con una estructura de tipo <i>SignedAndEnvelopedData</i>.
     * @param data Datos PKCS#7/CMS/CAdES.
     * @return <code>true</code> si los datos proporcionados se corresponden con una estructura de tipo <i>SignedAndEnvelopedData</i>,
     * <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas relacionados con la lectura de los datos */
    static boolean isCAdESSignedAndEnvelopedData(final byte[] data) throws IOException {
        boolean isValid = false;

        // LEEMOS EL FICHERO QUE NOS INTRODUCEN
        final ASN1InputStream is = new ASN1InputStream(data);
        final ASN1Sequence dsq;
        try {
        	dsq = (ASN1Sequence) is.readObject();
        }
        catch(final ClassCastException e) {
        	// No es una secuencia
        	return false;
        }
        finally {
        	is.close();
        }
        is.close();
        final Enumeration<?> e = dsq.getObjects();

        // Elementos que contienen los elementos OID Data
        final DERObjectIdentifier doi = (DERObjectIdentifier) e.nextElement();
        if (doi.equals(PKCSObjectIdentifiers.signedData)) {
            isValid = true;
        }
        // Contenido de SignedData
        final ASN1TaggedObject doj = (ASN1TaggedObject) e.nextElement();
        final ASN1Sequence datos = (ASN1Sequence) doj.getObject();
        final SignedAndEnvelopedData sd = new SignedAndEnvelopedData(datos);

        final ASN1Set signerInfosSd = sd.getSignerInfos();

        for (int i = 0; i < signerInfosSd.size(); i++) {
            final SignerInfo si = new SignerInfo((ASN1Sequence) signerInfosSd.getObjectAt(i));
            isValid = verifySignerInfo(si);
        }

        return isValid;
    }

    /** Comprueba que un archivo cumple con una estructura de tipo CAdES.
     * Se permite la verificaci&oacute;n de los siguientes tipos de estructuras:
     * <ul>
     *  <li>Data</li>
     *  <li>Signed Data</li>
     *  <li>Digested Data</li>
     *  <li>Encrypted Data</li>
     *  <li>Enveloped Data</li>
     *  <li>Signed and Enveloped Data</li>
     * </ul>
     * @param signData Datos que se desean comprobar.
     * @param type Tipo de firma o estructura CMS que se quiere verificar. Los valores aceptados son
     *             <ul>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_DATA</code></li>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_SIGNEDDATA</code></li>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_ENCRYPTEDDATA</code></li>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_ENVELOPEDDATA</code></li>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_SIGNEDANDENVELOPEDDATA</code></li>
     *              <li><code>AOSignConstants.CMS_CONTENTTYPE_DIGESTEDDATA</code></li>
     *             </ul>
     * @return <code>true</code> si los datos proporcionados se corresponden con la estructura CAdES
     *         indicada, <code>false</code> en caso contrario.
     * @throws IOException Si ocurren problemas en la lectura de la firma */
    public static boolean isCAdESValid(final byte[] signData, final String type) throws IOException {
        if (type.equals(AOSignConstants.CMS_CONTENTTYPE_DATA)) {
			return CAdESValidator.isCAdESData(signData);
        }
        else if (type.equals(AOSignConstants.CMS_CONTENTTYPE_SIGNEDDATA)) {
			return CAdESValidator.isCAdESSignedData(signData);
        }
        else if (type.equals(AOSignConstants.CMS_CONTENTTYPE_DIGESTEDDATA)) {
			return CAdESValidator.isCAdESDigestedData(signData);
        }
        else if (type.equals(AOSignConstants.CMS_CONTENTTYPE_ENCRYPTEDDATA)) {
			return CAdESValidator.isCAdESEncryptedData(signData);
        }
        else if (type.equals(AOSignConstants.CMS_CONTENTTYPE_ENVELOPEDDATA)) {
			return CAdESValidator.isCAdESEnvelopedData(signData);
        }
        else if (type.equals(AOSignConstants.CMS_CONTENTTYPE_SIGNEDANDENVELOPEDDATA)) {
			return CAdESValidator.isCAdESSignedAndEnvelopedData(signData);
        }
        LOGGER.warning("Tipo de contenido CADES no reconocido"); //$NON-NLS-1$
        return false;
    }

    /** Comprueba que un archivo cumple con una estructura de tipo CAdES.
     * Se permite la verificaci&oacute;n de los siguientes tipos de estructuras:
     * <ul>
     *  <li>Data</li>
     *  <li>Signed Data</li>
     *  <li>Digested Data</li>
     *  <li>Encrypted Data</li>
     *  <li>Enveloped Data</li>
     *  <li>Signed and Enveloped Data</li>
     * </ul>
     * @param data Datos que se desean comprobar.
     * @return <code>true</code> si los datos proporcionados se corresponden con la estructura CAdES
     *         indicada, <code>false</code> en caso contrario.
     * @throws IOException SI ocurren problemas en la lectura de los datos */
    public static boolean isCAdESValid(final byte[] data) throws IOException {
        // si se lee en el CMSDATA, el inputstream ya esta leido y en los demas
        // siempre sera nulo
        if (data == null) {
            LOGGER.warning("Se han introducido datos nulos para su comprobacion"); //$NON-NLS-1$
            return false;
        }

		// Comprobamos si su contenido es de tipo DATA
        boolean valido = CAdESValidator.isCAdESData(data);
        // Comprobamos si su contenido es de tipo SIGNEDDATA
        if (!valido) {
			valido = CAdESValidator.isCAdESSignedData(data);
        }
        // Comprobamos si su contenido es de tipo DIGESTDATA
        if (!valido) {
			valido = CAdESValidator.isCAdESDigestedData(data);
        }
        // Comprobamos si su contenido es de tipo ENCRYPTEDDATA
        if (!valido) {
			valido = CAdESValidator.isCAdESEncryptedData(data);
        }
        // Comprobamos si su contenido es de tipo ENVELOPEDDATA
        if (!valido) {
			valido = CAdESValidator.isCAdESEnvelopedData(data);
        }
        // Comprobamos si su contenido es de tipo SIGNEDANDENVELOPED
        if (!valido) {
			valido = CAdESValidator.isCAdESSignedAndEnvelopedData(data);
        }
        return valido;
    }



}
