package amberalert;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;

import org.json.*;


public class amberfetch {
	private static final int FIRST_NAME = 1;
	private static final int MIDDLE_NAME = 2;
	private static final int LAST_NAME = 3;
	private static final int AGE = 4;
	private static final int SEX = 5;
	private static final int RACE = 6;
	private static final int AGENOW = 7;
	private static final int IMAGE = 8;
	private static final int BIRTHDATE = 9;
	private static final int CASE_NUMBER = 10;
	private static final int CASE_TYPE = 11;
	private static final int CIRCUMSTANCE = 12;
	private static final int EYE_COLOR = 13;
	private static final int HAIR_COLOR = 14;
	private static final int HEIGHT = 15;
	private static final int WEIGHT = 16;
	private static final int MISSING_CITY = 17;
	private static final int MISSING_COUNTRY = 18;
	private static final int MISSING_COUNTY = 19;
	private static final int MISSING_PROVINCE = 20;
	private static final int MISSING_STATE = 21;
	private static final int MISSING_DATE = 22;
	private static final int ORG_CONTACT_INFO = 23;
	private static final int ORG_LOGO = 24;
	private static final int ORG_NAME = 25;
	private static final int ORG_PREFIX = 26;
	private static final int LAST_UPDATED = 27;
	static MysqlDataSource dataSource = new MysqlDataSource();
	static Connection mysql_conn;
	
