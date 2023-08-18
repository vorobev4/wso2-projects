package com.example.kt.castom.parser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CustomParcer extends AbstractMediator {
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

        ResultSet resultSet = statementInput.executeQuery("SELECT id, jsonData FROM mobilux_broken_xml_table "
                + "WHERE feed_id = " + feedId);

        while (resultSet.next()) {
            String offer_code = resultSet.getString(1);
            String json = resultSet.getString(2);
            String locale = null;
            String outputQuery = "INSERT INTO `feedf_product_values`(`feed_id`, `offer_code`, `attribute_id`, `locale`, `value`) VALUES ";
            String queryValues = "";

            Iterator iterator = jsonParser(json).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry pair = (Map.Entry)iterator.next();
                String mapKey = pair.getKey().toString();
                String mapValue = pair.getValue().toString()
                        .replaceAll("\\\\","\\\\\\\\")
                        .replaceAll("\"", "\\\\\"")
                        .replaceAll("\'", "\\\\'");

                switch (mapKey) {
	                    case "description_lv":
	                        locale = "lat";
	                        mapKey = "description";
	                        break;
	                    case "description_lt":
	                        locale = "lit";
	                        mapKey = "description";
	                        break;
	                    case "description_en":
	                        locale = "eng";
	                        mapKey = "description";
	                        break;
	                    case "description_et":
	                        locale = "est";
	                        mapKey = "description";
	                        break;
	                    case "description_ru":
	                        locale = "rus";
	                        mapKey = "description";
	                        break;
                    default: locale = "";
                        break;
                }

                if (mapValue.equals(""))
                    mapValue = "NULL";
                else
                    mapValue = "'" + mapValue + "'";
//                if (!locale.equals("")) System.out.println(mapValue);
                queryValues += "(" + feedId + ",'" + offer_code + "','" + mapKey
                        + "','" + locale + "'," + mapValue + "),";
            }

            queryValues = queryValues.substring(0, queryValues.length() - 1);
            statementOutput.executeUpdate(outputQuery + queryValues);
        }
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, String> jsonParser(String data) throws Exception {
        HashMap<String, String> productMap = new HashMap<String, String>();
        JSONObject jsonObject = (JSONObject) new JSONParser().parse(data);
        String[] arr = new String[jsonObject.keySet().size()];
        jsonObject.keySet().toArray(arr);

        for (String str : arr) {
            if (!badTags.contains(str)) {
                String key = str;
                String value = jsonObject.get(str).toString();

                if (key.equals("product_name")) {
                    String brand = jsonObject.get("brand").toString();
                    value = value.replaceAll("(?i)" + brand + " ", "").trim().replaceAll(" +", " ");

                    if (value != null && !value.equals("")) {
                        value = value.substring(0, 1).toUpperCase() + value.substring(1);
                    }
                }
                
                productMap.put(key, value);
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
