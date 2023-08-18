package com.example.kt.castom.parser;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.simple.JSONObject;

public class StAXtoMySQLParser extends AbstractMediator {

	private String getDataUrl;
	private String mainTag = ""; //xml
	private String bodyTag = ""; //good
	private String uIdTag = ""; //code
	private String mysqlUrl = "";
	private String mysqlUsername = "";
	private String mysqlPassword = "";
	private String mysqlDriver = "";
	private String mysqlCreateTableName = "defaultTable";
	private String feedId = "0";
	private String barcodesArrTag = ""; //eans
	private String barcodeTag = ""; //ean
	
	@Override
	public boolean mediate(MessageContext arg0) {
		
		System.out.println("Data url: " + getDataUrl);
		System.out.println("Main tag: " + mainTag);
		System.out.println("Body tag: " + bodyTag);
		System.out.println("Id tag: " + uIdTag);
		System.out.println("Feed Id: " + feedId);
		System.out.println("MySQL url: " + mysqlUrl);
		System.out.println("MySQL username: " + mysqlUsername);
		System.out.println("MySQL password: " + mysqlPassword);
		System.out.println("MySQL driver: " + mysqlDriver);
		System.out.println("MySQL new table name: " + mysqlCreateTableName.toLowerCase());

		try {
			staxParser();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void staxParser() throws SQLException, IOException, XMLStreamException    {
		try {
			Class.forName(mysqlDriver).getDeclaredConstructors();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);

		PreparedStatement preparedStatement;
		try {
			String tableDropQuery = "DROP TABLE `local_mysql_db`.`" + mysqlCreateTableName + "`";
			preparedStatement = conn.prepareStatement(tableDropQuery);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
		}
		try {
			String tableCreateQuery = "CREATE TABLE " + mysqlCreateTableName + " (`feed_id` varchar(255) COLLATE utf8mb4_unicode_ci NULL, `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL, `data` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL, `jsonData` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
			preparedStatement = conn.prepareStatement(tableCreateQuery);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
				
		
		
		URL url = new URL(getDataUrl);
        URLConnection uc = url.openConnection();

        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(uc.getInputStream());

        boolean bool = false;
        String productId = "";
        String str = "";
        int xmlCount = 0;
        
        String badTagName = "description";
        boolean badTagBool = true;
        
        JSONObject rootJson = new JSONObject();
        String jsonKey = "";
        String jsonValue = "";
                        
        System.out.println("Parsing start...");
        
        while (xsr.hasNext()) {
        	
        	int event = xsr.next();                        
            
        	if (event == XMLStreamConstants.START_ELEMENT && !xsr.getLocalName().equals(mainTag)) {         
            	if (uIdTag.equals(xsr.getLocalName())) {
            		bool = true;
            	}
                if (!xsr.getLocalName().equals(mainTag) && !xsr.getLocalName().equals(bodyTag)) {
                	jsonKey = xsr.getLocalName();
                }
                if (xsr.getLocalName().indexOf(badTagName) == -1) {
                	str += "<" + xsr.getLocalName() + ">";
                } else {
                	str += "<" + xsr.getLocalName() + ">";
                	badTagBool = false;
                }
            }
            
            if (event == XMLStreamConstants.CDATA) {
            	//System.out.println(xsr.getText());
            	if (badTagBool) {
            		str += xsr.getText();
            	}
            	if (bool) {
            		productId = xsr.getText();
            		bool = false;
            	}
            	jsonValue += xsr.getText();
            	//jsonValue = jsonValue.replaceAll("\\s+", "");
            	jsonValue = jsonValue.replaceAll("\n", "");
            	
                if ((jsonKey != null && !jsonKey.equals(""))) {
    	            if (rootJson.get(jsonKey) != null) {
    	            	String s = (String)rootJson.get(jsonKey);
    	            	s += "," + jsonValue;
    	            	rootJson.put(jsonKey, s);
    	            	jsonKey = "";
    	                jsonValue = "";
    	            } else {
    	            	rootJson.put(jsonKey, jsonValue);
    	            	jsonKey = "";
    	                jsonValue = "";
    	            }
                } else {
                	jsonValue = "";
                }
            }
            
            if (event == XMLStreamConstants.END_ELEMENT) {
            		str += "</" + xsr.getLocalName() + ">";
            	badTagBool = true;
            	if (xsr.getLocalName().equals(bodyTag)) {
            		if (rootJson.get(barcodeTag) != null) {
	            		String rje = rootJson.get(barcodeTag).toString();
	            		rootJson.put(barcodesArrTag, rje);
	            		rootJson.remove(barcodeTag);
            		}
            		//System.out.println(productId);
            		//System.out.println(str);
            		str = str.replaceAll("&", "&amp;");
            		
            		String isertIntoDataQuery = "INSERT INTO " + mysqlCreateTableName + " (`feed_id`, `id`, `data`, `jsonData`) VALUES (?, ?, ?, ?);";
            		
					try {
						preparedStatement = conn.prepareStatement(isertIntoDataQuery);

	            		preparedStatement.setString(1, feedId);
						preparedStatement.setString(2, productId);
	            		preparedStatement.setString(3, str);
	            		preparedStatement.setString(4, rootJson.toString());
	                    preparedStatement.executeUpdate();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					xmlCount++;
            		str = "";
            		productId = "";
            		rootJson = new JSONObject();
            	}
            }
        	        	
        }
    xsr.close();
    conn.close();
    
    System.out.println("Success!");
    System.out.println("Split into " + xmlCount + " elements.");
    }

	public void setFeedId(String feedId) {
		this.feedId = feedId;
	}
	
	public void setGetDataUrl(String getDataUrl) {
		this.getDataUrl = getDataUrl;
	}
	
	public void setMainTag(String mainTag) {
		this.mainTag = mainTag;
	}

	public void setBodyTag(String bodyTag) {
		this.bodyTag = bodyTag;
	}

	public void setUIdTag(String uIdTag) {
		this.uIdTag = uIdTag;
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
	
	public void setMysqlCreateTableName(String mysqlCreateTableName) {
		this.mysqlCreateTableName = mysqlCreateTableName;
	}

	public void setBarcodesArrTag(String barcodesArrTag) {
		this.barcodesArrTag = barcodesArrTag;
	}
	
	public void setBarcodeTag(String barcodeTag) {
		this.barcodeTag = barcodeTag;
	}

}