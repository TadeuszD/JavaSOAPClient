import java.io.ByteArrayInputStream;
        import java.io.IOException;
        import java.nio.charset.Charset;

        import javax.xml.soap.MessageFactory;
        import javax.xml.soap.MimeHeaders;
        import javax.xml.soap.SOAPConnection;
        import javax.xml.soap.SOAPConnectionFactory;
        import javax.xml.soap.SOAPException;
        import javax.xml.soap.SOAPMessage;
        import javax.xml.transform.Source;
        import javax.xml.transform.Transformer;
        import javax.xml.transform.TransformerFactory;
        import javax.xml.transform.stream.StreamResult;

// Credits:
// http://stackoverflow.com/questions/19291283/soap-request-to-webservice-with-java
// http://stackoverflow.com/questions/13180372/creating-a-soapmessage-from-string-xml-of-entire-soap-message
public class HelloSoapClient {

    /**
     * Starting point for the SAAJ - SOAP Client Testing
     */
    public static void main(String args[]) {
        try {
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            //String url = "http://localhost:8080/helloService";
            //String url = "http://www.dataaccess.com/webservicesserver/";


//            String soapMessage = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:nam=\"namespace\">\r\n" +
//                    "   <soapenv:Header/>\r\n" +
//                    "   <soapenv:Body>\r\n" +
//                    "      <nam:hello>\r\n" +
//                    "         <!--Optional:-->\r\n" +
//                    "         <arg0>World</arg0>\r\n" +
//                    "      </nam:hello>\r\n" +
//                    "   </soapenv:Body>\r\n" +
//                    "</soapenv:Envelope>";

            String url = "http://www.oorsprong.org/websamples.countryinfo/CountryInfoService.wso";
            String soapMessage = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:web=\"http://www.oorsprong.org/websamples.countryinfo\">\r\n" +
                    "   <soapenv:Header/>\r\n" +
                    "   <soapenv:Body>\r\n" +
                    "   <web:CapitalCity>\r\n" +
                    "   <web:sCountryISOCode>ARG</web:sCountryISOCode>\r\n" +
                    "   </web:CapitalCity>\r\n" +
                    "   </soapenv:Body>\r\n" +
                    "   </soapenv:Envelope>";

            SOAPMessage soapResponse = soapConnection.call(getSoapMessageFromString(soapMessage), url);

            // Process the SOAP Response
            printSOAPResponse(soapResponse);

            soapConnection.close();
        } catch (Exception e) {
            System.err.println("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }
    }

    private static SOAPMessage getSoapMessageFromString(String xml) throws SOAPException, IOException {
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
        return message;
    }
    /**
     * Method used to print the SOAP Response
     */
    private static void printSOAPResponse(SOAPMessage soapResponse) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        System.out.print("\nResponse SOAP Message = ");
        StreamResult result = new StreamResult(System.out);
        transformer.transform(sourceContent, result);
    }

}