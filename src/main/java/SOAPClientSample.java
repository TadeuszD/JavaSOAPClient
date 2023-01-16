
/*      RevenueGitHubAdmin/SOAPClientSample.java
        * Gists provided for illustrative purposes only. Developers can use these as a support tool
        * but the Office of the Revenue Commissioners (Revenue) does not provide any warranty with
        * these gists.
        * https://gist.github.com/RevenueGitHubAdmin/2bc2f593040b6f97c0002b5718063fb5
        */

import java.util.Base64;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


public class SOAPClientSample {
    

    /*
    Gists provided for illustrative purposes only. Developers can use these as a support tool
    but the Office of the Revenue Commissioners (Revenue) does not provide any warranty with 
    these gists. 
	
    In this example we will perform the following steps
	
    1.Read in an XML file and generate a DOM Document object. The input file should conform to the CoC schema.
    2.Wrap the DOM Document in a SOAP Envelope.
    3.Sign the SOAP Envelope.
    4.Output the generated SOAP Message to a file.
    5.Send message to the web service endpoint.
     */


    public static void main(String[] args) throws Exception {
        SOAPClientSample client = new SOAPClientSample();
        System.setProperty("javax.xml.soap.MessageFactory", "com.sun.xml.internal.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl");
        System.setProperty("javax.xml.bind.JAXBContext", "com.sun.xml.internal.bind.v2.ContextFactory");

        //uncomment this only when using a proxy and set correct values
        //System.setProperty("https.proxyHost","host");
        //System.setProperty("https.proxyPort","port");

        Document doc = client.readInXMLFile();
        SOAPMessage msg = client.createSOAPEnvelope(doc);
        msg = client.signSOAPMessage(msg);
        client.outputSOAPMessageToFile(msg);
        client.callTheWebServiceFromFile();

    }



    /*
    Sample request file
   <rpn:LookupRPNRequest xmlns:rpn="http://www.ros.ie/schemas/paye-employers/v1/rpn/">
	<rpn:EmployerRegistrationNumber>8000384SH</rpn:EmployerRegistrationNumber>
	<rpn:SoftwareUsed>
		<rpn:Name>SOAP UI</rpn:Name>
		<rpn:Version>1</rpn:Version>
	</rpn:SoftwareUsed>
	<rpn:TaxYear>2019</rpn:TaxYear>
</rpn:LookupRPNRequest>
     */
    private Document readInXMLFile() throws ParserConfigurationException,
            SAXException, IOException

    {

        File requestFile = new File("C:\\projects\\requests\\SOAP\\LOOKUP-RPN1.xml");
        javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(requestFile);

        return doc;
    }


    private SOAPMessage createSOAPEnvelope(Document xmlDocument)
            throws SOAPException {

        // Create SOAP Message
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();


        // Add DOM object to SOAP body
        SOAPBody soapBody = soapMessage.getSOAPBody();
        soapBody.addDocument(xmlDocument);
        soapBody.addAttribute(soapEnvelope.createName("Id", "wsu",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), "Body");

        return soapMessage;


    }


    private SOAPMessage signSOAPMessage(SOAPMessage soapMessage) throws Exception {
        // Create the security element
        SOAPElement soapHeader = soapMessage.getSOAPHeader();

        SOAPElement securityElement = soapHeader.addChildElement("Security",
                "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");


        securityElement.addNamespaceDeclaration("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");


        // (i) Extract the certificate from the .p12 file.
        java.security.cert.Certificate cert = getCertificate();

        // (ii) Add Binary Security Token. The base64 encoded value of the ROS digital certificate.
        addBinarySecurityToken(securityElement, cert);


        //(iii) Add Timestamp element
        SOAPElement timestamp = addTimestamp(securityElement, soapMessage);

        // (iv) Add signature element
        addSignature(securityElement, soapMessage.getSOAPBody(), timestamp);

        return soapMessage;


    }

