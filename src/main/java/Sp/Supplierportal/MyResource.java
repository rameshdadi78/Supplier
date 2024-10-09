package Sp.Supplierportal;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.postgresql.Driver;

import java.sql.*;


import org.json.JSONArray;
import org.json.JSONObject;
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")



	public class MyResource {

	    @GET
	    @Path("film")
	    @Produces(MediaType.APPLICATION_JSON)
	    public String getFilmData() throws SQLException, ClassNotFoundException {
	        String url = "jdbc:postgresql://localhost:5432/dvdrental";
	        String user = "postgres";
	        String password = "Manoj123";
	        String sql = "SELECT * FROM film";
	        
	        JSONArray jsonArray = new JSONArray();
	        try {
	        	
	        	Class.forName("org.postgresql.Driver");//instructs the Java Virtual Machine (JVM) to load and register the PostgreSQL JDBC driver class 
	        	//for establishing a connection to a PostgreSQL database.	
	        
	        	Connection conn = DriverManager.getConnection(url, user, password);
	             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
	             ResultSet result = stmt.executeQuery(sql);//Then given SQL statement, which returns a single ResultSet object. 
	            while (result.next()) {
	                JSONObject jsonObject = new JSONObject(); // Create new JSONObject for each row
	                String filmTitle = result.getString("title");
	                int filmId = result.getInt("film_id");
	                jsonObject.put("title", filmTitle);
	                jsonObject.put("film_id", filmId);
	                jsonArray.put(jsonObject);
	            }
//	            System.out.println(jsonArray.toString(1));
	        
	        } catch (Exception e) {
	            e.printStackTrace();
	            return "{\"error\": \"Database error occurred\"}";
	        }
	        
	        return jsonArray.toString();
	    }
	    
	    @GET
	    @Path("customer")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getCustomerDetails() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT * FROM Customer";
		        JSONArray jsonArray1 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		        
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject = new JSONObject(); // Create new JSONObject for each row
		                String firstname = result.getString("first_name");
		                String lastname = result.getString("last_name");
		                String emailAddress = result.getString("email");
		                int customerId = result.getInt("customer_id");
		                jObject.put("Email", emailAddress);
		                jObject.put("name", firstname + " " +lastname);
		                jObject.put("customer_id", customerId);
		                jsonArray1.put(jObject);
		            }
