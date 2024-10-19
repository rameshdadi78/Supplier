package Sp.Supplierportal;


import jakarta.ws.rs.GET;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * This class provides API endpoints to fetch change action details and affected item attributes.
 */
@Path("parts")
public class ECPartInfo {

    /**
     * Fetches the change action details based on the part ID provided as a query parameter.
     * 
     * @param uriInfo Contains the query parameters including part ID.
     * @return A JSON string containing change action details and affected item attributes.
     * @throws Exception If any error occurs during database interaction or file loading.
     */
    @GET
    @Path("changeActions")
    @Produces(MediaType.APPLICATION_JSON)
    public String getChangeActionDetails(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties")) {
            if (input == null) {
                throw new FileNotFoundException("CAMappingNames.properties file not found.");
            }
            pro.load(input);
        }

        String allAffectedItemsTable = pro.getProperty("all_AffectedItems_Table");
        String changeActionTable = pro.getProperty("change_Action_Table");
        String[] changeActionColumnNames = pro.getProperty("change_Action_Attributes_Mapping").split(",");
        String[] allAffectedItemsColumnNames = pro.getProperty("allAffectedItems_Attributes_Mapping").split(",");

        // Map to store column names and their display names
        Map<String, String> columnMap = new HashMap<>();
        for (String mapping : allAffectedItemsColumnNames) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                columnMap.put(parts[1].trim(), parts[0].trim());
            }
        }

        Map<String, String> changeActionMap = new HashMap<>();
        for (String mapping : changeActionColumnNames) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                changeActionMap.put(parts[1].trim(), parts[0].trim());
            }
        }

        Map<String, String> attributeDisplayNames = new HashMap<>();
        for (String key : pro.stringPropertyNames()) {
            if (key.startsWith("Attribute_")) {
                String columnName = key.substring("Attribute_".length()).toLowerCase();
                attributeDisplayNames.put(columnName, pro.getProperty(key));
            }
        }

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String partid = queryParams.getFirst("partid");

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(allAffectedItemsTable);
        if (partid != null && !partid.isEmpty()) {
            sql.append(" WHERE topartid = '").append(partid.replace("'", "''")).append("'");
        }

        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery(sql.toString())) {

            while (result.next()) {
                String currentPartid = result.getString("topartid");
                String caid = result.getString("caid");

                JSONObject jsonObject = new JSONObject();

                // Fetch the change action data and relationship data
                JSONObject changeActionData = fetchChangeActionData(conn, changeActionTable, caid, changeActionMap, attributeDisplayNames);
                JSONObject relationshipData = fetchRelationshipData(conn, allAffectedItemsTable, currentPartid, columnMap);

                // Merge the change action data into the main JSON object
                for (String key : changeActionData.keySet()) {
                    jsonObject.put(key, changeActionData.get(key));
                }

                // Merge the relationship data into the main JSON object
                for (String key : relationshipData.keySet()) {
                    jsonObject.put(key, relationshipData.get(key));
                }

                // Add the partid
                jsonObject.put("partid", currentPartid);

                // Create the final object structure with caid
                JSONObject idObject = new JSONObject();
                idObject.put("caid: " + caid, jsonObject);
                jsonArray.put(idObject);
            }
        } catch (Exception e) {
            System.err.println("Error fetching change action details: " + e.getMessage());
            e.printStackTrace();
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("objectdetails", jsonArray);
        return finalObject.toString();
    }

    /**
     * Fetches the change action data from the given table based on CAID.
     * 
     * @param conn Database connection.
     * @param tableName The name of the table to query.
     * @param caid The change action ID.
     * @param columnMappings The map of column names to display names.
     * @param attributeDisplayNames The map of attributes and their display names.
     * @return A JSON object containing change action data.
     * @throws SQLException If any SQL error occurs.
     */
    private JSONObject fetchChangeActionData(Connection conn, String tableName, String caid,
                                             Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
        JSONObject changeActionData = new JSONObject();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE caid = '").append(caid.replace("'", "''")).append("'");

        try (Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet resultSet = stmt.executeQuery(sql.toString())) {

            if (resultSet.next()) {
                JSONArray basicAttributesArray = new JSONArray();
                JSONArray attributesArray = new JSONArray();

                for (String column : columnMappings.keySet()) {
                    // Exclude certain attributes
                    if (!Arrays.asList("caid", "direction", "revision").contains(column.toLowerCase())) {
                        JSONObject attribute = new JSONObject();
                        String displayName = attributeDisplayNames.getOrDefault(column, columnMappings.get(column));
                        String value = resultSet.getString(column);

                        attribute.put("displayName", displayName);
                        attribute.put("name", column);
                        attribute.put("value", value);

                        if (Arrays.asList("name", "type", "state", "synopsis", "project", "description", "owner").contains(column)) {
                            basicAttributesArray.put(attribute);
                        } else {
                            attributesArray.put(attribute);
                        }
                    }
                }

                changeActionData.put("BasicAttributesOfChangeAction", basicAttributesArray);
                changeActionData.put("attributesOfCA", attributesArray);
            }
        }
        return changeActionData;
    }

    /**
     * Fetches relationship attributes for the given part ID.
     * 
     * @param conn Database connection.
     * @param tableName The name of the table to query.
     * @param topartid The part ID.
     * @param columnMap The map of column names to display names.
     * @return A JSON object containing relationship data.
     * @throws SQLException If any SQL error occurs.
     */
    private JSONObject fetchRelationshipData(Connection conn, String tableName, String topartid,
                                             Map<String, String> columnMap) throws SQLException {
        JSONObject relationshipData = new JSONObject();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE topartid = '").append(topartid.replace("'", "''")).append("'");

        try (Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(sql.toString())) {

            while (resultSet.next()) {
                JSONArray attributesArray = new JSONArray();
                for (String column : columnMap.keySet()) {
                    // Exclude certain columns
                    if (Arrays.asList("caid", "relationshipid", "topartid", "direction").contains(column.toLowerCase())) {
                        continue;
                    }

                    JSONObject attribute = new JSONObject();
                    String displayName = columnMap.get(column);
                    String value = resultSet.getString(column);

                    attribute.put("displayName", displayName);
                    attribute.put("name", column);
                    attribute.put("value", value);

                    attributesArray.put(attribute);
                }
                relationshipData.put("relattributesOfAffectedItems", attributesArray);
            }
        }
        return relationshipData;
    }
	
    /**
     * Retrieves specification details based on part ID.
     *
     * @param uriInfo UriInfo containing query parameters.
     * @return JSON response with specification details.
     * @throws Exception If any error occurs during the execution.
     */
    @GET
    @Path("specifications")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSpecificationDetails(@Context UriInfo uriInfo) throws Exception {
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = loadPropertiesFile();

        String specRelationshipTable = pro.getProperty("Specifications_Relationship_table");
        String specificationTable = pro.getProperty("Specification_Table");
        String[] specificationColumnNames = pro.getProperty("Specification_Attributes_Mapping").split(",");
        String[] specRelationshipColumnNames = pro.getProperty("Specification_Relationship_Attributes_Mapping").split(",");

        // Ensure property values are correctly loaded
        validatePropertyValues(specRelationshipTable, specificationTable);

        // Map to store the column names and their display names
        Map<String, String> columnMap = createColumnMap(specRelationshipColumnNames);
        Map<String, String> specificationMap = createColumnMap(specificationColumnNames);
        Map<String, String> attributeDisplayNames = loadAttributeDisplayNames(pro);

        // Get query parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String partid = queryParams.getFirst("partid");

        // SQL query construction
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(specRelationshipTable);
        if (partid != null && !partid.trim().isEmpty()) {
            sql.append(" WHERE topartid = '").append(partid.replace("'", "''")).append("'");
        }

        // Execute query and build response
        return executeSpecificationQuery(url, userName, password, sql.toString(), specificationTable, specificationMap, columnMap, attributeDisplayNames);
    }

    /**
     * Executes the SQL query to fetch specification details and build the response.
     *
     * @param url Database URL.
     * @param userName Database username.
     * @param password Database password.
     * @param sql SQL query string.
     * @param specificationTable Name of the specification table.
     * @param specificationMap Mapping of specification columns.
     * @param columnMap Mapping of relationship columns.
     * @param attributeDisplayNames Mapping of column display names.
     * @return JSON string response.
     * @throws Exception If any error occurs during the execution.
     */
    private String executeSpecificationQuery(String url, String userName, String password, String sql, String specificationTable, Map<String, String> specificationMap, Map<String, String> columnMap, Map<String, String> attributeDisplayNames) throws Exception {
        ResultSet result = null;
        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement stmt = conn.createStatement()) {
            result = stmt.executeQuery(sql);

            while (result.next()) {
                String currentPartid = result.getString("topartid");
                String specificationid = result.getString("specificationid");

                JSONObject jsonObject = new JSONObject();

                // Fetch the specification data and relationship data
                JSONObject specificationData = fetchSpecificationData(conn, specificationTable, specificationid, specificationMap, attributeDisplayNames);
                JSONArray relationshipData = fetchSpecRelationshipData(conn, sql, currentPartid, columnMap);

                jsonObject.put("BasicAttributesOfSpecification", specificationData.getJSONArray("BasicAttributesOfSpecification"));
                jsonObject.put("attributesOfSpec", specificationData.getJSONArray("attributesOfSpec"));
                jsonObject.put("relattributesOfSpec", relationshipData);
                jsonObject.put("partid", currentPartid);

                // Create the final object structure with specificationid
                JSONObject idObject = new JSONObject();
                idObject.put("specificationid: " + specificationid, jsonObject);
                jsonArray.put(idObject);
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("objectdetails", jsonArray);
        return finalObject.toString();
    }

    /**
     * Loads properties from the file.
     *
     * @return Loaded properties.
     * @throws IOException If the properties file is not found or cannot be read.
     */
    private Properties loadPropertiesFile() throws IOException {
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties")) {
            if (input == null) {
                throw new FileNotFoundException("CAMappingNames.properties file not found.");
            }
            pro.load(input);
        }
        return pro;
    }

    /**
     * Ensures that property values for tables are loaded.
     *
     * @param specRelationshipTable Name of the specification relationship table.
     * @param specificationTable Name of the specification table.
     */
    private void validatePropertyValues(String specRelationshipTable, String specificationTable) {
        if (specRelationshipTable == null || specificationTable == null) {
            throw new IllegalArgumentException("Required table names are not found in properties file.");
        }
    }

    /**
     * Loads attribute display names from properties.
     *
     * @param pro Properties object.
     * @return Mapping of attribute display names.
     */
    private Map<String, String> loadAttributeDisplayNames(Properties pro) {
        Map<String, String> attributeDisplayNames = new HashMap<>();
        for (String key : pro.stringPropertyNames()) {
            if (key.startsWith("Attribute_")) {
                String columnName = key.substring("Attribute_".length()).toLowerCase();
                attributeDisplayNames.put(columnName, pro.getProperty(key));
            }
        }
        return attributeDisplayNames;
    }

    /**
     * Fetches specification data for the given specification ID.
     *
     * @param conn Database connection.
     * @param tableName Name of the table.
     * @param specificationid Specification ID.
     * @param columnMappings Mapping of columns.
     * @param attributeDisplayNames Mapping of column display names.
     * @return JSON object containing specification data.
     * @throws SQLException If any SQL error occurs.
     */
    private JSONObject fetchSpecificationData(Connection conn, String tableName, String specificationid, Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
		return null;
        // Code for fetching specification data
    }

    /**
     * Fetches relationship attributes for the given part ID.
     *
     * @param conn Database connection.
     * @param sql SQL query string.
     * @param topartid Part ID.
     * @param columnMap Mapping of relationship columns.
     * @return JSONArray containing relationship attributes.
     * @throws SQLException If any SQL error occurs.
     */
    private JSONArray fetchSpecRelationshipData(Connection conn, String sql, String topartid, Map<String, String> columnMap) throws SQLException {
		return null;
        // Code for fetching relationship attributes
    }

    /**
     * Creates a column map from a string array.
     *
     * @param columnNames Array of column mappings.
     * @return Map of column mappings.
     */
    private Map<String, String> createColumnMap(String[] columnNames) {
        Map<String, String> columnMap = new HashMap<>();
        for (String mapping : columnNames) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                columnMap.put(parts[1].trim(), parts[0].trim());
            }
        }
        return columnMap;
    }
	    
    /**
     * Downloads the specified file from the server.
     *
     * @return Response containing the file to be downloaded or an error message if the file is not found.
     */
    @GET
    @Path("/download")
    @Produces("application/octet-stream")
    public Response downloadFile() {

        // File path declaration
        String filePath = "C:\\Users\\DELL\\Downloads\\ABC.txt";
        File file = new File(filePath);

        // Check if file exists
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File not found at the specified path.")
                    .build();
        }

        // Returning the file as a downloadable response
        return Response.ok(file)
                .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                .build();
    }

    /**
     * Retrieves alternate parts and their relationship details based on query parameters.
     * 
     * @param uriInfo context containing query parameters
     * @return JSON string with alternate parts and their attributes
     * @throws Exception if an error occurs while retrieving data
     */
    @GET
    @Path("alternates")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAlternateParts(@Context UriInfo uriInfo) throws Exception {
        // Database connection variables
        String url = System.getenv("SupplierPortalDBURL");
        String password = System.getenv("SupplierPortalDBPassword");
        String userName = System.getenv("SupplierPortalDBUsername");

        // Load properties file
        Properties pro = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("CAMappingNames.properties")) {
            if (input == null) {
                throw new FileNotFoundException("CAMappingNames.properties file not found.");
            }
            pro.load(input);
        }

        // Extract necessary property values
        String tablename = pro.getProperty("ecpartTable");
        String[] columnMappings = pro.getProperty("columns").split(",");
        String alternateRelTable = pro.getProperty("Alternate_Rel_Table");
        String alternateReffTable = pro.getProperty("Alternate_Reff_Table");
        String[] alternateReffAttributesMapping = pro.getProperty("Alternate_Reff_Attributes_Mapping").split(",");
        String[] alternateRelAttributesMapping = pro.getProperty("Alternate_Rel_Attributes_Mapping").split(",");

        // Prepare column mappings
        Map<String, String> columnMap = prepareColumnMap(columnMappings);
        Map<String, String> allAffectedItemsMap = prepareColumnMap(alternateRelAttributesMapping);
        Map<String, String> alternatePartsMap = prepareColumnMap(alternateReffAttributesMapping);

        Map<String, String> attributeDisplayNames = prepareAttributeDisplayNames(pro);

        // Build SQL query with query parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tablename).append(" WHERE 1=1");
        appendQueryParamsToSql(queryParams, sql);

        // Fetch results
        JSONArray jsonArray = new JSONArray();
        try (Connection conn = DriverManager.getConnection(url, userName, password);
             Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery(sql.toString())) {

            while (result.next()) {
                String fetchedPartid = result.getString("partid");

                String alternateId = fetchAlternateIdFromTable(conn, alternateRelTable, fetchedPartid);
                JSONObject jsonObject = buildJsonResponse(conn, pro, result, columnMap, attributeDisplayNames, alternateId, alternatePartsMap, alternateReffTable, allAffectedItemsMap, alternateRelTable, fetchedPartid);
                jsonArray.put(jsonObject);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JSONObject finalObject = new JSONObject();
        finalObject.put("results", jsonArray);
        return finalObject.toString();
    }

    private Map<String, String> prepareColumnMap(String[] mappings) {
        Map<String, String> columnMap = new HashMap<>();
        for (String mapping : mappings) {
            String[] parts = mapping.split("\\|");
            if (parts.length == 2) {
                columnMap.put(parts[1].trim(), parts[0].trim());
            }
        }
        return columnMap;
    }

    private Map<String, String> prepareAttributeDisplayNames(Properties pro) {
        Map<String, String> attributeDisplayNames = new HashMap<>();
        for (String key : pro.stringPropertyNames()) {
            if (key.startsWith("Attribute_")) {
                String columnName = key.substring("Attribute_".length()).toLowerCase();
                attributeDisplayNames.put(columnName, pro.getProperty(key));
            }
        }
        return attributeDisplayNames;
    }

    private void appendQueryParamsToSql(MultivaluedMap<String, String> queryParams, StringBuilder sql) {
        queryParams.forEach((key, values) -> {
            String value = values.get(0);
            if (value != null && !value.trim().isEmpty()) {
                sql.append(" AND ").append(key).append(" = '").append(value.replace("'", "''")).append("'");
            }
        });
    }

    private String fetchAlternateIdFromTable(Connection conn, String tableName, String partid) throws SQLException {
        String alternateId = null;
        String query = "SELECT Alternate_Id FROM " + tableName + " WHERE topartid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, partid);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                alternateId = resultSet.getString("Alternate_Id");
            }
        }
        return alternateId;
    }

    private JSONObject buildJsonResponse(Connection conn, Properties pro, ResultSet result, Map<String, String> columnMap, 
                                         Map<String, String> attributeDisplayNames, String alternateId, Map<String, String> alternatePartsMap, 
                                         String alternateReffTable, Map<String, String> allAffectedItemsMap, String alternateRelTable, 
                                         String fetchedPartid) throws SQLException {
        JSONObject jsonObject = new JSONObject();
        JSONArray basicAttributesArray = new JSONArray();
        JSONArray attributesArray = new JSONArray();

        // Process part attributes
        for (String column : columnMap.keySet()) {
            JSONObject attribute = new JSONObject();
            String columnValue = result.getString(column);
            String displayName = attributeDisplayNames.getOrDefault(column, columnMap.get(column));

            attribute.put("displayName", displayName);
            attribute.put("name", column);
            attribute.put("value", columnValue);

            if (Arrays.asList(pro.getProperty("basicAttributes").split(",")).contains(column)) {
                basicAttributesArray.put(attribute);
            } else {
                attributesArray.put(attribute);
            }
        }

        jsonObject.put("basicAttributes", basicAttributesArray);
        jsonObject.put("attributes", attributesArray);

        if (alternateId != null) {
            JSONObject alternatePartsArray = fetchAlternatePartData(conn, alternateReffTable, alternateId, alternatePartsMap, attributeDisplayNames);
            jsonObject.put("alternateParts", alternatePartsArray);
        }

        JSONObject relationshipAttributesArray = fetchAlternateRelationshipData(conn, alternateRelTable, fetchedPartid, allAffectedItemsMap);
        jsonObject.put("relationshipattributes", relationshipAttributesArray);

        return jsonObject;
    }

    private JSONObject fetchAlternatePartData(Connection conn, String tableName, String alternateId, Map<String, String> columnMappings, Map<String, String> attributeDisplayNames) throws SQLException {
        JSONObject alternatesPartsData = new JSONObject();
        String sql = "SELECT * FROM " + tableName + " WHERE Alternate_Id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, alternateId);
            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                JSONArray attributesArray = new JSONArray();
                for (Entry<String, String> entry : columnMappings.entrySet()) {
                    String columnName = entry.getKey();
                    String displayName = entry.getValue();
                    String value = resultSet.getString(columnName);

                    JSONObject attribute = new JSONObject();
                    attribute.put("displayName", displayName);
                    attribute.put("name", columnName);
                    attribute.put("value", value);

                    attributesArray.put(attribute);
                }
                alternatesPartsData.put("id: " + alternateId, attributesArray);
            }
        }

        return alternatesPartsData;
    }

    private JSONObject fetchAlternateRelationshipData(Connection conn, String tableName, String topartid, Map<String, String> columnMap) throws SQLException {
        JSONObject relationshipData = new JSONObject();
        String sql = "SELECT * FROM " + tableName + " WHERE topartid = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, topartid);
            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String relationshipId = resultSet.getString("relationshipId");

                JSONArray attributesArray = new JSONArray();
                for (Entry<String, String> entry : columnMap.entrySet()) {
                    String columnName = entry.getKey();
                    String displayName = entry.getValue();
                    String value = resultSet.getString(columnName);

                    JSONObject attribute = new JSONObject();
                    attribute.put("displayName", displayName);
                    attribute.put("name", columnName);
                    attribute.put("value", value);

                    attributesArray.put(attribute);
                }
                relationshipData.put("relationship id: " + relationshipId, attributesArray);
            }
        }

        return relationshipData;
    }
}