	public static void main(String[] args)
	{
		CookieHandler.setDefault( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
		dataSource.setUser("galblank");
		dataSource.setPassword("aB290377Ba");
		dataSource.setServerName("ec2rds.csqar5t9wpws.us-east-1.rds.amazonaws.com");
		dataSource.setDatabaseName("amberalert");
		int currentPage = 1;
		
		String response = excutePost("http://www.missingkids.com/missingkids/servlet/JSONDataServlet?action=publicSearch&search=new&searchLang=en_US&subjToSearch=child&caseType=All&sex=All&goToPage="+currentPage,"");
		try {
			mysql_conn = dataSource.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject obj = new JSONObject(response);
		
		int totalPages = obj.getInt("totalPages");
		
		while(currentPage < totalPages){
			if(obj.getJSONArray("persons") != null){
				JSONArray arrayOfPeople = obj.getJSONArray("persons");
				for(int i=0;i<arrayOfPeople.length();i++){
					JSONObject person = arrayOfPeople.getJSONObject(i);
					String caseNumber = person.getString("caseNumber");
					String orgPrefix = person.getString("orgPrefix");
					System.out.println(person.toString());
					response = excutePost("http://www.missingkids.com/missingkids/servlet/JSONDataServlet?action=childDetail&orgPrefix="+orgPrefix+"&caseNum="+caseNumber+"&seqNum=1","");
					JSONObject detailedperson = new JSONObject(response);
					if(detailedperson.has("childBean")){
						JSONObject childBean = detailedperson.getJSONObject("childBean");
						addPersonToDB(childBean);
					}
				}
			}
			currentPage++;
			response = excutePost("http://www.missingkids.com/missingkids/servlet/JSONDataServlet?action=publicSearch&searchLang=en_US&subjToSearch=child&caseType=All&sex=All&goToPage="+currentPage,"");
			obj = new JSONObject(response);
		}
		
		
		response = excutePost("http://www.missingkids.com/missingkids/servlet/JSONDataServlet?action=amberAlert","");
		//{"status":"success","type":"amberAlert","alertCount":0,"persons":[]}
	
		if(response != null){
			JSONObject amberObject = new JSONObject(response);
			int numberofAlerts = amberObject.getInt("alertCount");
			JSONArray alerts = amberObject.getJSONArray("persons");
			String [] alertsarray = new String[numberofAlerts];
			for(int i = 0;i<numberofAlerts;i++){
				JSONObject oneAlert = alerts.getJSONObject(i);
				String firstName = oneAlert.getString("firstName");
				String lastName = oneAlert.getString("lastName");
				String missingCity = oneAlert.getString("missingCity");
				String sendAlert = "Amber alert! " + firstName + " " + lastName + " is missing from " + missingCity;
				alertsarray[i]=sendAlert;
			}
			
			String selectquery = "select * from users";
			ApnsService service = APNS.newService().withCert("/home/ec2-user/pushamber.p12", "123456").withSandboxDestination().build();	
			
			try {
				Statement stmt = mysql_conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(selectquery);
				while(resultSet.next()){
					String token = resultSet.getString("apnskey");
					if(token == null || token.length() < 15){
						continue;
					}
					for(int j=0;j<alertsarray.length;j++){
						String alert = alertsarray[j];
						String payload = APNS.newPayload().badge(1).alertBody(alert).sound("ambersound").build();
						service.push(token, payload);
					}
					
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*
			Thread thread = new Thread(){
			    public void run(){
			      System.out.println("Thread Running");
			    }
			  };

			  thread.start();*/
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
		String middleName = "";
		if(person.has("middleName")){
			middleName = person.getString("middleName");
		}
		String lastName = person.getString("lastName");
		int age = person.getInt("age");
		String sex = person.has("sex")?person.getString("sex"):"";
		String ageNow = "";
		if(person.has("approxAge") && person.getString("approxAge").length() > 0){
			ageNow = person.getString("approxAge");
		}
		long birtdaySince1970 = 0;
		if(person.has("birthDay")){
			String birthDay = person.has("birthDay")?person.getString("birthDay"):"";
			DateFormat format = new SimpleDateFormat("MMMM d, yyyy hh:mm:ss aaa", Locale.ENGLISH);
			java.util.Date date;
			try {
				date = format.parse(birthDay);
				birtdaySince1970 = date.getTime();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
		String caseNumber = person.getString("caseNumber");
		String circumstance = "";
		if(person.has("circumstance")){
			circumstance = person.has("circumstance")?person.getString("circumstance"):"";
		}
		
		String eyeColor = "";
		if(person.has("eyeColor")){
			eyeColor = person.getString("eyeColor");
		}
		
		String hairColor = "";
		if(person.has("hairColor")){
			hairColor = person.getString("hairColor");
		}
		
		int height = 0;
		if(person.has("height")){
			height = person.getInt("height");;
		}
		
		int weight = 0;
		if(person.has("weight")){
			weight = person.getInt("weight");
		}
		

		String missingCity = person.getString("missingCity");
		String missingCounty = person.getString("missingCounty");
		String missingState = person.getString("missingState");
		String missingCountry = "";
		if(person.has("missingCountry")){
			missingCountry = person.getString("missingCountry");
		}
		
		String missingProvince = "";
		if(person.has("missingProvince")){
			missingProvince = person.getString("missingProvince");
		}
		
		
		long missingDateSince1970 = 0;
		if(person.has("missingDate")){
			String missingDate = person.getString("missingDate");
			String birthDay = person.has("missingDate")?person.getString("missingDate"):"";
			DateFormat format = new SimpleDateFormat("MMMM d, yyyy hh:mm:ss aaa", Locale.ENGLISH);
			java.util.Date date;
			try {
				date = format.parse(birthDay);
				missingDateSince1970 = date.getTime();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		String orgName = "";
		if(person.has("orgName")){
			orgName = person.getString("orgName");
		}
		
		String orgContactInfo = "";
		if(person.has("orgContactInfo")){
			orgContactInfo = person.getString("orgContactInfo");
		}
		
		String thumbnailUrl = "";
		if(person.has("thumbnailUrl")){
			thumbnailUrl = person.getString("thumbnailUrl");
		}

		if(person.has("photoMap")){
			thumbnailUrl =  person.getString("photoMap");
		}
		

		String orgLogo = "";
		if(person.has("orgLogo")){
			orgLogo = person.getString("orgLogo");
		}
		
		String orgPrefix = "";
		if(person.has("orgPrefix")){
			orgPrefix = person.getString("orgPrefix");
		}
		
		String race = "";
		if(person.has("race")){
			race = person.getString("race");
		}
		
		String caseType = "";
		if(person.has("caseType")){
			caseType = person.getString("caseType");
		}
		
		long todayInMilli = new Date().getTime();

		try {
			java.sql.PreparedStatement pstmt = null;
			Statement stmt = mysql_conn.createStatement();
			String seelctquery = "select * from person where caseNumber = '" + caseNumber + "'";
			ResultSet resultSet = stmt.executeQuery(seelctquery);
			if (!resultSet.next()) {    
				//this case is not found meaning this is new person - add it to the db
				String query = "INSERT INTO person(firstName,"
						+ "middleName,"
						+ "lastName,"
						+ "age,"
						+ "sex,"
						+ "race,"
						+ "ageNow,"
						+ "image,"
						+ "birthDate,"
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
						+ "orgName,"
						+ "orgPrefix,"
						+ "lastupdate) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				
				pstmt = mysql_conn.prepareStatement(query);  
				pstmt.setString(FIRST_NAME,firstName);
				pstmt.setString(MIDDLE_NAME,middleName);
				pstmt.setString(LAST_NAME,lastName);
				pstmt.setInt(AGE,age);
				pstmt.setString(SEX,sex);
				pstmt.setString(RACE,race);
				pstmt.setString(AGENOW,ageNow);
				pstmt.setString(IMAGE,thumbnailUrl);
				pstmt.setLong(BIRTHDATE, birtdaySince1970);
				pstmt.setString(CASE_NUMBER,caseNumber);
				pstmt.setString(CASE_TYPE,caseType);
				pstmt.setString(CIRCUMSTANCE,circumstance);
				pstmt.setString(EYE_COLOR,eyeColor);
				pstmt.setString(HAIR_COLOR,hairColor);
				pstmt.setInt(HEIGHT,height);
				pstmt.setInt(WEIGHT,weight);
				pstmt.setString(MISSING_CITY,missingCity);
				pstmt.setString(MISSING_COUNTRY,missingCountry);
				pstmt.setString(MISSING_COUNTY,missingCounty);
				pstmt.setString(MISSING_PROVINCE,missingProvince);
				pstmt.setString(MISSING_STATE,missingState);
				pstmt.setLong(MISSING_DATE,missingDateSince1970);
				pstmt.setString(ORG_CONTACT_INFO,orgContactInfo);
				pstmt.setString(ORG_LOGO,orgLogo);
				pstmt.setString(ORG_NAME,orgName);
				pstmt.setString(ORG_PREFIX,orgPrefix);
				pstmt.setDouble(LAST_UPDATED,todayInMilli);
				pstmt.executeUpdate();
			}
			else{
				//person exists in the DB
				System.out.println("person with case number " + caseNumber + "exists in DB");
			}
			if(resultSet != null){
				resultSet.close();
			}
			
			if(pstmt != null){
				pstmt.close();
			}
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static String excutePost(String targetURL, String urlParameters) {
		System.out.println(targetURL);
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