    private SOAPElement addTimestamp(SOAPElement securityElement, SOAPMessage soapMessage) throws SOAPException {
        SOAPElement timestamp = securityElement.addChildElement("Timestamp", "wsu");


        SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();

        timestamp.addAttribute(soapEnvelope.createName("Id", "wsu",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"), "TS");

        String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        DateTimeFormatter timeStampFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);


        timestamp.addChildElement("Created", "wsu").setValue(timeStampFormatter.format(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"))));
        timestamp.addChildElement("Expires", "wsu").setValue(timeStampFormatter.format(ZonedDateTime.now().plusSeconds(30).toInstant().atZone(ZoneId.of("UTC"))));

        return timestamp;

    }


    private java.security.cert.Certificate getCertificate() throws Exception {

        String password = "password";


        // (i) Get byte array of password
        byte[] passwordByte = password.getBytes();

        // (ii) Get MD5 Hash of byte array
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        byte[] passwordHashed = digest.digest(passwordByte);

        // (iii) Base64 encode hashed byte array
        String passwordHashedbase64 = Base64.getEncoder().encodeToString(passwordHashed);

        // (iv) Open the Seat using KeyStore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(new File("C:\\projects\\certificates\\999963110.p12")), passwordHashedbase64.toCharArray());

        // (v) Extract the certificate.
        java.security.cert.Certificate cert = keystore.getCertificate("paye-employers-999963110");

        return cert;


    }


    private SOAPElement addBinarySecurityToken(SOAPElement securityElement,
                                               java.security.cert.Certificate cert) throws Exception {

        // Get byte array of cert.
        byte[] certByte = cert.getEncoded();

        // Add the Binary Security Token element
        SOAPElement binarySecurityToken = securityElement.addChildElement("BinarySecurityToken", "wsse");

        binarySecurityToken.setAttribute("ValueType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");
        binarySecurityToken.setAttribute("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
        binarySecurityToken.setAttribute("wsu:Id", "X509Token");
        binarySecurityToken.addTextNode(Base64.getEncoder().encodeToString(certByte));
        return securityElement;

    }


    private SOAPElement addSignature(
            SOAPElement securityElement, SOAPBody soapBody, SOAPElement timestamp) throws Exception {


        // Get private key from ROS digital certificate
        PrivateKey key = getKeyFormCert();


        SOAPElement securityTokenReference = addSecurityToken(securityElement);

        // Add signature
        createDetachedSignature(securityElement, key, securityTokenReference, soapBody, timestamp);

        return securityElement;

    }


    private PrivateKey getKeyFormCert() throws Exception {

        String password = "password";

        // Get cert password.
        // (i) Get byte array of password
        byte[] passwordByte = password.getBytes();

        // (ii) Get MD5 Hash of byte array
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        byte[] passwordHashed = digest.digest(passwordByte);

        // (iii) Base64 encode hashed byte array
        String passwordHashedBase64 = Base64.getEncoder().encodeToString(passwordHashed);

        // (iv) Open the cert using KeyStore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(new File("C:\\projects\\certificates\\999963110.p12")), passwordHashedBase64.toCharArray());

        // (v) Extract Private Key
        PrivateKey key = (PrivateKey) keystore.getKey("paye-employers-999963110", passwordHashedBase64.toCharArray());

        return key;

    }


    private SOAPElement addSecurityToken(SOAPElement signature)
            throws SOAPException {
        SOAPElement securityTokenReference = signature.addChildElement("SecurityTokenReference", "wsse");
        SOAPElement reference = securityTokenReference.addChildElement("Reference", "wsse");

        reference.setAttribute("URI", "#X509Token");

        return securityTokenReference;
    }


    private void createDetachedSignature(SOAPElement signatureElement, PrivateKey privateKey, SOAPElement securityTokenReference, SOAPBody soapBody, SOAPElement timestamp) throws Exception {


        String providerName = System.getProperty
                ("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM",
                (Provider) Class.forName(providerName).newInstance());


        //Digest method
        javax.xml.crypto.dsig.DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha512", null);
        ArrayList<Transform> transformList = new ArrayList<Transform>();


        //Transform
        Transform envTransform = xmlSignatureFactory.newTransform("http://www.w3.org/2001/10/xml-exc-c14n#", (TransformParameterSpec) null);
        transformList.add(envTransform);


        //References

        ArrayList<Reference> refList = new ArrayList<Reference>();
        Reference refTS = xmlSignatureFactory.newReference("#TS", digestMethod, transformList, null, null);
        Reference refBody = xmlSignatureFactory.newReference("#Body", digestMethod, transformList, null, null);


        refList.add(refBody);
        refList.add(refTS);


        javax.xml.crypto.dsig.CanonicalizationMethod cm = xmlSignatureFactory.newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#",
                (C14NMethodParameterSpec) null);

        javax.xml.crypto.dsig.SignatureMethod sm = xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", null);
        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(cm, sm, refList);


        DOMSignContext signContext = new DOMSignContext(privateKey, signatureElement);
        signContext.setDefaultNamespacePrefix("ds");
        signContext.putNamespacePrefix("http://www.w3.org/2000/09/xmldsig#", "ds");


        //These are required for new Java versions
        signContext.setIdAttributeNS
                (soapBody,
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id");


        signContext.setIdAttributeNS
                (timestamp,
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id");


        KeyInfoFactory keyFactory = KeyInfoFactory.getInstance();
        DOMStructure domKeyInfo = new DOMStructure(securityTokenReference);
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyFactory.newKeyInfo(java.util.Collections.singletonList(domKeyInfo));
        javax.xml.crypto.dsig.XMLSignature signature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
        signContext.setBaseURI("");


        signature.sign(signContext);

    }


    /*
    Sample signed SOAP message

    The signature value depends on
    1) Time when it was generated.
    2) Request file
    3) Certificate

    Therefore, the envelope can be different from expected.

 <env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
	<env:Header>
		<wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
			<wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary" ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3" wsu:Id="X509Token">MIIEmDCCA4CgAwIBAgIQWqFOg5Xcex9Y/6mNfeipuDANBgkqhkiG9w0BAQsFADBpMQswCQYDVQQGEwJJRTEeMBwGA1UEChMVUmV2ZW51ZSBDb21taXNzaW9uZXJzMSAwHgYDVQQLExdSZXZlbnVlIE9uLUxpbmUgU2VydmljZTEYMBYGA1UEAxMPREVWIFJPUyBDQSAyMDIxMB4XDTE4MDMyODA4Mjk1N1oXDTIyMDYwNzE3MzExMVowPzELMAkGA1UEBhMCSUUxDTALBgNVBAoTBFRFU1QxEjAQBgNVBAsTCTk5OTk2MzExMDENMAsGA1UEAxMEVEVTVDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL8VLnVrMl8hk09qlksCXqTEUiK2w1FgniLu51jkQ7JOlescCJzRVmHJsmZYVYMEWivdtPm6geBsz+iNe197I9iqHMuqsO2OUrH/RepW9rj9Y0Knz81VgZwA12KLGmr1oxX0oStejk1q19W5oEHt1103H8AhWIp+ZZx3AQo7TIxD5gRseEIbOu7MNmTL0kjxi4P43Vg3Nr8Jwwdm7BiBIGTTgV6Xnz5BOm7Q5Gqd3B7/HogTdRYit3XzrzCOExC4c4jzuLixjynTKnrMaZiqO91cbkPCrSjpY9Tol390TC4oti8K+ODXK9A1UxKeixyDq9CuD7ccAaQNUp7Tkgda5UsCAwEAAaOCAWQwggFgMB8GA1UdIwQYMBaAFKEt5xi09serBBpMDFevig/M9P/wMIIBDwYDVR0gBIIBBjCCAQIwgf8GCyqCdLvoIwEBAQEBMIHvMBoGCCsGAQUFBwIBFg53d3cucmV2ZW51ZS5pZTCB0AYIKwYBBQUHAgIwgcMagcBDZXJ0aWZpY2F0ZXMgaXNzdWVkIHVuZGVyIHRoaXMgQ1AgYXJlIHF1YWxpZmllZCBjZXJ0aWZpY2F0ZXMgdW5kZXIgdGhlIEVsZWN0cm9uaWMgQ29tbWVyY2UgQWN0IDIwMDAgZm9yIHVzZSBieSBBcHByb3ZlZCBvciBBdXRob3Jpc2VkIHBlcnNvbnMgb25seSB0byBjb21tdW5pY2F0ZSB3aXRoIHRoZSBSZXZlbnVlIENvbW1pc3Npb25lcnMwCwYDVR0PBAQDAgbAMB0GA1UdDgQWBBTS91SeurUDG788GWZt/cJhgTVL4jANBgkqhkiG9w0BAQsFAAOCAQEAEGjWKAnpLDDtXR+G8Wq5KGZ6Nxnehv3nVtc/iEs+tw5EeChU9beCEmgLF1vV3QFCfil8ta4k0uB8ZFUKIbMhF4JHXTJaMo39e8kq+UGReAgJxHfw5YGcngwGBLhZV1ut+1HKgbWLCUnji7zOOMSkv2flkmubOqsxIgYrvcSw24d0/o+pyN9PKgRYRcizbBFjthQX1VzeF32dvprorXUqEQ+3HcbsvIBdSjF7IbwECO6iABir2aKdhCQm+poVPD6SmOzRoHggrvniAmUKy3czQKKYiG1cyoJZ/6u4o3naTWn/Q/egZGH9qTLM2eiQzYQ2otViw45HSPtuyUEsfpdBqQ==</wsse:BinarySecurityToken>
			<wsu:Timestamp wsu:Id="TS">
				<wsu:Created>2018-09-05T13:57:41.449Z</wsu:Created>
				<wsu:Expires>2018-09-05T13:58:11.462Z</wsu:Expires>
			</wsu:Timestamp>
			<ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
				<ds:SignedInfo>
					<ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
					<ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha512"/>
					<ds:Reference URI="#Body">
						<ds:Transforms>
							<ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
						</ds:Transforms>
						<ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
						<ds:DigestValue>+I/W7eTNgtLlOtxazFh+vjAG+MCUET2+LJ2yu3+iP4A6yJznTkdQiJ34onWE8SYAoZUYD2MKvsHt
Wz++X9wzEA==</ds:DigestValue>
					</ds:Reference>
					<ds:Reference URI="#TS">
						<ds:Transforms>
							<ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
						</ds:Transforms>
						<ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha512"/>
						<ds:DigestValue>Jr/9zWaQ0Y7VBQE9OM6HPwzX+AD7tNnAiJFYqRdF0E+b40c9JZt/f4w0x25v9kKMszAovhgmbEQc
1ZrVzG5d2Q==</ds:DigestValue>
					</ds:Reference>
				</ds:SignedInfo>
				<ds:SignatureValue>GPBxhqQ6aBcIQDpphqboZsTJYjbM6FJcnpDckUjNymp4MZZpPpasn1eo0CWKOM3GxqKh1BiNt586
YIPrDne6gkyYtadRiLMKQKFwWpQDvjGTtjfLfi5EHzTyD2OhAH+VswOvsNraLlDC8Ph4qc6rpCu2
HwiRW/vl+8a2v3pyL/c=</ds:SignatureValue>
				<ds:KeyInfo>
					<wsse:SecurityTokenReference>
						<wsse:Reference URI="#X509Token"/>
					</wsse:SecurityTokenReference>
				</ds:KeyInfo>
			</ds:Signature>
		</wsse:Security>
	</env:Header>
	<env:Body xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="Body">
		<rpn:LookupRPNRequest xmlns:rpn="http://www.ros.ie/schemas/paye-employers/v1/rpn/">
			<rpn:EmployerRegistrationNumber>8001259UH</rpn:EmployerRegistrationNumber>
			<rpn:SoftwareUsed>
				<rpn:Name>SOAP UI</rpn:Name>
				<rpn:Version>1</rpn:Version>
			</rpn:SoftwareUsed>
			<rpn:TaxYear>2019</rpn:TaxYear>
		</rpn:LookupRPNRequest>
	</env:Body>
</env:Envelope>

     */


    private void outputSOAPMessageToFile(SOAPMessage soapMessage)
            throws SOAPException, IOException {

        File outputFile = new File("C:\\projects\\output.soap");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
        soapMessage.writeTo(fos);
        fos.close();

    }


    private void callTheWebServiceFromFile() throws IOException, SOAPException {


        //load the soap request file

        File soapFile = new File("C:\\projects\\output.soap");
        FileInputStream fis = new FileInputStream(soapFile);
        javax.xml.transform.stream.StreamSource ss = new javax.xml.transform.stream.StreamSource(fis);

        // Create a SOAP Message Object

        SOAPMessage msg = MessageFactory.newInstance().createMessage();
        SOAPPart soapPart = msg.getSOAPPart();


        // Set the soapPart Content with the stream source
        soapPart.setContent(ss);

        // Create a webService connection

        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();


        // Invoke the webService.

        String soapEndpointUrl = "https://softwaretest.ros.ie/paye-employers/v1/soap";
        SOAPMessage resp = soapConnection.call(msg, soapEndpointUrl);

        // Reading result
        resp.writeTo(System.out);

        fis.close();
        soapConnection.close();


    }
}