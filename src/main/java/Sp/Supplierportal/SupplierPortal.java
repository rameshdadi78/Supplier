package Sp.Supplierportal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.plaf.synth.SynthOptionPaneUI;

import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * RESTful web service for Supplier Portal EC Part retrieval and EBOM details.
 * 
 * This service provides APIs for retrieving EC Parts data and EBOM details from
 * the database. The service reads configurations from a properties file and 
 * constructs dynamic SQL queries based on the provided query parameters.
 * The results are returned in JSON format.
 */

@Path("SupplierPortal")
public class SupplierPortal {

	/**
	 * Retrieves columns from the specified table in the database and returns them in a JSON format.
	 * 
	 * This method dynamically builds a SQL query based on the query parameters provided in the request URL.
	 * It uses properties loaded from the `sp.properties` file to map internal column names to display names.
	 * The results are grouped into "basicAttributes" and "attributes" based on the mappings defined in the properties file.
	 * 
	 * @param uriInfo Provides access to query parameters of the HTTP request.
	 * @return A JSON string containing the results of the query, with attributes mapped to display names.
	 * @throws Exception If there is an error accessing the database or processing the request.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/parts?column1=value1&column2=value2
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectId: 1": {
	 *         "basicAttributes": [
	 *           {
	 *             "displayName": "Display Name 1",
	 *             "name": "internalName1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Display Name 2",
	 *             "name": "internalName2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Display Name 3",
	 *             "name": "internalName3",
	 *             "value": "value3"
	 *           },
	 *           {
	 *             "displayName": "Display Name 4",
	 *             "name": "internalName4",
	 *             "value": "value4"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ecpartcolumns` and `ecpartbasicAttributes`.</li>
	 *   <li>The `Id` column from the database is used as the key for each object in the JSON response.</li>
	 * </ul>
	 */

	
	@GET
	@Path("parts")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllColumns(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

		// Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    String tablename = pro.getProperty("ecpartTable");

	    // Load ecpartcolumns and ecpartbasicAttributes
	    String columnsProperty = pro.getProperty("ecpartcolumns");
	    String basicAttributesProperty = pro.getProperty("ecpartbasicAttributes");

	    // Map to store the column names, display names, and internal names
	    Map<String, Map<String, String>> columnMap = new HashMap<>();
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            columnMap.put(parts[1].trim(), detailMap);
	        }
	    }

	    // Map to store the basic attribute column names, display names, and internal names
	    Map<String, Map<String, String>> basicAttributeMap = new HashMap<>();
	    for (String mapping : basicAttributesProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            basicAttributeMap.put(parts[1].trim(), detailMap);
	        }
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tablename).append(" WHERE 1=1");
	    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	    queryParams.forEach((key, values) -> {
	        String value = values.get(0);
	        if (value != null && !value.trim().isEmpty()) {
	            sql.append(" AND ").append(key).append(" = '").append(value).append("'");
	        }
	    });

	    ResultSet result = null;
	    JSONArray jsonArray = new JSONArray();
	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("partid"); // Extract the Id value

	            JSONObject jsonObject = new JSONObject();
	            JSONArray basicAttributesArray = new JSONArray();
	            JSONArray attributesArray = new JSONArray();

	            // Add basic attributes
	            for (String column : basicAttributeMap.keySet()) {
	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                Map<String, String> details = basicAttributeMap.get(column);
	                attribute.put("displayName", details.get("displayName"));
	                attribute.put("name", details.get("internalName"));
	                attribute.put("value", columnValue);
	                basicAttributesArray.put(attribute);
	            }

	            // Add other attributes
	            for (String column : columnMap.keySet()) {
	                if (column.equalsIgnoreCase("Id")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }

	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                Map<String, String> details = columnMap.get(column);
	                attribute.put("displayName", details.get("displayName"));
	                attribute.put("name", details.get("internalName"));
	                attribute.put("value", columnValue);
	                attributesArray.put(attribute);
	            }


	            jsonObject.put("basicAttributes", basicAttributesArray);
	            jsonObject.put("attributes", attributesArray);
	            
	            JSONObject idObject = new JSONObject();
	            idObject.put("objectId: " + id, jsonObject); // Use Id as the key
	            jsonArray.put(idObject);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (result != null) {
	            try {
	                result.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    JSONObject finalObject = new JSONObject();
	    finalObject.put("results", jsonArray);
	    return finalObject.toString();
	}




	/**
	 * Retrieves detailed EBOM (Engineering Bill of Materials) records from the database based on the provided query parameters.
	 * The results are returned in a JSON format, including connection attributes, basic attributes, and other attributes.
	 * 
	 * The method dynamically builds a SQL query based on the provided `ebomrelid`, `fromid`, and `connattributes` parameters.
	 * It fetches data from the table specified in the `ebomTable` property of the `sp.properties` file. The attributes for
	 * connection attributes, basic attributes, and other attributes are also defined in the properties file.
	 * 
	 * @param ebomRelId The EBOM relationship ID to filter the records. If not provided or empty, no filter is applied.
	 * @param fromId The ID from which the EBOM is related. If not provided or empty, no filter is applied.
	 * @param connAttributes A boolean flag indicating whether to include connection attributes in the response.
	 * @return A JSON string containing the EBOM details. Each object in the JSON array represents a record from the database
	 *         with attributes based on the provided query parameters.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/ebomdetails?ebomrelid=123&fromid=456&connattributes=true
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectid: 456": {
	 *         "connection attributes": [
	 *           {
	 *             "displayName": "Connection Attribute 1",
	 *             "name": "connAttr1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Connection Attribute 2",
	 *             "name": "connAttr2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "basic attributes": [
	 *           {
	 *             "displayName": "Basic Attribute 1",
	 *             "name": "basicAttr1",
	 *             "value": "value3"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Other Attribute 1",
	 *             "name": "attr1",
	 *             "value": "value4"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ebomTable`, `ebomconnectionAttributes`, `ebombasicAttributes`,
	 *       and `attributes`.</li>
	 *   <li>The `ebomrelid` and `fromid` query parameters are optional and used to filter the records. If they are not provided,
	 *       all records from the table are included in the response.</li>
	 *   <li>If `connAttributes` is set to `true`, connection attributes are included in the response; otherwise, they are omitted.</li>
	 * </ul>
	 */

	@GET
	@Path("ebomdetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getEBOMDetails(
	        @QueryParam("fromid") String fromId,
	        @QueryParam("connattributes") boolean connAttributes
	) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties properties = new Properties();
	    try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
	        if (input == null) {
	            throw new FileNotFoundException("Property file 'sp.properties' not found in the classpath");
	        }
	        properties.load(input);
	    }

	    String ebomTable = properties.getProperty("ebomTable");
	    String connectionAttributes = properties.getProperty("ebomconnectionAttributes");
	    String basicAttributes = properties.getProperty("ebombasicAttributes");
	    String attributes = properties.getProperty("attributes");

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ");
	    sql.append(ebomTable).append(" WHERE 1=1");

	   
	    if (fromId != null && !fromId.trim().isEmpty()) {
	        sql.append(" AND fromid = '").append(fromId).append("'");
	    }

	    ResultSet result = null;
	    JSONArray jsonArray = new JSONArray();
	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        
	        result = stmt.executeQuery(sql.toString());
	        while (result.next()) {
	            JSONObject jsonObject = new JSONObject(); // Create new JSONObject for each row
	            JSONObject objectDetails = new JSONObject();
	            JSONArray connAttrArray = new JSONArray();
	            JSONArray basicAttrArray = new JSONArray();
	            JSONArray attrArray = new JSONArray();

	            // Process connection attributes
	            if (connAttributes) {
	                for (String attrPair : connectionAttributes.split(",")) {
	                    String[] attrParts = attrPair.split("\\|");
	                    String attr = attrParts[1].trim();
	                    JSONObject attrObject = new JSONObject();
	                    String displayName = attrParts[0].trim();
	                    attrObject.put("displayName", displayName);
	                    attrObject.put("name", attr);
	                    attrObject.put("value", result.getString(attr));
	                    connAttrArray.put(attrObject);
	                }
	                objectDetails.put("connectionattributes", connAttrArray);
	            }

	            // Process basic attributes
	            for (String attrPair : basicAttributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String attr = attrParts[1].trim();
	                JSONObject attrObject = new JSONObject();
	                String displayName = attrParts[0].trim();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                basicAttrArray.put(attrObject);
	            }

	            // Process other attributes
	            for (String attrPair : attributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String attr = attrParts[1].trim();
	                if (attr.equalsIgnoreCase("fromid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	                JSONObject attrObject = new JSONObject();
	                String displayName = attrParts[0].trim();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                attrArray.put(attrObject);
	            }

	            objectDetails.put("basicattributes", basicAttrArray);
	            objectDetails.put("attributes", attrArray);

	            jsonObject.put("objectid: " + result.getString("fromid"), objectDetails);
	            jsonArray.put(jsonObject);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (result != null) {
	            try {
	                result.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    return new JSONObject().put("results", jsonArray).toString();
	}



	/**
	 * Retrieves detailed EBOM (Engineering Bill of Materials) records and associated EC Part details from the database based on
	 * the provided query parameters. The results are returned in JSON format, including connection attributes, basic attributes,
	 * other attributes, and optionally EC Part attributes.
	 * 
	 * This method dynamically constructs a SQL query to fetch records from the EBOM table and, if requested, fetches additional
	 * attributes from the EC Part table. The attributes to be displayed for EC Parts are specified through the `objectAttributes`
	 * parameter and their display names are mapped using properties loaded from the `sp.properties` file.
	 * 
	 * @param ebomRelId The EBOM relationship ID to filter the records. If not provided or empty, no filter is applied.
	 * @param fromId The ID from which the EBOM is related. If not provided or empty, no filter is applied.
	 * @param connAttributes A boolean flag indicating whether to include connection attributes in the response.
	 * @param objectAttributes A comma-separated string specifying the EC Part attributes to be included in the response. 
	 *                         Each attribute is represented by its internal name. If not provided or empty, no EC Part attributes
	 *                         are included.
	 * @return A JSON string containing the EBOM details and optionally EC Part details. Each object in the JSON array represents
	 *         a record from the database with attributes based on the provided query parameters.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/ebomretrivalparentcolumns?ebomrelid=123&fromid=456&connattributes=true&objectattributes=attr1,attr2
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "objectid: 456": {
	 *         "connection attributes": [
	 *           {
	 *             "displayName": "Connection Attribute 1",
	 *             "name": "connAttr1",
	 *             "value": "value1"
	 *           },
	 *           {
	 *             "displayName": "Connection Attribute 2",
	 *             "name": "connAttr2",
	 *             "value": "value2"
	 *           }
	 *         ],
	 *         "basic attributes": [
	 *           {
	 *             "displayName": "Basic Attribute 1",
	 *             "name": "basicAttr1",
	 *             "value": "value3"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Other Attribute 1",
	 *             "name": "attr1",
	 *             "value": "value4"
	 *           }
	 *         ],
	 *         "ecpart attributes": [
	 *           {
	 *             "displayName": "EC Part Attribute 1",
	 *             "name": "attr1",
	 *             "value": "ecValue1"
	 *           },
	 *           {
	 *             "displayName": "EC Part Attribute 2",
	 *             "name": "attr2",
	 *             "value": "ecValue2"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `ebomTable`, `ebomconnectionAttributes`, `ebombasicAttributes`,
	 *       `attributes`, and `ecpartcolumnsfordisplaythroughebom`.</li>
	 *   <li>The `ebomrelid` and `fromid` query parameters are optional and used to filter the records. If they are not provided,
	 *       all records from the table are included in the response.</li>
	 *   <li>If `connAttributes` is set to `true`, connection attributes are included in the response; otherwise, they are omitted.</li>
	 *   <li>The `objectAttributes` parameter should be a comma-separated list of internal names for EC Part attributes to be included.
	 *       If it is not provided or empty, EC Part attributes are not included.</li>
	 *   <li>The `ecpartcolumnsfordisplaythroughebom` property in the `sp.properties` file maps internal names to display names for
	 *       EC Part attributes.</li>
	 * </ul>
	 */
	
	@GET
	@Path("ebomretrivalparentcolumns")
	@Produces(MediaType.APPLICATION_JSON)
	public String getParentObjectColumnsFromEBOMTable(
	        @QueryParam("fromid") String fromId,
	        @QueryParam("connattributes") boolean connAttributes,
	        @QueryParam("objectattributes") String objectAttributes
	) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties properties = new Properties();
	    try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
	        if (input == null) {
	            throw new FileNotFoundException("Property file 'sp.properties' not found in the classpath");
	        }
	        properties.load(input);
	    }

	    String ebomTable = properties.getProperty("ebomTable");
	    String connectionAttributes = properties.getProperty("ebomconnectionAttributes");
	    String basicAttributes = properties.getProperty("ebombasicAttributes");
	    String attributes = properties.getProperty("attributes");
	    String ecPartTable = properties.getProperty("ecpartTable");
	    
	    // Load EC Part Attributes Display Names from properties
	    String ecPartAttributesDisplayNames = properties.getProperty("ecpartcolumnsfordisplaythroughebom");
	    Map<String, String> ecPartAttributesMap = new HashMap<>();
	    for (String attrPair : ecPartAttributesDisplayNames.split(",")) {
	        String[] attrParts = attrPair.split("\\|");
	        ecPartAttributesMap.put(attrParts[1].trim(), attrParts[0].trim()); // internalName -> displayName
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ");
	    sql.append(ebomTable).append(" WHERE 1=1");

	  
	    if (fromId != null && !fromId.trim().isEmpty()) {
	        sql.append(" AND fromid = '").append(fromId).append("'");
	    }

	    ResultSet result = null;
	    ResultSet ecPartResult = null;
	    JSONArray jsonArray = new JSONArray();
	    Connection conn = null;
	    Statement stmt = null;
	    Statement ecPartStmt = null;

	    try {
	        Class.forName("org.postgresql.Driver");
	        conn = DriverManager.getConnection(url, userName, password);
	        stmt = conn.createStatement();
	        
	        result = stmt.executeQuery(sql.toString());
	        while (result.next()) {
	            JSONObject jsonObject = new JSONObject(); // Create new JSONObject for each row
	            JSONObject objectDetails = new JSONObject();
	            JSONArray connAttrArray = new JSONArray();
	            JSONArray basicAttrArray = new JSONArray();
	            JSONArray attrArray = new JSONArray();

	            // Process connection attributes
	            if (connAttributes) {
	                for (String attrPair : connectionAttributes.split(",")) {
	                    String[] attrParts = attrPair.split("\\|");
	                    String displayName = attrParts[0].trim();
	                    String attr = attrParts[1].trim();
	                    JSONObject attrObject = new JSONObject();
	                    attrObject.put("displayName", displayName);
	                    attrObject.put("name", attr);
	                    attrObject.put("value", result.getString(attr));
	                    connAttrArray.put(attrObject);
	                }
	                objectDetails.put("connection attributes", connAttrArray);
	            }

	            // Process basic attributes
	            for (String attrPair : basicAttributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String displayName = attrParts[0].trim();
	                String attr = attrParts[1].trim();
	                JSONObject attrObject = new JSONObject();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                basicAttrArray.put(attrObject);
	            }

	            // Process other attributes
	            for (String attrPair : attributes.split(",")) {
	                String[] attrParts = attrPair.split("\\|");
	                String displayName = attrParts[0].trim();
	                String attr = attrParts[1].trim();
	                if (attr.equalsIgnoreCase("fromid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	                JSONObject attrObject = new JSONObject();
	                attrObject.put("displayName", displayName);
	                attrObject.put("name", attr);
	                attrObject.put("value", result.getString(attr));
	                attrArray.put(attrObject);
	            }

	            // Process EC_Part_Details attributes if specified
	            if (objectAttributes != null && !objectAttributes.trim().isEmpty()) {
	                String[] objectAttrs = objectAttributes.split(",");
	                String ecPartSql = "SELECT * FROM " + ecPartTable + " WHERE Id = '" + result.getString("fromid") + "'";

	                ecPartStmt = conn.createStatement();
	                ecPartResult = ecPartStmt.executeQuery(ecPartSql);
	                if (ecPartResult.next()) {
	                    JSONArray ecPartAttrArray = new JSONArray();
	                    for (String attr : objectAttrs) {
	                        String internalName = attr.trim();
	                        if (internalName.equalsIgnoreCase("id")) {
	    	                    // Skip processing the 'Id' column
	    	                    continue;
	    	                }
	                        System.out.println("internalName*"+internalName);
	                        String displayName = ecPartAttributesMap.getOrDefault(internalName, internalName); // Use default if not found
	                        System.out.println("displayName*"+displayName);
	                        JSONObject attrObject = new JSONObject();
	                        attrObject.put("displayName", displayName);
	                        attrObject.put("name", internalName);
	                        attrObject.put("value", ecPartResult.getString(internalName));
	                        ecPartAttrArray.put(attrObject);
	                    }
	                    objectDetails.put("ecpart attributes", ecPartAttrArray);
	                }
	            }

	            objectDetails.put("basic attributes", basicAttrArray);
	            objectDetails.put("attributes", attrArray);

	            jsonObject.put("objectid: " + result.getString("fromid"), objectDetails);
	            jsonArray.put(jsonObject);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (result != null) {
	            try {
	                result.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (ecPartResult != null) {
	            try {
	                ecPartResult.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (stmt != null) {
	            try {
	                stmt.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (ecPartStmt != null) {
	            try {
	                ecPartStmt.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        if (conn != null) {
	            try {
	                conn.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    return new JSONObject().put("results", jsonArray).toString();
	}


	/**
	 * RESTful API to fetch person member company details along with associated person details attributes.
	 * This method reads from a properties file to dynamically build and execute an SQL query, 
	 * retrieves data from the database, and returns the results as a JSON response.
	 *
	 * @param uriInfo Contains the query parameters passed to the API endpoint.
	 * @return A JSON string containing the person member company details and associated person details attributes.
	 * @throws Exception If there is an issue loading the properties file or executing the SQL query.
	 *
	 * Example usage:
	 * 
	 * Suppose the following URL is used to make a GET request:
	 * 
	 * http://localhost:8080/api/getpersonmembercompanydetails?PersonId=12345
	 * 
	 * The properties file (`sp.properties`) includes:
	 * 
	 * supplierpersonmembercompanydetailsTable=Person_Member_Company_Details
	 * supplierpersonmembercompanydetailscolumns=Type|type,Name|name,Revision|rev,Policy|policy,State|state,Organization|organization,Project|project,Owner|owner
	 * supplierpersonmembercompanydetailsattributes=Attribute1|attribute1,Attribute2|attribute2
	 * 
	 * The response might look like:
	 * 
	 * {
	 *   "results": [
	 *     {
	 *       "personid: 12345": {
	 *         "attributes": [
	 *           {
	 *             "displayName": "Type",
	 *             "name": "type",
	 *             "value": "Employee"
	 *           },
	 *           {
	 *             "displayName": "Name",
	 *             "name": "name",
	 *             "value": "John Doe"
	 *           }
	 *           // ... other attributes
	 *         ],
	 *         "Person Details attribute": [
	 *           {
	 *             "displayName": "Attribute1",
	 *             "name": "attribute1",
	 *             "value": "Value1"
	 *           },
	 *           {
	 *             "displayName": "Attribute2",
	 *             "name": "attribute2",
	 *             "value": "Value2"
	 *           }
	 *           // ... other person details attributes
	 *         ]
	 *       }
	 *     }
	 *     // ... other person member company details
	 *   ]
	 * }
	 */
	
	@GET
	@Path("getpersonmembercompanydetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPersonMemberCompanyDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    String tableName = pro.getProperty("supplierpersonmembercompanydetailsTable");

	    // Load supplierpersonmembercompanydetails columns 
	    String columnsProperty = pro.getProperty("supplierpersonmembercompanydetailscolumns");

	    // Load supplierpersondetails columns for Person Details attribute
	    String personDetailsColumnsProperty = pro.getProperty("supplierpersonmembercompanydetailsattributes");

	    // Map to store the column names and their display names for supplierpersonmembercompanydetails
	    Map<String, String> columnMap = new HashMap<>();
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            columnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Map to store the column names and their display names for Supplier_Person_Details
	    Map<String, String> personDetailsColumnMap = new HashMap<>();
	    for (String mapping : personDetailsColumnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            personDetailsColumnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE 1=1");
	    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	    queryParams.forEach((key, values) -> {
	        String value = values.get(0);
	        if (value != null && !value.trim().isEmpty()) {
	            sql.append(" AND ").append(key).append(" = '").append(value).append("'");
	        }
	    });

	    ResultSet result = null;
	    JSONArray jsonArray = new JSONArray();
	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("PersonId"); // Extract the PersonId value

	            JSONObject jsonObject = new JSONObject();
	            JSONArray attributesArray = new JSONArray();
	            JSONArray personDetailsArray = new JSONArray();

	            // Add attributes processing
	            for (String column : columnMap.keySet()) {
	            	  if (column.equalsIgnoreCase("personid")) {
		                    // Skip processing the 'Id' column
		                    continue;
		                }
	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = columnMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                attributesArray.put(attribute);
	            }

	            // Retrieve all columns from Supplier_Person_Details based on PersonId
	            String personDetailsQuery = "SELECT * FROM Supplier_Person_Details WHERE PersonId = '" + id + "'";
	            ResultSet personDetailsResult = stmt.executeQuery(personDetailsQuery);
	            ResultSetMetaData metaData = personDetailsResult.getMetaData();
	            int columnCount = metaData.getColumnCount();

	            if (personDetailsResult.next()) {
	                for (int i = 1; i <= columnCount; i++) {
	                    String columnName = metaData.getColumnName(i);
	                    String columnValue = personDetailsResult.getString(columnName);
	                    if (columnName.equalsIgnoreCase("personid")) {
		                    // Skip processing the 'Id' column
		                    continue;
		                }
	                    JSONObject personDetail = new JSONObject();
	                    String displayName = personDetailsColumnMap.getOrDefault(columnName, columnName); // Get display name from map or use column name
	                    personDetail.put("displayName", displayName);
	                    personDetail.put("name", columnName.toLowerCase()); // Lowercase name for consistency
	                    personDetail.put("value", columnValue);
	                    personDetailsArray.put(personDetail);
	                }
	            }
	            personDetailsResult.close();

	            jsonObject.put("attributes", attributesArray);
	            jsonObject.put("Person Details attribute", personDetailsArray);

	            JSONObject idObject = new JSONObject();
	            idObject.put("personid: " + id, jsonObject); // Use PersonId as the key
	            jsonArray.put(idObject);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (result != null) {
	            try {
	                result.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    JSONObject finalObject = new JSONObject();
	    finalObject.put("results", jsonArray);
	    return finalObject.toString();
	}


	/**
	 * Retrieves details of supplier persons from the `supplierpersondetailsTable` based on the provided query parameters.
	 * The results are returned in JSON format, including both basic attributes and other attributes dynamically mapped from columns specified in the properties file.
	 * 
	 * This method constructs a SQL query dynamically based on the query parameters and column mappings defined in the `sp.properties` file.
	 * It then fetches records from the database and formats them into JSON, including basic attributes and additional attributes with their display names.
	 * 
	 * @param uriInfo The URI information containing the query parameters to filter the database records.
	 * @return A JSON string containing the details of supplier persons. Each entry in the JSON array represents a record,
	 *         with basic attributes and additional attributes formatted based on the column mappings.
	 * @throws Exception If there is an error accessing the database, processing the request, or if the properties file is missing.
	 * 
	 * <p>Example Usage:</p>
	 * 
	 * <pre>
	 * Example URL: http://localhost:8081/Webservice/webapi/getsupplierpersondetails?PersonId=456&CompanyName=GlobalTech
	 * 
	 * Response:
	 * {
	 *   "results": [
	 *     {
	 *       "personId: 456": {
	 *         "basicAttributes": [
	 *           {
	 *             "displayName": "Person ID",
	 *             "internalName": "PersonId",
	 *             "value": "456"
	 *           },
	 *           {
	 *             "displayName": "First Name",
	 *             "internalName": "FirstName",
	 *             "value": "John"
	 *           },
	 *           {
	 *             "displayName": "Last Name",
	 *             "internalName": "LastName",
	 *             "value": "Doe"
	 *           }
	 *         ],
	 *         "attributes": [
	 *           {
	 *             "displayName": "Company Name",
	 *             "name": "CompanyName",
	 *             "value": "GlobalTech"
	 *           },
	 *           {
	 *             "displayName": "Email",
	 *             "name": "Email",
	 *             "value": "john.doe@globaltech.com"
	 *           }
	 *         ]
	 *       }
	 *     }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>Note:</p>
	 * <ul>
	 *   <li>The `sp.properties` file should contain mappings for `supplierpersondetailsTable`, `supplierpersonAttributedetailscolumns`, and `supplierpersonBasicAttributedetailscolumns`.</li>
	 *   <li>The `supplierpersonAttributedetailscolumns` property should be in the format `displayName|internalName,displayName|internalName,...` for additional attributes.</li>
	 *   <li>The `supplierpersonBasicAttributedetailscolumns` property should be in the format `displayName|internalName,displayName|internalName,...` for basic attributes.</li>
	 *   <li>The query parameters are dynamically appended to the SQL query to filter the records from the database. For example, `PersonId=456` will filter records where the `PersonId` column is equal to `456`.</li>
	 *   <li>The `PersonId` column from the result set is used as the key in the JSON response object.</li>
	 * </ul>
	 */
	
	@GET
	@Path("getsupplierpersondetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllSupplierPersonDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalSPDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");


	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    String tableName = pro.getProperty("supplierpersondetailsTable");

	    // Load supplierpersondetails columns and basic attributes
	    String columnsProperty = pro.getProperty("supplierpersonAttributedetailscolumns");
	    String basicAttributesProperty = pro.getProperty("supplierpersonBasicAttributedetailscolumns");

	    // Map to store the column names and their display names
	    Map<String, String> columnMap = new HashMap<>();
	    for (String mapping : columnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            columnMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Map to store the basic attribute column names and their display names
	    Map<String, String> basicAttributeMap = new HashMap<>();
	    for (String mapping : basicAttributesProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            basicAttributeMap.put(parts[1].trim(), parts[0].trim());
	        }
	    }

	    // Build the SQL query dynamically based on provided query parameters
	    StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE 1=1");
	    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	    queryParams.forEach((key, values) -> {
	        String value = values.get(0);
	        if (value != null && !value.trim().isEmpty()) {
	            sql.append(" AND ").append(key).append(" = '").append(value).append("'");
	        }
	    });

	    ResultSet result = null;
	    JSONArray jsonArray = new JSONArray();
	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection conn = DriverManager.getConnection(url, userName, password);
	        Statement stmt = conn.createStatement();
	        result = stmt.executeQuery(sql.toString());

	        while (result.next()) {
	            String id = result.getString("PersonId"); // Extract the PersonId value

	            JSONObject jsonObject = new JSONObject();
	            JSONArray basicAttributesArray = new JSONArray();
	            JSONArray attributesArray = new JSONArray();

	            // Add basic attributes
	            for (String column : basicAttributeMap.keySet()) {
	                JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = basicAttributeMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                basicAttributesArray.put(attribute);
	            }

	            // Add other attributes
	            for (String column : columnMap.keySet()) {
	            	if (column.equalsIgnoreCase("personid")) {
	                    // Skip processing the 'Id' column
	                    continue;
	                }
	            	JSONObject attribute = new JSONObject();
	                String columnValue = result.getString(column);
	                String displayName = columnMap.get(column);
	                
	                attribute.put("displayName", displayName); // Add display name
	                attribute.put("name", column);     // Add internal name
	                attribute.put("value", columnValue);       // Add value
	                
	                attributesArray.put(attribute);
	            }

	            jsonObject.put("basicattributes", basicAttributesArray);
	            jsonObject.put("attributes", attributesArray);
	            
	            JSONObject idObject = new JSONObject();
	            idObject.put("personid: " + id, jsonObject); // Use PersonId as the key
	            jsonArray.put(idObject);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (result != null) {
	            try {
	                result.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    JSONObject finalObject = new JSONObject();
	    finalObject.put("results", jsonArray);
	    return finalObject.toString();     
	}

	@POST
	@Path("search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getData(String s) throws IOException {
	    if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
	        JSONObject errorResponse = new JSONObject();
	        errorResponse.put("status", "fail");
	        errorResponse.put("message", "Invalid JSON input.");
	        return Response.status(Response.Status.BAD_REQUEST)
	                       .entity(errorResponse.toString())
	                       .type(MediaType.APPLICATION_JSON)
	                       .build();
	    }

	    JSONObject json = new JSONObject(s);

	    // Retrieve and sanitize the search text
	    String text = json.optString("text", "%"); // Default to '%' if text is empty

	    if (text.equals("*") || text.length() < 3) {
	        // Case where '*' or fewer than 3 characters are entered
	        JSONObject errorResponse = new JSONObject();
	        errorResponse.put("status", "fail");
	        errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
	        return Response.status(Response.Status.BAD_REQUEST)
	                       .entity(errorResponse.toString())
	                       .type(MediaType.APPLICATION_JSON)
	                       .build();
	    }

	    // Replace '*' with '%' for SQL LIKE syntax
	    text = text.replace("*", "%");

	    String field = json.optString("field");

	    String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    JSONArray jsonArray = new JSONArray();

	    try {
	        Class.forName("org.postgresql.Driver");
	        Connection con = DriverManager.getConnection(url, userName, password);

	        String sql = "";

	        if (field.equalsIgnoreCase("Everything")) {
	            // Search in both 'partid' and 'name' fields
	            sql = "SELECT partid, name, type FROM ec_parts_details WHERE (partid = ? OR name ILIKE ?)";
	        } else if (field.equalsIgnoreCase("name")) {
	            // Search by 'name' field only
	            sql = "SELECT partid, name, type FROM ec_parts_details WHERE name ILIKE ?";
	        }

	        if (!sql.isEmpty()) {
	            try (PreparedStatement ps = con.prepareStatement(sql)) {
	                if (field.equalsIgnoreCase("Everything")) {
	                    // For 'partid', allow only exact match, check for wildcards
	                    if (text.contains("%")) {
	                        // If 'partid' contains wildcards, skip the 'partid' search and return results only for 'name'
	                        ps.setString(1, "");  // No 'partid' results with wildcard
	                        ps.setString(2, "%" + text + "%");  // Wildcard search for 'name'
	                    } else {
	                        // Exact match for 'partid' and wildcard match for 'name'
	                        ps.setString(1, text);  // Exact match for 'partid'
	                        ps.setString(2, "%" + text + "%");  // Wildcard match for 'name'
	                    }
	                } else if (field.equalsIgnoreCase("name")) {
	                    // Search by name with wildcard
	                    ps.setString(1, "%" + text + "%");  // Wildcard for 'name'
	                }

	                ResultSet set = ps.executeQuery();
	                Map<String, List<String>> partsByType = new HashMap<>();

	                while (set.next()) {
	                    String id = set.getString("partid");
	                    String type = set.getString("type");
	                    partsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(id);
	                }

	                // Construct JSON output
	                for (Map.Entry<String, List<String>> entry : partsByType.entrySet()) {
	                    String type = entry.getKey();
	                    List<String> partIds = entry.getValue();

	                    JSONObject jsonObject = new JSONObject();
	                    JSONObject typeObject = new JSONObject();
	                    typeObject.put("partid", String.join("|", partIds));
	                    jsonObject.put("type: " + type, typeObject);
	                    jsonArray.put(jsonObject);
	                }
	            }
	        }

	        JSONObject js = new JSONObject();
	        js.put("results", jsonArray);
	        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

	    } catch (Exception e) {
	        e.printStackTrace();
	        JSONObject js = new JSONObject();
	        js.put("status", "fail");
	        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
	    }
	}



	@GET
	@Path("getSupplierDetailsByEmail")
	@Produces(MediaType.APPLICATION_JSON)
	public String getSupplierDetailsByEmail(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);

	    // Load column mappings for ca_suppliers_details attributes
	    String detailsColumnsProperty = pro.getProperty("casuppliersdetailsattributes");
	    Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
	    for (String mapping : detailsColumnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            detailsColumnMap.put(parts[1].trim(), detailMap);
	        }
	    }

	 // Load column mappings for ca_suppliers_connection_details attributes
	    String connectionColumnsProperty = pro.getProperty("casuppliersconnectiondetailsattributes");
	    Map<String, Map<String, String>> connectionColumnMap = new HashMap<>();
	    for (String mapping : connectionColumnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            connectionColumnMap.put(parts[1].trim(), detailMap);
	        }
	    }

	    // Get email address from query parameter
	    String email = uriInfo.getQueryParameters().getFirst("email");
	    if (email == null || email.trim().isEmpty()) {
	        return "{ \"error\": \"Missing or empty email parameter\" }";
	    }

	    // Query combining both person details and supplier details using JOIN
	    String supplierDetailsQuery = "SELECT casd.*, ca.state, ca.owner, ca.description " +
                "FROM Supplier_Person_Details spd " +
                "JOIN ca_suppliers_details casd ON spd.companyid = casd.companyid " +
                "JOIN changeaction ca ON ca.caid = casd.changenumber " +
                "WHERE spd.email_address = ?";


	    JSONArray jsonArray = new JSONArray();
	    Class.forName("org.postgresql.Driver");

	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	         PreparedStatement supplierStmt = conn.prepareStatement(supplierDetailsQuery)) {

	        // Set the email parameter
	        supplierStmt.setString(1, email);
	        ResultSet supplierResult = supplierStmt.executeQuery();

	        // Check if result set has any rows
	        if (!supplierResult.isBeforeFirst()) {
	            return "{ \"error\": \"No supplier details found for email: " + email + "\" }";
	        }
	        // Process result set
	        while (supplierResult.next()) {
	            JSONObject jsonObject = new JSONObject();
	            JSONArray attributesArray = new JSONArray();
	            JSONArray connectionAttributesArray = new JSONArray();

	            // Add ca_suppliers_details attributes
	            for (String column : detailsColumnMap.keySet()) {
	                String columnValue = supplierResult.getString(column);
	                if (column.equalsIgnoreCase("companyid")) {
	                    // Skip processing the 'companyid' column
	                    continue;
	                }
	                // Handle null values
	                if (columnValue == null) {
	                    columnValue = "";  // set default value to an empty string
	                }


	                JSONObject attribute = new JSONObject();
	                Map<String, String> details = detailsColumnMap.get(column);
	                attribute.put("displayName", details.get("displayName"));
	                attribute.put("name", details.get("internalName"));
	                attribute.put("value", columnValue);
	                attributesArray.put(attribute);
	            }
	            // Add ca_suppliers_connection_details attributes
	            for (String column : connectionColumnMap.keySet()) {
	                String columnValue = supplierResult.getString(column);
	                if (columnValue != null) {  // Check if the value exists
	                    JSONObject attribute = new JSONObject();
	                    Map<String, String> details = connectionColumnMap.get(column);
	                    attribute.put("displayName", details.get("displayName"));
	                    attribute.put("name", details.get("internalName"));
	                    attribute.put("value", columnValue);
	                    connectionAttributesArray.put(attribute);
	                }
            }
	            jsonObject.put("attributes", attributesArray);
	            jsonObject.put("connectionattributes", connectionAttributesArray);
	        
	            JSONObject idObject = new JSONObject();
	            idObject.put("companyid: " + supplierResult.getString("companyid"),jsonObject);  // Use companyid as the key
	            jsonArray.put(idObject);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
	    }

	    JSONObject finalObject = new JSONObject();
	    finalObject.put("results", jsonArray);
	    return finalObject.toString();
	}

	@GET
	@Path("getchangeactiondetails")
	@Produces(MediaType.APPLICATION_JSON)
	public String getChangeActionDetails(@Context UriInfo uriInfo) throws Exception {
		String url=System.getenv("SupplierPortalDBURL");
		String password=System.getenv("SupplierPortalDBPassword");
		String userName= System.getenv("SupplierPortalDBUsername");

	    // Load properties file
	    Properties pro = new Properties();
	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	    if (input == null) {
	        throw new FileNotFoundException("sp.properties file not found.");
	    }
	    pro.load(input);
	    
	    String caTable = pro.getProperty("catable");

	    // Load column mappings for ca_suppliers_details attributes
	    String detailsColumnsProperty = pro.getProperty("cadetails");
	    Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
	    for (String mapping : detailsColumnsProperty.split(",")) {
	        String[] parts = mapping.split("\\|");
	        if (parts.length == 2) {
	            Map<String, String> detailMap = new HashMap<>();
	            detailMap.put("internalName", parts[1].trim());
	            detailMap.put("displayName", parts[0].trim());
	            detailsColumnMap.put(parts[1].trim(), detailMap);
	        }
	    }

	    // Get caID from query parameter
	    String caID = uriInfo.getQueryParameters().getFirst("caid");
	    if (caID == null || caID.trim().isEmpty()) {
	        return "{ \"error\": \"Missing or empty caID parameter\" }";
	    }

	    // Query to fetch the required data
	    String caDetailsQuery = "SELECT * FROM "+ caTable +" ca WHERE ca.caid = ?";

	    JSONArray jsonArray = new JSONArray();
	    Class.forName("org.postgresql.Driver");

	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

	        // Set the caID parameter
	    	Stmt.setString(1, caID);
	        ResultSet Result = Stmt.executeQuery();

	        // Process the result set
	        while (Result.next()) {
	            JSONObject jsonObject = new JSONObject();
	            JSONArray attributesArray = new JSONArray();

	            // Add attributes from ca_suppliers_details
	            for (String column : detailsColumnMap.keySet()) {
	                String columnValue = Result.getString(column);

	                // Skip processing if no value for this column
	                if (columnValue == null) continue;

	                JSONObject attribute = new JSONObject();
	                Map<String, String> details = detailsColumnMap.get(column);
	                attribute.put("displayName", details.get("displayName"));
	                attribute.put("name", details.get("internalName"));
	                attribute.put("value", columnValue);
	                attributesArray.put(attribute);
	            }

	            jsonObject.put("attributes", attributesArray);
	            JSONObject idObject = new JSONObject();
	            idObject.put("id: " + Result.getString("caid"), jsonObject);
	            jsonArray.put(idObject);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
	    }

	    JSONObject finalObject = new JSONObject();
	    finalObject.put("results", jsonArray);
	    return finalObject.toString();
	}

	 @POST
		@Path("updateAcknowledgedInfo")
	    @Consumes(MediaType.APPLICATION_JSON)
	    @Produces(MediaType.APPLICATION_JSON)
	    public Response setAcknowledgedInfo(String jsonInput) throws ClassNotFoundException {
	        // Database connection parameters
		 String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
	        // Load properties file
	        Properties pro = new Properties();
	        try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
	            if (input == null) {
	                throw new FileNotFoundException("sp.properties file not found.");
	            }
	            pro.load(input);
	        } catch (IOException e) {
	            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                           .entity("Error loading properties file: " + e.getMessage())
	                           .build();
	        }

	        String tablename = pro.getProperty("casuppliersdetailsTable");

	        // Parse the JSON input
	        JSONObject jsonObject = new JSONObject(jsonInput);
	        String acknowledgedBy = jsonObject.getString("username");
	        
	        // Remove everything after the '@' in the email
	        String username = acknowledgedBy.split("@")[0];

	        String objectId = jsonObject.getString("objectId");
	        String value = jsonObject.getString("value");
	        if ("true".equals(value)) {
	            value = "Yes";
	        } else if ("false".equals(value)) {
	            value = "No";
	        }
	        

	        // Connect to PostgreSQL database and update the record
	        String updateSQL = "UPDATE " + tablename + 
	                           " SET acknowledge = ?, acknowledgedby = ? " +
	                           "WHERE changenumber = ?";
	        Class.forName("org.postgresql.Driver");
	        try (Connection conn = DriverManager.getConnection(url, userName, password);
	             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
	             
	            pstmt.setString(1, value); // Acknowledge value
	            pstmt.setString(2, username); // Use the extracted username
	            pstmt.setString(3, objectId); // Change number (objectId)

	            int rowsAffected = pstmt.executeUpdate();
	            if (rowsAffected > 0) {
	                return Response.ok("Update successful").build();
	            } else {
	                return Response.status(Response.Status.NOT_FOUND)
	                               .entity("No records updated").build();
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	                           .entity("Database error: " + e.getMessage())
	                           .build();
	        }
	    }

	 	@GET
		@Path("getcaaffectedItems")
		@Produces(MediaType.APPLICATION_JSON)
		public String getCaAffectedItems(@Context UriInfo uriInfo) throws Exception {
	 		String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    String tablename = pro.getProperty("PartTable");
		    String supplierTable = pro.getProperty("Ca_supplier_Table");

		    // Load ecpartcolumns and ecpartbasicAttributes
		    String columnsProperty = pro.getProperty("partcolumns");
		    String basicAttributesProperty = pro.getProperty("partbasicAttributes");

		    // Map to store the column names, display names, and internal names
		    Map<String, Map<String, String>> columnMap = new HashMap<>();
		    for (String mapping : columnsProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            columnMap.put(parts[1].trim(), detailMap);
		        }
		    }

		    // Map to store the basic attribute column names, display names, and internal names
		    Map<String, Map<String, String>> basicAttributeMap = new HashMap<>();
		    for (String mapping : basicAttributesProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            basicAttributeMap.put(parts[1].trim(), detailMap);
		        }
		    }
		    String caId = uriInfo.getQueryParameters().getFirst("caid");
		    if (caId == null || caId.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty email parameter\" }";
		    }
		    // Build the SQL query dynamically based on provided query parameters
		    String caDetailsQuery =  "SELECT epd.*, csd.supplier_visibility, csd.supplier_item_visibility, csd.supplier_spec_visibility " +
		            "FROM " + tablename + " epd " +
		            "JOIN " + supplierTable + " csd " +
		            "ON epd.changenumber = csd.changenumber " +
		            "WHERE epd.changenumber = ?";

		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

		        // Set the caID parameter
		    	Stmt.setString(1, caId);
		        ResultSet Result = Stmt.executeQuery();


		        while (Result.next()) {
		            String id = Result.getString("partid"); // Extract the Id value

		            JSONObject jsonObject = new JSONObject();
		            JSONArray basicAttributesArray = new JSONArray();
		            JSONArray attributesArray = new JSONArray();

		            // Add basic attributes
		            for (String column : basicAttributeMap.keySet()) {
		                JSONObject attribute = new JSONObject();
		                String columnValue = Result.getString(column);
		                Map<String, String> details = basicAttributeMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                basicAttributesArray.put(attribute);
		            }

		            // Add other attributes
		            for (String column : columnMap.keySet()) {
//		                if (column.equalsIgnoreCase("partid")) {
//		                    // Skip processing the 'Id' column
//		                    continue;
//		                }

		                JSONObject attribute = new JSONObject();
		                String columnValue = Result.getString(column);
		                Map<String, String> details = columnMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                attributesArray.put(attribute);
		            }


		            jsonObject.put("basicAttributes", basicAttributesArray);
		            jsonObject.put("attributes", attributesArray);
		            
		            JSONObject idObject = new JSONObject();
		            idObject.put("objectId: " + id, jsonObject); // Use Id as the key
		            jsonArray.put(idObject);
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    } 
		    JSONObject finalObject = new JSONObject();
		    finalObject.put("results", jsonArray);
		    return finalObject.toString();
	 }
	 	@POST
	 	@Path("getSupplierData")
	 	@Produces(MediaType.APPLICATION_JSON)
	 	@Consumes(MediaType.APPLICATION_JSON)
	 	public String getSupplierData(String inputJson) throws Exception {
	 		String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
	 	    Properties pro = new Properties();
	 	    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
	 	    if (input == null) {
	 	        throw new FileNotFoundException("sp.properties file not found.");
	 	    }
	 	    pro.load(input);

	 	    String mpnTable = pro.getProperty("MpnTable");
	 	    String connectionTablename = pro.getProperty("MpnconnectionTable");
	 	    String caSuppliersTable = pro.getProperty("Ca_supplier_Table");

	 	    Class.forName("org.postgresql.Driver");


	 	    JSONObject inputObject = new JSONObject(inputJson);
	 	    String objectIds = inputObject.getString("objectIds");
	 	    String caid = inputObject.getString("caid");

	 	    String[] objectIdsArray;
	 	    if (objectIds.contains("|")) {
	 	        objectIdsArray = objectIds.split("\\|");
	 	    } else {
	 	        objectIdsArray = new String[]{objectIds};  // Single objectId
	 	    }

	 	    // Prepare queries
	 	    String Sql = "SELECT mpn.ManufacturerName " +
	 	               "FROM " + connectionTablename + " AS related_parts " +
	 	               "JOIN " + mpnTable + " AS mpn " +
	 	               "ON related_parts.MPNID = mpn.MPNID " +
	 	               "WHERE related_parts.Partid = ?";
	 	    
	 	    String sql2 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
	 	               "FROM " + caSuppliersTable + " WHERE changenumber= ? AND company_name = ?";
	 	    
	 	    String sql3 = "SELECT supplier_visibility, supplier_item_visibility, supplier_spec_visibility " +
	 	               "FROM " + caSuppliersTable +
	 	               " WHERE (supplier_item_visibility LIKE ? OR supplier_spec_visibility LIKE ?)";

	 	    // JSON response structure
	 	    JSONObject jsonResponse = new JSONObject();
	 	    JSONArray resultsArray = new JSONArray();

	 	    try (Connection conn = DriverManager.getConnection(url, userName, password);
	 	         PreparedStatement joinStmt = conn.prepareStatement(Sql);
	 	         PreparedStatement ps2 = conn.prepareStatement(sql2);
	 	         PreparedStatement ps3 = conn.prepareStatement(sql3)) {

	 	        // Loop through each object ID
	 	        for (String partid : objectIdsArray) {
	 	            joinStmt.setString(1, partid);
	 	            ResultSet joinResultSet = joinStmt.executeQuery();

	 	            JSONObject resultObject = new JSONObject();
	 	            JSONObject idObject = new JSONObject();  // This will hold the attributes under the part ID
	 	            JSONObject attributes = new JSONObject(); // Initialize attributes with default values
	 	            attributes.put("supplier", false);
	 	            attributes.put("supplieritem", false);
	 	            attributes.put("supplierspec", false);

	 	            if (joinResultSet.next()) {
	 	                String manufacturerName = joinResultSet.getString("manufacturername");

	 	                
	 	                ps2.setString(1, caid);  // Set changenumber (caid)
	 	                ps2.setString(2, manufacturerName);  // Set suppliername
	 	                ResultSet res1 = ps2.executeQuery();

	 	                if (res1.next()) {
	 	                    // Retrieve visibility values from the result set
	 	                    String supplierVisibility = res1.getString("supplier_visibility");
	 	                    String supplierItemVisibility = res1.getString("supplier_item_visibility");
	 	                    String supplierSpecVisibility = res1.getString("supplier_spec_visibility");

	 	                    attributes.put("supplier", true);
	 	                    attributes.put("supplier_visibility", supplierVisibility);
	 	                    attributes.put("supplier_item_visibility", supplierItemVisibility);
	 	                    attributes.put("supplier_spec_visibility", supplierSpecVisibility);
	 	                } else {
	 	                    
	 	                    String partidLike = "%" + partid + "%";
	 	                    ps3.setString(1, partidLike); // Check if partid exists in supplier_item_visibility
	 	                    ps3.setString(2, partidLike); // Check if partid exists in supplier_spec_visibility
	 	                    ResultSet res2 = ps3.executeQuery();

	 	                    if (res2.next()) {
	 	                        // Retrieve visibility values from the result set
	 	                        String supplierVisibility = res2.getString("supplier_visibility");
	 	                        String supplierItemVisibility = res2.getString("supplier_item_visibility");
	 	                        String supplierSpecVisibility = res2.getString("supplier_spec_visibility");

	 	                        // Set true if supplier_item_visibility or supplier_spec_visibility matched
	 	                        if (supplierItemVisibility.contains(partid) || supplierSpecVisibility.contains(partid)) {
	 	                            attributes.put("supplieritem", true);
	 	                            attributes.put("supplierspec", true);
	 	                        }

	 	                        attributes.put("supplier_visibility", supplierVisibility);
	 	                        attributes.put("supplier_item_visibility", supplierItemVisibility);
	 	                        attributes.put("supplier_spec_visibility", supplierSpecVisibility);
	 	                    }
	 	                }
	 	            }

	 	            // Attach attributes to the idObject with partid as the key
	 	            idObject.put("attributes", new JSONArray().put(attributes));

	 	            
	 	            resultObject.put("id: " + partid, idObject);
	 	            resultsArray.put(resultObject);
	 	        }
	 	        jsonResponse.put("results", resultsArray);
	 	        return jsonResponse.toString();

	 	    } catch (SQLException e) {
	 	        e.printStackTrace();
	 	        throw new Exception("Error fetching supplier data", e);
	 	    }
	 	}
	 	
	 	@POST
		@Path("updateprocessattribute")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Response setProcessAttributes(String jsonInput) throws ClassNotFoundException {
		    // Database connection parameters
	 		String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");

		    // Load properties file
		    Properties pro = new Properties();
		    try (InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties")) {
		        if (input == null) {
		            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		                           .entity("sp.properties file not found.")
		                           .build();
		        }
		        pro.load(input);
		    } catch (IOException e) {
		        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		                       .entity("Error loading properties file: " + e.getMessage())
		                       .build();
		    }

		    String tablename = pro.getProperty("casuppliersdetailsTable");

		    // Parse the JSON input
		    JSONObject jsonObject = new JSONObject(jsonInput);
		    System.out.println("Received JSON: " + jsonObject);

		    String objectId = jsonObject.optString("rowId", null);
		    String comment = jsonObject.optString("comment", null);
		    String processedBy = jsonObject.optString("processedBy", null);
		    String processedDate = jsonObject.optString("processedDate", null);

		    if (objectId == null || comment == null || processedBy == null || processedDate == null) {
		        return Response.status(Response.Status.BAD_REQUEST)
		                       .entity("Missing required parameters in the input.")
		                       .build();
		    }

		    // Connect to PostgreSQL database and update the record
		    String updateSQL = "UPDATE " + tablename + 
		                       " SET processingcomment = ?, processedby = ?, processeddate = ? " +
		                       "WHERE changenumber = ?"; // 'name' here refers to the object ID in your case

		    try {
		        Class.forName("org.postgresql.Driver");
		    } catch (ClassNotFoundException e) {
		        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		                       .entity("PostgreSQL Driver not found: " + e.getMessage())
		                       .build();
		    }

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

		        pstmt.setString(1, comment);
		        pstmt.setString(2, processedBy);
		        pstmt.setString(3, processedDate);
		        pstmt.setString(4, objectId);  // Assuming objectId is used as 'name' in your table

		        int rowsAffected = pstmt.executeUpdate();
		        if (rowsAffected > 0) {
		            return Response.ok("Update successful").build();
		        } else {
		            return Response.status(Response.Status.NOT_FOUND)
		                           .entity("No records updated for objectId: " + objectId)
		                           .build();
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		                       .entity("Database error: " + e.getMessage())
		                       .build();
		    }
		}

		
		@GET
		@Path("getprocessattributedetails")
		@Produces(MediaType.APPLICATION_JSON)
		public String getProcessAttributeDetails(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");

		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    
		    String caSupplierDetailsTable = pro.getProperty("casuppliersdetailsTable");

		    // Load column mappings for ca_suppliers_details attributes
		    String detailsColumnsProperty = pro.getProperty("casupplierswithoutcadetailsattributes");
		    Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
		    for (String mapping : detailsColumnsProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            detailsColumnMap.put(parts[1].trim(), detailMap);
		        }
		    }
		    System.out.println("detailsColumnMap1111***"+detailsColumnMap);

		    // Get caID from query parameter
		    String caID = uriInfo.getQueryParameters().getFirst("changenumber");
		    if (caID == null || caID.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty caID parameter\" }";
		    }
		    System.out.println("caID***"+caID);
		    // Query to fetch the required data
		    String caDetailsQuery = "SELECT * FROM "+ caSupplierDetailsTable +" casd WHERE casd.changenumber = ?";

		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

		        // Set the caID parameter
		    	Stmt.setString(1, caID);
		        ResultSet Result = Stmt.executeQuery();

		        // Process the result set
		        while (Result.next()) {
		            JSONObject jsonObject = new JSONObject();
		            JSONArray attributesArray = new JSONArray();

		            // Add attributes from ca_suppliers_details
		            for (String column : detailsColumnMap.keySet()) {
		                String columnValue = Result.getString(column);

		                // Skip processing if no value for this column
		                if (columnValue == null) continue;

		                JSONObject attribute = new JSONObject();
		                Map<String, String> details = detailsColumnMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                attributesArray.put(attribute);
		            }

		            jsonObject.put("attributes", attributesArray);
		            JSONObject idObject = new JSONObject();
		            idObject.put("id: " + Result.getString("changenumber"), jsonObject);
		            jsonArray.put(idObject);
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

		    JSONObject finalObject = new JSONObject();
		    finalObject.put("results", jsonArray);
		    return finalObject.toString();
		}
		@GET
		@Path("getDevaitionDetailsByEmail")
		@Produces(MediaType.APPLICATION_JSON)
		public String getDevaitionDetailsByEmail(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);

		    // Load column mappings for ca_suppliers_details attributes
		    String detailsColumnsProperty = pro.getProperty("casuppliersdetailsattributes");
		    String supplierTable = pro.getProperty("supplierpersondetailsTable");
		    String caSupplierTable = pro.getProperty("casuppliersdetailsTable");
		    String devaitionTable = pro.getProperty("deviationTable");
		    Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
		    for (String mapping : detailsColumnsProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            detailsColumnMap.put(parts[1].trim(), detailMap);
		        }
		    }

		 // Load column mappings for ca_suppliers_connection_details attributes
		    String connectionColumnsProperty = pro.getProperty("casuppliersconnectiondetailsattributes");
		    Map<String, Map<String, String>> connectionColumnMap = new HashMap<>();
		    for (String mapping : connectionColumnsProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            connectionColumnMap.put(parts[1].trim(), detailMap);
		        }
		    }

		    // Get email address from query parameter
		    String email = uriInfo.getQueryParameters().getFirst("email");
		    if (email == null || email.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty email parameter\" }";
		    }

		    // Query combining both person details and supplier details using JOIN
		    String supplierDetailsQuery = "SELECT casd.*, dev.state, dev.owner, dev.description " +
	                "FROM " + supplierTable + " spd " +
	                "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
	                "JOIN "+ devaitionTable + "  dev ON dev.deviationid = casd.changenumber " +
	                "WHERE spd.email_address = ?";


		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement supplierStmt = conn.prepareStatement(supplierDetailsQuery)) {

		        // Set the email parameter
		        supplierStmt.setString(1, email);
		        ResultSet supplierResult = supplierStmt.executeQuery();

		        // Check if result set has any rows
		        if (!supplierResult.isBeforeFirst()) {
		            return "{ \"error\": \"No supplier details found for email: " + email + "\" }";
		        }
		        // Process result set
		        while (supplierResult.next()) {
		            JSONObject jsonObject = new JSONObject();
		            JSONArray attributesArray = new JSONArray();
		            JSONArray connectionAttributesArray = new JSONArray();

		            // Add ca_suppliers_details attributes
		            for (String column : detailsColumnMap.keySet()) {
		                String columnValue = supplierResult.getString(column);
		                if (column.equalsIgnoreCase("companyid")) {
		                    // Skip processing the 'companyid' column
		                    continue;
		                }
		                // Handle null values
		                if (columnValue == null) {
		                    columnValue = "";  // set default value to an empty string
		                }


		                JSONObject attribute = new JSONObject();
		                Map<String, String> details = detailsColumnMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                attributesArray.put(attribute);
		            }
		            // Add ca_suppliers_connection_details attributes
		            for (String column : connectionColumnMap.keySet()) {
		                String columnValue = supplierResult.getString(column);
		                if (columnValue != null) {  // Check if the value exists
		                    JSONObject attribute = new JSONObject();
		                    Map<String, String> details = connectionColumnMap.get(column);
		                    attribute.put("displayName", details.get("displayName"));
		                    attribute.put("name", details.get("internalName"));
		                    attribute.put("value", columnValue);
		                    connectionAttributesArray.put(attribute);
		                }
	            }
		            jsonObject.put("attributes", attributesArray);
		            jsonObject.put("connectionattributes", connectionAttributesArray);
		        
		            JSONObject idObject = new JSONObject();
		            idObject.put("companyid: " + supplierResult.getString("companyid"),jsonObject);  // Use companyid as the key
		            jsonArray.put(idObject);
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

		    JSONObject finalObject = new JSONObject();
		    finalObject.put("results", jsonArray);
		    return finalObject.toString();
		}
		
		
		
		@GET
		@Path("getDevaitionDetails")
		@Produces(MediaType.APPLICATION_JSON)
		public String getDevaitionDetails(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    
		    String devTable = pro.getProperty("deviationTable");

		    // Load column mappings for ca_suppliers_details attributes
		    String detailsColumnsProperty = pro.getProperty("Attributes_Deviation");
		    Map<String, Map<String, String>> detailsColumnMap = new HashMap<>();
		    for (String mapping : detailsColumnsProperty.split(",")) {
		        String[] parts = mapping.split("\\|");
		        if (parts.length == 2) {
		            Map<String, String> detailMap = new HashMap<>();
		            detailMap.put("internalName", parts[1].trim());
		            detailMap.put("displayName", parts[0].trim());
		            detailsColumnMap.put(parts[1].trim(), detailMap);
		        }
		    }

		    // Get caID from query parameter
		    String devID = uriInfo.getQueryParameters().getFirst("devid");
		    if (devID == null || devID.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty caID parameter\" }";
		    }

		    // Query to fetch the required data
		    String caDetailsQuery = "SELECT * FROM "+ devTable +" dev WHERE dev.deviationid = ?";

		    JSONArray jsonArray = new JSONArray();
		    Class.forName("org.postgresql.Driver");

		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement Stmt = conn.prepareStatement(caDetailsQuery)) {

		        // Set the caID parameter
		    	Stmt.setString(1, devID);
		        ResultSet Result = Stmt.executeQuery();

		        // Process the result set
		        while (Result.next()) {
		            JSONObject jsonObject = new JSONObject();
		            JSONArray attributesArray = new JSONArray();

		            // Add attributes from ca_suppliers_details
		            for (String column : detailsColumnMap.keySet()) {
		                String columnValue = Result.getString(column);

		                // Skip processing if no value for this column
		                if (columnValue == null) continue;

		                JSONObject attribute = new JSONObject();
		                Map<String, String> details = detailsColumnMap.get(column);
		                attribute.put("displayName", details.get("displayName"));
		                attribute.put("name", details.get("internalName"));
		                attribute.put("value", columnValue);
		                attributesArray.put(attribute);
		            }

		            jsonObject.put("attributes", attributesArray);
		            JSONObject idObject = new JSONObject();
		            idObject.put("id: " + Result.getString("deviationid"), jsonObject);
		            jsonArray.put(idObject);
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

		    JSONObject finalObject = new JSONObject();
		    finalObject.put("results", jsonArray);
		    return finalObject.toString();
		}
		
		@GET
		@Path("getCount")
		@Produces(MediaType.APPLICATION_JSON)
		public String getCount(@Context UriInfo uriInfo) throws Exception {
			System.out.println(System.getProperties().values());
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    String supplierTable = pro.getProperty("supplierpersondetailsTable");
		    String caSupplierTable = pro.getProperty("casuppliersdetailsTable");
		    String deviationTable = pro.getProperty("deviationTable");
		    String changeactionTable = pro.getProperty("catable");
		    String companyDetailsTable = pro.getProperty("companyTable");
		    String mpnTable = pro.getProperty("MpnTable");
		    String mpnRelatedPartsTable = pro.getProperty("MpnconnectionTable");
		    String ecPartTable = pro.getProperty("ecpartTable");
		    
		    String email = uriInfo.getQueryParameters().getFirst("email");
		    if (email == null || email.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty email parameter\" }";
		    }

		    // Queries
		    String deviationQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
		            "JOIN " + deviationTable + " dev ON dev.deviationid = casd.changenumber " +
		            "WHERE spd.email_address = ?";
		    
		    String changeactionQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + caSupplierTable + " casd ON spd.companyid = casd.companyid " +
		            "JOIN " + changeactionTable + " ca ON ca.caid = casd.changenumber " +
		            "WHERE spd.email_address = ?";

		    String ecPartsCountQuery = "SELECT COUNT(*) " +
		            "FROM " + supplierTable + " spd " +
		            "JOIN " + companyDetailsTable + " cd ON spd.companyid = cd.companyid " +
		            "JOIN " + mpnTable + " mpn ON cd.name = mpn.manufacturername " +
		            "JOIN " + mpnRelatedPartsTable + " mrp ON mpn.mpnid = mrp.mpnid " +
		            "JOIN " + ecPartTable + " part ON part.partid=mrp.partid " +
		            "WHERE spd.email_address = ?";

		    Class.forName("org.postgresql.Driver");
		    Connection conn = DriverManager.getConnection(url, userName, password);
		    try {
		        // Prepare statements
		        PreparedStatement supplierStmt = conn.prepareStatement(deviationQuery);
		        PreparedStatement caStmt = conn.prepareStatement(changeactionQuery);
		        PreparedStatement ecPartsStmt = conn.prepareStatement(ecPartsCountQuery);
		        
		        supplierStmt.setString(1, email);
		        caStmt.setString(1, email);
		        ecPartsStmt.setString(1, email);

		        // Execute queries
		        ResultSet supplierResult = supplierStmt.executeQuery();
		        ResultSet caResult = caStmt.executeQuery();
		        ResultSet ecPartsResult = ecPartsStmt.executeQuery();

		        // Initialize counts
		        int deviationCount = 0;
		        int changeActionCount = 0;
		        int ecPartsCount = 0;

		        // Get the count for deviations
		        if (supplierResult.next()) {
		            deviationCount = supplierResult.getInt(1);
		        }

		        // Get the count for change actions
		        if (caResult.next()) {
		            changeActionCount = caResult.getInt(1);
		        }

		        // Get the count for EC parts
		        if (ecPartsResult.next()) {
		            ecPartsCount = ecPartsResult.getInt(1);
		        }

		        // Build the JSON response
		        String jsonResponse = String.format(
		            "{ \"results\": [{ \"changeaction\": %d, \"deviation\": %d, \"ecparts\": %d }] }",
		            changeActionCount, deviationCount, ecPartsCount
		        );

		        return jsonResponse;
		    } finally {
		        // Clean up resources
		        if (conn != null) conn.close();
		    }
		}

		
		@GET
		@Path("getSupplierName")
		@Produces(MediaType.APPLICATION_JSON)
		public String getSupplierName(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    
		    String supplierPersonTable = pro.getProperty("supplierpersondetailsTable");
		    String companyTable = pro.getProperty("companyTable");

		    // Get email from query parameters
		    String email = uriInfo.getQueryParameters().getFirst("username");
		    if (email == null || email.trim().isEmpty()) {
		    	 return "{ \"error\": \"Missing or empty caID parameter\" }";		   
		    	}

		    // Establish database connection
		    

		    // SQL query
		    String query = "SELECT c.name " +
		                   "FROM " + supplierPersonTable + " sp " +
		                   "JOIN " + companyTable + " c ON sp.companyid = c.companyid " +
		                   "WHERE sp.email_address = ?";
		    Class.forName("org.postgresql.Driver");
		    Connection conn = DriverManager.getConnection(url, userName, password);
		    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
		        pstmt.setString(1, email);  // Set the email in the query
		        ResultSet resultSet = pstmt.executeQuery();

		        // Create JSON structure
		        JSONObject jsonResponse = new JSONObject();
		        JSONArray resultsArray = new JSONArray();

		        if (resultSet.next()) {
		            String companyName = resultSet.getString("name");
		            System.out.println(companyName);
		            // Create JSON object for the result
		            JSONObject resultObject = new JSONObject();
		            resultObject.put("suppliername", companyName);

		            // Add result object to results array
		            resultsArray.put(resultObject);
		        } else {
		            // Handle case where no supplier was found
		            JSONObject resultObject = new JSONObject();
		            resultObject.put("suppliername", "No supplier found for the provided email");
		            resultsArray.put(resultObject);
		        }

		        // Add results array to the response object
		        jsonResponse.put("results", resultsArray);

		        // Return the JSON response
		        return jsonResponse.toString();

		    } catch (SQLException e) {
		        e.printStackTrace();

		        // Create error response in case of exception
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("error", "An error occurred while fetching the supplier details");
		        return errorResponse.toString();
		    } finally {
		        if (conn != null && !conn.isClosed()) {
		            conn.close();
		        }
		    }
		}
		
		
		@GET
		@Path("getAssignedPartsVisibility")
		@Produces(MediaType.APPLICATION_JSON)
		public String getAssignedPartsVisibility(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");
			
		    // Load properties file
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);
		    
		    String mpnTable = pro.getProperty("MpnTable"); 
			String connectionTablename = pro.getProperty("MpnconnectionTable");

		    // Get email from query parameters
		    String suppliername = uriInfo.getQueryParameters().getFirst("suppliername");
		    String partid = uriInfo.getQueryParameters().getFirst("partid");
		    if (suppliername == null || suppliername.trim().isEmpty() || partid == null || partid.trim().isEmpty()) {
		    	 return "{ \"error\": \"Missing or empty caID parameter\" }";
		    }

		    // Establish database connection
		    
	        String sql = "SELECT ct.partid " +
	                     "FROM " + connectionTablename + " ct " +
	                     "JOIN " + mpnTable + " mt ON ct.mpnid = mt.mpnid " +
	                     "WHERE ct.partid = ? " +
	                     "AND mt.manufacturername = ?";

	        String partId = null;
		    Class.forName("org.postgresql.Driver");
		    Connection conn = DriverManager.getConnection(url, userName, password);
		    try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
	            preparedStatement.setString(1, partid);  // Set the partid parameter
	            preparedStatement.setString(2, suppliername);  // Set the suppliername parameter
	            
	            try (ResultSet resultSet = preparedStatement.executeQuery()) {
	                if (resultSet.next()) {
	                	System.out.println(resultSet.getString("partid"));
	                    partId = resultSet.getString("partid");  // Retrieve the partid from the result set
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();  // Handle exceptions appropriately
	        }

	        // Create the desired JSON structure
	        JSONObject jsonResponse = new JSONObject();
	        JSONArray resultsArray = new JSONArray();
	        JSONObject resultObject = new JSONObject();

	        if (partId != null) {
	            resultObject.put("Visibility", "true");
	            resultObject.put("id",partId);
	        } else {
	            resultObject.put("Visibility", "false");
	            resultObject.put("id",partId);
	        }

	        resultsArray.put(resultObject);
	        jsonResponse.put("results", resultsArray);

	        return jsonResponse.toString();
		}
		
		@POST
		@Path("searchForCA")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Response getDataCA(String s) throws IOException {
		    if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("status", "fail");
		        errorResponse.put("message", "Invalid JSON input.");
		        return Response.status(Response.Status.BAD_REQUEST)
		                       .entity(errorResponse.toString())
		                       .type(MediaType.APPLICATION_JSON)
		                       .build();
		    }

		    JSONObject json = new JSONObject(s);
		    String text = json.optString("text", "%"); // Default to '%' if text is empty
		    String field = json.optString("field");

		    // Validate input for 'Name' field
		    if (field.equalsIgnoreCase("Name")) {
		        if (!text.startsWith("CA-") || text.contains("*00*")) {
		            JSONObject errorResponse = new JSONObject();
		            errorResponse.put("status", "fail");
		            errorResponse.put("message", "For 'Name' field, the text must start with 'CA-' and cannot contain '*00*'.");
		            return Response.status(Response.Status.BAD_REQUEST)
		                           .entity(errorResponse.toString())
		                           .type(MediaType.APPLICATION_JSON)
		                           .build();
		        }
		    }

		    // Ensure the search text contains at least 3 characters
		    if (text.equals("*") || text.length() < 3) {
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("status", "fail");
		        errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
		        return Response.status(Response.Status.BAD_REQUEST)
		                       .entity(errorResponse.toString())
		                       .type(MediaType.APPLICATION_JSON)
		                       .build();
		    }

		    // Handle 'Everything' field - special case for caid and name
		    if (field.equalsIgnoreCase("Everything")) {
		        if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.\\d{4}")) {
		            // If text matches exact caid format, use an exact match
		            text = text;
		        } else if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.*")) {
		            // If it's a partial caid like 24724.43239.18190.*, return empty results
		            JSONObject js = new JSONObject();
		            js.put("results", new JSONArray());
		            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		        } else if (text.startsWith("CA-")) {
		            // If the text starts with CA-, apply wildcard search
		            text = text.replace("*", "%");
		        } else {
		            // If the input is invalid for Everything, return empty results
		            JSONObject js = new JSONObject();
		            js.put("results", new JSONArray());
		            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		        }
		    } else {
		        // For other fields, use LIKE with wildcards
		        text = text.replace("*", "%");
		        if (!text.startsWith("%")) {
		            text = "%" + text;
		        }
		        if (!text.endsWith("%")) {
		            text = text + "%";
		        }
		    }

		    String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");

		    JSONArray jsonArray = new JSONArray();

		    try {
		        Class.forName("org.postgresql.Driver");
		        Connection con = DriverManager.getConnection(url, userName, password);

		        String sql = "";

		        if (field.equalsIgnoreCase("Everything")) {
		            // Exact caid match or partial search for 'name' starting with CA-
		            sql = "SELECT caid, name, type FROM changeaction WHERE (caid = ? OR name ILIKE ?) AND name ILIKE 'CA-%'";
		        } else if (field.equalsIgnoreCase("name")) {
		            // Search by 'name' field, only names starting with 'CA-'
		            sql = "SELECT caid, name, type FROM changeaction WHERE name ILIKE ? AND name ILIKE 'CA-%'";
		        }

		        if (!sql.isEmpty()) {
		            try (PreparedStatement ps = con.prepareStatement(sql)) {
		                if (field.equalsIgnoreCase("Everything")) {
		                    ps.setString(1, text);  // Use exact 'caid' or processed 'name' search
		                    ps.setString(2, text);  // For CA- based name
		                } else {
		                    ps.setString(1, text);  // Use processed text for 'name' search
		                }

		                ResultSet set = ps.executeQuery();
		                Map<String, List<String>> caidsByType = new HashMap<>();

		                while (set.next()) {
		                    String caid = set.getString("caid");
		                    String type = set.getString("type");

		                    // Check if the caid exists in ca_suppliers_details with acknowledge = 'Yes' or 'No'
		                    String checkSql = "SELECT COUNT(*) FROM ca_suppliers_details WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
		                    try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
		                        checkPs.setString(1, caid);
		                        ResultSet checkSet = checkPs.executeQuery();
		                        if (checkSet.next() && checkSet.getInt(1) > 0) {
		                            // If the caid exists and acknowledge is 'Yes' or 'No', add it to the list
		                            caidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(caid);
		                        }
		                    }
		                }

		                // Construct JSON output
		                for (Map.Entry<String, List<String>> entry : caidsByType.entrySet()) {
		                    String type = entry.getKey();
		                    List<String> caids = entry.getValue();

		                    JSONObject jsonObject = new JSONObject();
		                    JSONObject typeObject = new JSONObject();
		                    typeObject.put("caid", String.join("|", caids));
		                    jsonObject.put("type: " + type, typeObject);
		                    jsonArray.put(jsonObject);
		                }
		            }
		        }

		        JSONObject js = new JSONObject();
		        js.put("results", jsonArray);
		        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

		    } catch (Exception e) {
		        e.printStackTrace();
		        JSONObject js = new JSONObject();
		        js.put("status", "fail");
		        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		    }
		}

		
		@POST
		@Path("searchForDeviation")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces(MediaType.APPLICATION_JSON)
		public Response getDataDev(String s) throws IOException {
		    if (s == null || s.trim().isEmpty() || !s.trim().startsWith("{")) {
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("status", "fail");
		        errorResponse.put("message", "Invalid JSON input.");
		        return Response.status(Response.Status.BAD_REQUEST)
		                       .entity(errorResponse.toString())
		                       .type(MediaType.APPLICATION_JSON)
		                       .build();
		    }

		    JSONObject json = new JSONObject(s);
		    String text = json.optString("text", "%"); // Default to '%' if text is empty
		    String field = json.optString("field");

		    // Validate input for 'Name' field
		    if (field.equalsIgnoreCase("Name")) {
		        if (!text.startsWith("DEV-") || text.contains("*00*")) {
		            JSONObject errorResponse = new JSONObject();
		            errorResponse.put("status", "fail");
		            errorResponse.put("message", "For 'Name' field, the text must start with 'DEV-' and cannot contain '*00*'.");
		            return Response.status(Response.Status.BAD_REQUEST)
		                           .entity(errorResponse.toString())
		                           .type(MediaType.APPLICATION_JSON)
		                           .build();
		        }
		    }

		    // Ensure the search text contains at least 3 characters
		    if (text.equals("*") || text.length() < 3) {
		        JSONObject errorResponse = new JSONObject();
		        errorResponse.put("status", "fail");
		        errorResponse.put("message", "Please provide at least 3 characters or digits for the search.");
		        return Response.status(Response.Status.BAD_REQUEST)
		                       .entity(errorResponse.toString())
		                       .type(MediaType.APPLICATION_JSON)
		                       .build();
		    }

		    // Handle 'Everything' field - special case for deviationid and name
		    if (field.equalsIgnoreCase("Everything")) {
		        if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.\\d{4}")) {
		            // If text matches exact deviationid format, use an exact match
		            text = text;
		        } else if (text.matches("\\d{5}\\.\\d{5}\\.\\d{5}\\.*")) {
		            // If it's a partial deviationid like 24724.43239.18190.*, return empty results
		            JSONObject js = new JSONObject();
		            js.put("results", new JSONArray());
		            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		        } else if (text.startsWith("DEV-")) {
		            // If the text starts with DEV-, apply wildcard search
		            text = text.replace("*", "%");
		        } else {
		            // If the input is invalid for Everything, return empty results
		            JSONObject js = new JSONObject();
		            js.put("results", new JSONArray());
		            return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		        }
		    } else {
		        // For other fields, use LIKE with wildcards
		        text = text.replace("*", "%");
		        if (!text.startsWith("%")) {
		            text = "%" + text;
		        }
		        if (!text.endsWith("%")) {
		            text = text + "%";
		        }
		    }

		    String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");

		    JSONArray jsonArray = new JSONArray();

		    try {
		        Class.forName("org.postgresql.Driver");
		        Connection con = DriverManager.getConnection(url, userName, password);

		        String sql = "";

		        if (field.equalsIgnoreCase("Everything")) {
		            // Exact deviationid match or partial search for 'name' starting with DEV-
		            sql = "SELECT deviationid, name, type FROM deviation_details WHERE (deviationid = ? OR name ILIKE ?) AND name ILIKE 'DEV-%'";
		        } else if (field.equalsIgnoreCase("name")) {
		            // Search by 'name' field, only names starting with 'DEV-'
		            sql = "SELECT deviationid, name, type FROM deviation_details WHERE name ILIKE ? AND name ILIKE 'DEV-%'";
		        }

		        if (!sql.isEmpty()) {
		            try (PreparedStatement ps = con.prepareStatement(sql)) {
		                if (field.equalsIgnoreCase("Everything")) {
		                    ps.setString(1, text);  // Use exact 'deviationid' or processed 'name' search
		                    ps.setString(2, text);  // For DEV- based name
		                } else {
		                    ps.setString(1, text);  // Use processed text for 'name' search
		                }

		                ResultSet set = ps.executeQuery();
		                Map<String, List<String>> deviationidsByType = new HashMap<>();

		                while (set.next()) {
		                    String deviationid = set.getString("deviationid");
		                    String type = set.getString("type");

		                    // Check if the deviationid exists in ca_suppliers_details with acknowledge = 'Yes' or 'No'
		                    String checkSql = "SELECT COUNT(*) FROM ca_suppliers_details WHERE changenumber = ? AND (acknowledge = 'Yes' OR acknowledge = 'No')";
		                    try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
		                        checkPs.setString(1, deviationid);
		                        ResultSet checkSet = checkPs.executeQuery();
		                        if (checkSet.next() && checkSet.getInt(1) > 0) {
		                            // If the deviationid exists and acknowledge is 'Yes' or 'No', add it to the list
		                            deviationidsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(deviationid);
		                        }
		                    }
		                }

		                // Construct JSON output
		                for (Map.Entry<String, List<String>> entry : deviationidsByType.entrySet()) {
		                    String type = entry.getKey();
		                    List<String> deviationids = entry.getValue();

		                    JSONObject jsonObject = new JSONObject();
		                    JSONObject typeObject = new JSONObject();
		                    typeObject.put("deviationid", String.join("|", deviationids));
		                    jsonObject.put("type: " + type, typeObject);
		                    jsonArray.put(jsonObject);
		                }
		            }
		        }

		        JSONObject js = new JSONObject();
		        js.put("results", jsonArray);
		        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();

		    } catch (Exception e) {
		        e.printStackTrace();
		        JSONObject js = new JSONObject();
		        js.put("status", "fail");
		        return Response.ok(js.toString(), MediaType.APPLICATION_JSON).build();
		    }
		}

		// Web service method to check if cd.name equals mpn.manufacturername
		@GET
		@Path("getSupplierusercheckforebom")
		@Produces(MediaType.APPLICATION_JSON)
		public String getSupplierUserCheckForEBOM(@Context UriInfo uriInfo) throws Exception {
			String url=System.getenv("SupplierPortalDBURL");
			String password=System.getenv("SupplierPortalDBPassword");
			String userName= System.getenv("SupplierPortalDBUsername");

		    // Load properties file (if necessary)
		    Properties pro = new Properties();
		    InputStream input = getClass().getClassLoader().getResourceAsStream("sp.properties");
		    if (input == null) {
		        throw new FileNotFoundException("sp.properties file not found.");
		    }
		    pro.load(input);

		    // Get partId from query parameters
		    String partId = uriInfo.getQueryParameters().getFirst("partid");
		    if (partId == null || partId.trim().isEmpty()) {
		        return "{ \"error\": \"Missing or empty partId parameter\" }";
		    }

		    // Define SQL query to fetch cd.name and mpn.manufacturername
		    String nameAndManufacturerQuery = "SELECT cd.name, mpn.manufacturername " +
		                                      "FROM mpn_related_parts_details mrpd " +
		                                      "JOIN mpn ON mrpd.mpnid = mpn.mpnid " +
		                                      "JOIN company_details cd ON mpn.manufacturername = cd.name " +
		                                      "JOIN supplier_person_details spd ON cd.companyid = spd.companyid " +
		                                      "WHERE mrpd.partid = ?";

		    // Define a separate SQL query to fetch module_end_item from ec_parts_details
		    String moduleEndItemQuery = "SELECT ec.module_end_item " +
		                                "FROM ec_parts_details ec " +
		                                "WHERE ec.partid = ?";

		    Class.forName("org.postgresql.Driver");

		    // Initialize response variables
		    String cdName = "";
		    String manufacturerName = "";
		    String moduleEndItem = "";
		    boolean isMatch = false;

		    // Connect to the database and execute the first query
		    try (Connection conn = DriverManager.getConnection(url, userName, password);
		         PreparedStatement nameAndManufacturerStmt = conn.prepareStatement(nameAndManufacturerQuery);
		         PreparedStatement moduleEndItemStmt = conn.prepareStatement(moduleEndItemQuery)) {

		        // Set the partId parameter in the first query
		        nameAndManufacturerStmt.setString(1, partId);
		        ResultSet rs = nameAndManufacturerStmt.executeQuery();

		        // Process the result set for cd.name and mpn.manufacturername
		        if (rs.next()) {
		            cdName = rs.getString("name");
		            manufacturerName = rs.getString("manufacturername");

		            // Check if cd.name equals mpn.manufacturername
		            if (cdName.equals(manufacturerName)) {
		                isMatch = true;
		            }
		        }

		        // Set the partId parameter in the second query
		        moduleEndItemStmt.setString(1, partId);
		        ResultSet rsModule = moduleEndItemStmt.executeQuery();

		        // Process the result set for module_end_item
		        if (rsModule.next()) {
		            moduleEndItem = rsModule.getString("module_end_item");
		        }

		    } catch (SQLException e) {
		        e.printStackTrace();
		        return "{ \"error\": \"Database error: " + e.getMessage() + "\" }";
		    }

		    // Create JSON response
		    JSONObject jsonResponse = new JSONObject();
		    jsonResponse.put("name in companydetails", cdName);
		    jsonResponse.put("manufacturername in mpn", manufacturerName);
		    jsonResponse.put("isMatch", isMatch);
		    jsonResponse.put("module_end_item", moduleEndItem); // Add the module_end_item attribute

		    return jsonResponse.toString();
		}

}

