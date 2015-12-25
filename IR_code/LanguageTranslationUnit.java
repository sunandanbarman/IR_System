import java.net.URLEncoder;
import java.net.URL;
/*Microsoft translation API wrapper class*/
import com.memetix.mst.detect.Detect;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
/* For jSON parsing*/
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import java.util.List;

class LanguageTranslationUnit {
	private String translateClientID = "IRSystemForTweetsAndOtherData";
	private String translateClientSecret = "tMQW4iyOewudR9//jD3mxFefr+PU6O2CfuvlTRDGfAI=";
    private boolean bIsTranslationAuth 	 = false; // if false, translation CANNOT be done
	private final String LANG_DETECT_API_KEY = "711d7412f6f412fa86b040eee064b988";
	private String detectLanguageURL 		 = "http://ws.detectlanguage.com/0.2/detect";
	private String detectedLang = "";
	private boolean bIsLangDetectedReliable  = false;
	private double nLangDetectionConfidence  = 0.0;
	LanguageTranslationUnit() {
		bIsTranslationAuth = DoAuthentication();
	}	
	/**
	*@param none
	*@return bIsTranslationAuth
	*/
	public boolean getIsTranslationAuth() {
		return this.bIsTranslationAuth;	
	}
	/**
	*@param none
	*@return detectedLang
	*/
	public String getDetectedLang() {
		return this.detectedLang;	
	}
	/**
	*@param lang
	*@return none
	*/
	public void setDetectedLang(String lang) {
		this.detectedLang = lang;	
	}
	/**
	*@param none
	*@return bIsLangDetectedReliable
	*/
	public boolean getIsLangDetectionReliable() {
		return this.bIsLangDetectedReliable;	
	}
	/**
	*@param none
	*@return bIsTranslationAuth
	*/
	public double getLangDetectionConfidence() {
		return this.nLangDetectionConfidence;	
	}	
	/**
	*
	*/
	public String doTranslation(String text,String fromLang,String toLang)  {
	    String response = text;
	    try {
    	    response = new String(Translate.execute(text,Language.fromString(fromLang), Language.fromString(toLang)).getBytes("UTF-8"),
    	                                java.nio.charset.Charset.forName("UTF-8"));
	    }	catch(Exception ex) {
	        HelperFunctions.writeInformationIntoFile("Translation exception . Reason :" + ex.getMessage() + " " );
	        HelperFunctions.writeInformationIntoFile("Original string returned as translationn !! ");
	    }
		return response;
	}
	/**
	* Do the authentication of the system for using Microsoft translation API
	*/
	public boolean DoAuthentication() {
		boolean bResult = true;
		Translate.setClientId(translateClientID);
	    Translate.setClientSecret(translateClientSecret);
        try {
        	doTranslation("hello","en","fr");
        }
        catch(Exception e) { // if translation fails, then authentication has failed
        	bResult = false;
        	HelperFunctions.writeInformationIntoFile("Language translation authentication failed ! Reason :" + e.getMessage());
        }
        return bResult;
	}
	
	/**
	* @param :QueryText 
	* The param is tested by firing a HTTP post request to http://ws.detectlanguage.com/0.2/detect
	* The output result is in JSON Format which is parsed to extract language field
	* The isReliableField of the json field is used to decide whether to boost the tweets which are 
	* in the original language or not
	*/
	public boolean getLanguageOfQuery(String queryText) {
		boolean bResponse =false;
		String response;
		try {
			String query = String.format("q=%s&key=%s", 
							URLEncoder.encode(queryText, HelperFunctions.charset), 
							URLEncoder.encode(LANG_DETECT_API_KEY, HelperFunctions.charset));

			response = HelperFunctions.fetchHTTPData(detectLanguageURL, query);	
			 if (!response.equals("")) {
				parseJSONStringAndFindLanguage(response);
				bResponse = true; 
			}
			 else
				HelperFunctions.writeInformationIntoFile("No response from server for language detection.");
		} 
		catch(Exception ex) {
			HelperFunctions.writeInformationIntoFile("Exception occured in getLanguageOfQuery :" + ex.getMessage());
		}
		return bResponse;
	}	
	/**
	 * 
	 * @param jSONString
	 * @return
	 * @throws JSONException
	 */
	public void parseJSONStringAndFindLanguage(String jSONString) throws JSONException {
		JSONObject jObj;
		jObj = new JSONObject(jSONString);
		try {
			detectedLang = jObj.getJSONObject("data").getJSONArray("detections").getJSONObject(0).get("language").toString();
	
			bIsLangDetectedReliable = Boolean.valueOf(jObj.getJSONObject("data").getJSONArray("detections").getJSONObject(0).get("isReliable").toString());
			
			nLangDetectionConfidence = Double.valueOf(jObj.getJSONObject("data").getJSONArray("detections").getJSONObject(0).get("confidence").toString());
		}
		catch(Exception ex) {
			HelperFunctions.writeInformationIntoFile("Exception in parseJSONStringAndFindLanguage :" + ex.getMessage());
		}	
	}
	
	/**
	 * 
	 * @param responseFromSolr
	 * @return
	 * @throws JSONException 
	 * @throws NumberFormatException 
	*/
	public String parseJSONResponseFromSolr(String responseFromSolr,String sortOrder) throws NumberFormatException {
		String numFound = "";
		if (responseFromSolr.equals(""))
			return numFound;
		try {
			JSONObject jObj = new JSONObject(responseFromSolr).getJSONObject("response");
			numFound = jObj.get("numFound").toString();
			if ( (sortOrder.equalsIgnoreCase("ASC")) && (HelperFunctions.corpusCreatedDate.equals(""))) {
			    try {
			        HelperFunctions.corpusCreatedDate = jObj.getJSONArray("docs").getJSONObject(0).getString("created_at");
			    } catch(Exception ex) {
			        HelperFunctions.writeInformationIntoFile("Fetching corpusCreatedDate failed .Reason:" + ex.getMessage());
			    }
			}
			if ( (sortOrder.equalsIgnoreCase("DESC")) && (HelperFunctions.corpusEndDate.equals(""))) {
			    try {
			        HelperFunctions.corpusEndDate = jObj.getJSONArray("docs").getJSONObject(0).getString("created_at");
			    } catch(Exception ex) {
			        HelperFunctions.writeInformationIntoFile("Fetching corupsEndDate failed .Reason:" + ex.getMessage());
			    }
			}
		}
		catch(JSONException ex) {
			HelperFunctions.writeInformationIntoFile("Exception occur in parseJSONResponseFromSolr:" + ex.getMessage());
		}
		return numFound;
	}
	/**
	 * @param list    : list to be added to the existing JSON String in [] Format
	 * @param response: the JSON string to append the list to
	 * @param keyName : keyname for the list
	 * */
	public String addArrayToJSON(List<String> list,String response,String keyName) {
		String resp="";
	    try {
    		JSONObject jResp= new JSONObject(response);
    		jResp.put(keyName, list);
    		resp= jResp.toString();
	    }
	    catch(Exception ex) {
	        HelperFunctions.writeInformationIntoFile("Exception occured in addArrayToJSON . Reason:" + ex.getMessage());
	    }
	    return resp;
	}	

	
}