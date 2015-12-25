/*For HTTP connections*/
import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.DataInputStream;
/* For helper functions */
import java.util.List;
import java.util.Collections;
/*For logging we need date-time information*/
import java.text.SimpleDateFormat;
import java.util.Date;
/******************************************************
Helper functions
*******************************************************/

class HelperFunctions {
	public static String charset = java.nio.charset.StandardCharsets.UTF_8.name();
	private static String USER_AGENT  = "Mozilla/5.0";
	public static PrintWriter pWriter;
	public static StringBuilder sBuilder;
	public static String workingDirectory;
	public static String twitterCoreURL;
	public static String nytCoreURL = "";
	public static String guardianCoreURL = "";
	public static String corpusCreatedDate ="", corpusEndDate ="";
	/**
	 * 
	 */
	public static String findMaxOccurenceInList(List<String> list) {
		int curr = 0, max = 0 ;
		String maxKey = "";
		if (list.size() == 0) {
			return maxKey;
		}	
		for(String key:list) {
			curr = Collections.frequency(list,key);
			if(max < curr) {
				max = curr;
				maxKey = key;	
			}	
		}	
		return maxKey;
	}	
	/**
	  * See: {@link org.apache.lucene.queryparser.classic queryparser syntax} 
	  * for more information on Escaping Special Characters
	*/
	public static String escapeQueryChars(String s) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
	  // These characters are part of the query syntax and must be escaped
	  if (c == '\\' || c == '+' || c == '-' || c == '!'  || c == '(' || c == ')' || c == ':'
	    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
	    || c == '*' || c == '?' || c == '|' || c == '&'  || c == ';' || c == '/'
	    || Character.isWhitespace(c)) {
	    sb.append('\\');
	      }
	      sb.append(c);
	    }
	    return sb.toString();
	}
	public static boolean isContainsSpecialCharacters(String s) {
	    String query = s;
	    boolean bResult = false;
	    String text = HelperFunctions.escapeQueryChars(query);
	    if ( !(text.equalsIgnoreCase(s))) {
	        bResult = true;
	    }    
	    return bResult;
	}
	/**
	 * 
	 * @param URL
	 * @param query
	 * @return
	 * @throws IOException 
	 */
	public static String fetchHTTPData(String URL, String query)  {
		String response = "";
		int responseCode = 0;
		try {
				HttpURLConnection httpConn = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
				httpConn.setDoOutput(true); // Triggers POST.
				
				httpConn.setRequestProperty("Accept-Charset", charset);
				httpConn.setRequestProperty("User-Agent", USER_AGENT);
				responseCode = httpConn.getResponseCode();
				if ( responseCode == 200) { //OK
					BufferedReader in = new BufferedReader(
							new InputStreamReader(httpConn.getInputStream(),"UTF-8"));
						String inputLine;
						StringBuffer responseBuffer = new StringBuffer();

						while ((inputLine = in.readLine()) != null) {
							responseBuffer.append(inputLine);
						}
						in.close();
						response = responseBuffer.toString();
				}
		}
		catch(Exception ex) {
			HelperFunctions.writeInformationIntoFile("Exception while fetching HTTP from URL:" + URL + "?" + query + "\r\nError stackTrace :" + ex.getMessage());		
		}		
		return response;
	}
    /**
     *@param : inputQuery 
     * Check if the given term is a hashtag or not
    */
	public static boolean isTermHashTag(String inputQuery) {
		boolean bIsHashTag = false;
		String[] sTermSplit = inputQuery.split(" ");
		for ( String sTemp : sTermSplit ) {
			if ( sTemp.charAt(0) == '#') {
				bIsHashTag = true;
				break;
			}
		}
		return bIsHashTag;
	}
	/**
	*@param : URLtoTest ( check if given URL can be connected to or not)
	*/
	public static int testURL(String URLtoTest) {
	    String strUrl = URLtoTest;
	    int nResult = 404;
	    try {
	        URL url = new URL(strUrl);
	        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
	        urlConn.connect();
	        nResult = urlConn.getResponseCode();
	    } catch (IOException e) {
	        HelperFunctions.writeInformationIntoFile("Error in testURL. Connecting to URL " + strUrl + " Exception :" + e.getMessage());
	    }
		return nResult;
	}	
	/**
	 * Write the information into the log file
	 */
	public static void writeInformationIntoFile(String text) {
	    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
		try {
			String fileName= HelperFunctions.workingDirectory + "//log.txt";
            Writer out = new OutputStreamWriter(new FileOutputStream(fileName, true),"UTF-8");
            try {
                out.write("\r\n" + "[ " + simpleDateFormat.format(new Date()).toString() + " ] : " + text);
            } finally {
                out.close();
            }			

		} catch(Exception ex) {
		}	
	}
}