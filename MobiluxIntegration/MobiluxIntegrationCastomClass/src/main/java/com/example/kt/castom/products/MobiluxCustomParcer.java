package com.example.kt.castom.products;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class MobiluxCustomParcer extends AbstractMediator {
	private String feedId = "";
	private String badTags = "";
	private String mysqlUrl = "";
    private String mysqlUsername = "";
    private String mysqlPassword = "";
    private String mysqlDriver = "";

	@Override
	public boolean mediate(MessageContext arg0) {
		try {
			productValues();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	 @SuppressWarnings("rawtypes")
	public void productValues() throws Exception {
        Class.forName(mysqlDriver).getDeclaredConstructor();
        Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);
        Statement statementInput = conn.createStatement();
        Statement statementOutput = conn.createStatement();

        ResultSet resultSet = statementInput.executeQuery("SELECT id, data FROM mobilux_broken_xml_table "
                                                                + "WHERE feed_id = " + feedId);

        while (resultSet.next()) {
            String offer_code = resultSet.getString(1);
            String xml = resultSet.getString(2);
            String locale = null;
            String outputQuery = "INSERT INTO `feedf_product_values`(`feed_id`, `offer_code`, `attribute_id`, `locale`, `value`) VALUES ";
            String queryValues = "";

            Iterator iterator = xmlParser(xml).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry pair = (Map.Entry)iterator.next();
                String mapKey = pair.getKey().toString();
                String mapValue = pair.getValue().toString();

                switch (mapKey) {
                    case "description_lv": locale = "lat";
                        break;
                    case "description_en": locale = "eng";
                        break;
                    case "description_et": locale = "est";
                        break;
                    case "description_ru": locale = "rus";
                        break;
                    default: locale = null;
                        break;
                }

                if (mapValue.equals(""))
                    mapValue = null;
                queryValues += "(" + feedId + ",'" + offer_code + "','" + mapKey
                                + "','" + locale + "','" + mapValue + "'),";
            }

            queryValues = queryValues.substring(0, queryValues.length() - 1);
            statementOutput.executeUpdate(outputQuery + queryValues);
        }
    }

    public HashMap<String, String> xmlParser(String data) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(data));

        HashMap<String, String> productMap = new HashMap<String, String>();

        String attributeName = new String();
        String attributeValue = new String();

        boolean bool = false;

        while (xsr.hasNext()) {
            int event = xsr.next();

            if (event == XMLStreamConstants.START_ELEMENT && !badTags.contains(xsr.getLocalName())) {
                attributeName = xsr.getLocalName();
                bool = true;
            }

            if (event == XMLStreamConstants.CHARACTERS && attributeName != null && !badTags.contains(attributeName)) {
                attributeValue = xsr.getText();
            }

            if (event == XMLStreamConstants.END_ELEMENT && !badTags.contains(xsr.getLocalName()) && bool) {
                productMap.put(attributeName, attributeValue);

                attributeName = new String();
                attributeValue = new String();

                bool = false;
            }
        }

        return productMap;
    }
	
	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}

	public void setBadTags(String badTags) {
		this.badTags = badTags;
	}
	
	public void setMysqlUrl(String mysqlUrl) {
		this.mysqlUrl = mysqlUrl;
	}

	public void setMysqlUsername(String mysqlUsername) {
		this.mysqlUsername = mysqlUsername;
	}

	public void setMysqlPassword(String mysqlPassword) {
		this.mysqlPassword = mysqlPassword;
	}

	public void setMysqlDriver(String mysqlDriver) {
		this.mysqlDriver = mysqlDriver;
	}
}