//		            System.out.println(jsonArray1.toString(1));
		        
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray1.toString();
	    }
	    @GET
	    @Path("activeCustomer")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getActiveCustomerDetails() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT * FROM Customer WHERE activebool= true";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		        
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String firstname = result.getString("first_name");
		                String lastname = result.getString("last_name");
		                boolean active = result.getBoolean("activebool");
		                int customerId = result.getInt("customer_id");
		                if (active) {
		                jObject1.put("active", "Active");
		                }
		                jObject1.put("name", firstname + " " +lastname);
		                jObject1.put("customer_id", customerId);
		                jsonArray2.put(jObject1);
		            }
		        
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray2.toString();
	    }
	    @GET
	    @Path("Language")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getFilmLanguages() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT DISTINCT language.name,language.language_id FROM language LEFT JOIN film ON language.language_id = film.language_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		      
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String name = result.getString("name");
		                int customerId = result.getInt("language_id");
		                jObject1.put("Language_name", name.trim());
		                jObject1.put("Language_id", customerId);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray2.toString();
			
	    }
	    //SELECT inventory.store_id,address_id FROM customer LEFT JOIN inventory ON customer.store_id = inventory.store_id
	    @GET
	    @Path("InventoryId")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getInventoryId() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT inventory.store_id,address_id FROM customer INNER JOIN inventory ON customer.customer_id = inventory.inventory_id ";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		      
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                int storeid = result.getInt("store_id");
		                int addressid = result.getInt("address_id");
		                jObject1.put("StoreId", storeid);
		                jObject1.put("AddressId", addressid);
		                jsonArray2.put(jObject1);
		            }
		        
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray2.toString();
			
	    }
	    //SELECT title, rental_duration FROM film ORDER BY rental_duration DESC LIMIT 5;
	    @GET
	    @Path("LongestRentalFilm")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getLongestRentalFilm() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT title, rental_duration FROM film ORDER BY rental_duration DESC LIMIT 5";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		        
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String filmtilte = result.getString("title");
		                int Duration = result.getInt("rental_duration");
		                jObject1.put("FilmTitle", filmtilte);
		                jObject1.put("Duration", Duration);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("NonRentalFilm")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getNonRentalFilm() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT film.film_id,film.title FROM film LEFT JOIN inventory ON film.film_id = inventory.film_id\r\n"
		        		+ "LEFT JOIN rental ON inventory.inventory_id = rental.inventory_id\r\n"
		        		+ "WHERE rental.rental_id IS NULL ORDER BY film_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String filmtilte = result.getString("title");
		                int filmid = result.getInt("film_id");
		                jObject1.put("FilmTitle", filmtilte);
		                jObject1.put("FilmID", filmid);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	
	    @GET
	    @Path("CountryName")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getCountryName() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT country.country, country.country_id\r\n"
		        		+ "FROM city JOIN country ON city.country_id = country.country_id\r\n"
		        		+ "WHERE city.city = 'Lethbridge'";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String country = result.getString("country");
		                int countryid = result.getInt("country_id");
		                jObject1.put("country", country);
		                jObject1.put("Country_id", countryid);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		           
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("TotalamountofCustomerSpent")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getTotalamountofCustomer() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT customer.customer_id, customer.first_name, customer.last_name, SUM(payment.amount) AS total_amount_spent\r\n"
		        		+ "FROM customer\r\n"
		        		+ "JOIN payment ON customer.customer_id = payment.customer_id\r\n"
		        		+ "GROUP BY customer.customer_id, customer.first_name, customer.last_name\r\n"
		        		+ "ORDER BY customer_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String FisrtName = result.getString("first_name");
		                String LastName = result.getString("last_name");
		                int Customer_id = result.getInt("customer_id");
		                int totalamount = result.getInt("total_amount_spent");
		                jObject1.put("CustomerName", FisrtName+ " " +LastName);
		                jObject1.put("CustomerId", Customer_id);
		                jObject1.put("Total Amount Spent", totalamount);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		           
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("TotalPaymentsDonebyCutomer")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getPaymentsDonebyCutomer() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT customer.customer_id, customer.first_name, customer.last_name, COUNT(payment.payment_id) AS total_payments\r\n"
		        		+ "FROM customer\r\n"
		        		+ "JOIN payment ON customer.customer_id = payment.customer_id\r\n"
		        		+ "GROUP BY customer.customer_id, customer.first_name, customer.last_name\r\n"
		        		+ "ORDER BY customer_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String FisrtName = result.getString("first_name");
		                String LastName = result.getString("last_name");
		                int Customer_id = result.getInt("customer_id");
		                int totalpayments = result.getInt("total_payments");
		                jObject1.put("CustomerName", FisrtName+ " " +LastName);
		                jObject1.put("CustomerId", Customer_id);
		                jObject1.put("Total Payments Done", totalpayments);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("FilmsinEachategory")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getFilmsinEachategory() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT category.name,category.category_id, COUNT(film.film_id) AS total_films FROM category\r\n"
		        		+ "JOIN film_category ON category.category_id = film_category.category_id\r\n"
		        		+ "JOIN film  ON film_category.film_id = film.film_id\r\n"
		        		+ "GROUP BY category.category_id,category.name\r\n"
		        		+ "ORDER BY category_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String name = result.getString("name");
		                int category_id = result.getInt("category_id");
		                int total_films = result.getInt("total_films");
		                jObject1.put("CategoryName", name);
		                jObject1.put("CategoryId", category_id);
		                jObject1.put("Total Films", total_films);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("StoreAddress")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getStoreAddress() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT store.store_id, address.address, address.district, city.city, country.country\r\n"
		        		+ "FROM store\r\n"
		        		+ "JOIN address ON store.address_id = address.address_id\r\n"
		        		+ "JOIN city ON address.city_id = city.city_id\r\n"
		        		+ "JOIN country ON city.country_id = country.country_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String Address = result.getString("address");
		                String District = result.getString("district");
		                String city = result.getString("city");
		                String country = result.getString("country");
		                int store_id = result.getInt("store_id");		                
		                jObject1.put("Address", Address);
		                jObject1.put("District", District);
		                jObject1.put("City", city);
		                jObject1.put("Country", country);
		                jObject1.put("StoreId", store_id);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		            return "{\"error\": \"Database error occurred\"}";
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("MostRentedFilm")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getMostRentedFilm() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT film.title,film.film_id, COUNT(rental.rental_id) AS rental_count FROM film \r\n"
		        		+ "JOIN inventory  ON film.film_id = inventory.film_id\r\n"
		        		+ "JOIN rental ON inventory.inventory_id = rental.inventory_id\r\n"
		        		+ "GROUP BY film.film_id, film.title ORDER BY rental_count DESC LIMIT 3";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String filmTitle = result.getString("title");
		                int rentalCount = result.getInt("rental_count");
		                int film_id = result.getInt("film_id");
		                jObject1.put("filmTitle", filmTitle);
		                jObject1.put("Rental_count", rentalCount);
		                jObject1.put("Film_id", film_id);
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	     
	    @GET
	    @Path("FilmsCustomerRented")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getFilmsCustomerRented() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT c.customer_id, c.first_name, c.last_name, f.title\r\n"
		        		+ "FROM customer c\r\n"
		        		+ "JOIN rental r ON c.customer_id = r.customer_id\r\n"
		        		+ "JOIN inventory i ON r.inventory_id = i.inventory_id\r\n"
		        		+ "JOIN film f ON i.film_id = f.film_id";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String title = result.getString("title");
		                String first_name = result.getString("first_name");
		                String last_name = result.getString("last_name");
		                int customer_id = result.getInt("customer_id");
		                jObject1.put("Username", first_name + " "+last_name);
		                jObject1.put("filmName", title);
		                jObject1.put("CustomerID", customer_id);

		               
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	    @GET
	    @Path("FilmsinMultipleLanguage")
	    @Produces(MediaType.APPLICATION_JSON) // indiactes the method return type
	    public String getFilmsinMultipleLanguage() throws SQLException, ClassNotFoundException{
	    	 String url = "jdbc:postgresql://localhost:5432/dvdrental";
		      String user = "postgres";
		        String password = "Manoj123";
		        String sql = "SELECT film.title, COUNT(film.title) AS title_count\r\n"
		        		+ "FROM film\r\n"
		        		+ "GROUP BY film.title \r\n"
		        		+ "HAVING COUNT(film.title) > 1";
		        JSONArray jsonArray2 = new JSONArray();
		        try {
		        	
		        	Class.forName("org.postgresql.Driver");
		       
		        	Connection conn = DriverManager.getConnection(url, user, password);
		             Statement stmt = conn.createStatement();//Creates a Statement object for sendingSQL statements to the database
		             ResultSet result = stmt.executeQuery(sql);
		            while (result.next()) {
		                JSONObject jObject1 = new JSONObject(); // Create new JSONObject for each row
		                String title = result.getString("title");
		               
		                jObject1.put("filmName", title);
		 

		               
		                jsonArray2.put(jObject1);
		            }
		       
		        } catch (Exception e) {
		            e.printStackTrace();
		        }

			return jsonArray2.toString();
			
	    }
	}