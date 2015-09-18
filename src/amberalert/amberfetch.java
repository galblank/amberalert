package amberalert;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.json.*;

public class amberfetch {

	static MysqlDataSource dataSource = new MysqlDataSource();
	static Connection mysql_conn;
	
	public static void main(String[] args)
	{
		dataSource.setUser("galblank");
		dataSource.setPassword("aB290377Ba");
		dataSource.setServerName("ec2rds.csqar5t9wpws.us-east-1.rds.amazonaws.com");
		dataSource.setDatabaseName("amberalert");
		String response = excutePost("http://www.missingkids.com/missingkids/servlet/JSONDataServlet?action=publicSearch&searchLang=en_US&search=new&subjToSearch=child&missState=CA&missCountry=US&caseType=All&sex=All","");
		try {
			mysql_conn = dataSource.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parseResponse(response);
	}
	
	public static void parseResponse(String data){
		JSONObject obj = new JSONObject(data);
		JSONArray arrayOfPeople = obj.getJSONArray("persons");
		
		for(int i=0;i<arrayOfPeople.length();i++){
			JSONObject person = arrayOfPeople.getJSONObject(i);
			System.out.println(person.toString());
			addPersonToDB(person);
		}
		 
		try {
			Connection conn = dataSource.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
/*	{"lastName":"ABDEL-WAHED",
	"missingCity":"PASADENA",
	"approxAge":"",
	"orgName":"National Center for Missing & Exploited Children",
	"hasThumbnail":true,
	"race":"",
	"hasPoster":true,
	"missingDate":"May 3, 2015 12:00:00 AM",
	"missingState":"CA",
	"missingCounty":"Los Angeles",
	"inDay":false,
	"inMonth":false,
	"caseType":"",
	"posterTitle":""
	,"firstName":"RWAND"
	,"caseNumber":"1251416"
	,"missingCountry":"US"
	,"seqNumber":1,
	"middleName":"",
	"orgPrefix":"NCMC",
	"langId":"en_US",
	"isChild":true,
	"age":17,
	"thumbnailUrl":"/photographs/NCMC1251416c1t.jpg"}
	*/

	public static void addPersonToDB(JSONObject person)
	{
		String firstName = person.getString("firstName");
		String middleName = person.getString("middleName");
		String lastName = person.getString("lastName");
		int age = person.getInt("age");
		String sex = person.getString("sex");
		String ageNow = person.getString("approxAge");
		String birthDay = person.getString("birthDay");
		
		DateFormat format = new SimpleDateFormat("MMMM d, yyyy hh:mm:ss aaa", Locale.ENGLISH);
		Date date = format.parse(birthDay);
		long birtdaySince1970 = date.getTime();
		
		
		int caseNumber = person.getInt("caseNumber");
		String circumstance = person.getString("circumstance");
		String eyeColor = person.getString("eyeColor");
		String hairColor = person.getString("hairColor");
		int height = person.getInt("height");
		int weight = person.getInt("weight");
		String missingCity = person.getString("missingCity");
		String missingCounty = person.getString("missingCounty");
		String missingState = person.getString("missingState");
		String missingCountry = person.getString("missingCountry");
		String missingProvince = person.getString("missingProvince");
		String missingDate = person.getString("missingDate");
		String orgName = person.getString("orgName");
		String orgContactInfo = person.getString("orgContactInfo");
		String thumbnailUrl = person.getString("thumbnailUrl");
		String orgLogo = person.getString("orgLogo");
		String race = person.getString("race");
		String caseType = person.getString("caseType");

		try {
			java.sql.PreparedStatement pstmt;
			Statement stmt = mysql_conn.createStatement();
			ResultSet resultSet = stmt.executeQuery("select * from person where caseNumber = " + caseNumber);
			if (!resultSet.isBeforeFirst() ) {    
				//this case is not found meaning this is new person - add it to the db
				String query = "INSERT INTO person(id,"
						+ "firstName,"
						+ "middleName,"
						+ "lastName,"
						+ "age,"
						+ "sex,"
						+ "race,"
						+ "ageNow,"
						+ "thumbnailUrl,"
						+ "birtdaySince1970,"
						+ "caseNumber,"
						+ "caseType,"
						+ "circumstance,"
						+ "eyeColor,"
						+ "hairColor,"
						+ "height,"
						+ "weight,"
						+ "missingCity,"
						+ "missingCountry,"
						+ "missingCounty,"
						+ "missingProvince,"
						+ "missingState,"
						+ "missingDate,"
						+ "orgContactInfo,"
						+ "orgLogo,"
						+ "orgName) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt.setInt(1, null); // set input parameter 1
			    pstmt.setString(2, "deptname"); // set input parameter 2
			    pstmt.setString(3, "deptLocation"); // se
			      
				pstmt = mysql_conn.prepareStatement(query);
				pstmt.executeUpdate();
			}
			else{
				//person exists in the DB
				pstmt = mysql_conn.prepareStatement("UPDATE COFFEES " +
                        "SET PRICE = ? " +
                        "WHERE COF_NAME = ?");
			}
			resultSet.close();
			
			pstmt.close();
			mysql_conn.close();
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String excutePost(String targetURL, String urlParameters) {
		  HttpURLConnection connection = null;  
		  try {
		    //Create connection
		    URL url = new URL(targetURL);
		    connection = (HttpURLConnection)url.openConnection();
		    connection.setRequestMethod("POST");
		    connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		    connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
		    connection.setRequestProperty("Content-Language", "en-US");  

		    connection.setUseCaches(false);
		    connection.setDoOutput(true);

		    //Send request
		    DataOutputStream wr = new DataOutputStream (
		        connection.getOutputStream());
		    wr.writeBytes(urlParameters);
		    wr.close();

		    //Get Response  
		    InputStream is = connection.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+ 
		    String line;
		    while((line = rd.readLine()) != null) {
		      response.append(line);
		      response.append('\r');
		    }
		    rd.close();
		    return response.toString();
		  } catch (Exception e) {
		    e.printStackTrace();
		    return null;
		  } finally {
		    if(connection != null) {
		      connection.disconnect(); 
		    }
		  }
		}
	
}
