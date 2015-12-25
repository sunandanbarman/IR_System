/*
*Servlet which will query the Solr instance for the data retrieval
*/
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import twitter4j.JSONObject;
import twitter4j.JSONException;

import java.util.Date;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public class QueryServlet extends HttpServlet {
	private final String USER_AGENT  = "Mozilla/5.0";
	private LanguageTranslationUnit langUnitObj;
	//private AlchemyUnit alchemyUnitObj;
	private HelperFunctions helper;
	private double originalLangBoost  = 2.0;
	private double otherLangBoost     = 1.2;
	private double hashtagBoost       = 1.0;
	private final int phraseQrySlop   = 3;  // value of phraseSlop and querySlop     
	private final int phraseLength    = 4;  // if length of query >= phraseLength, then we introduce query slop/phrase slop 
	private final double tieBreaker   = 0.1;//the tie breaker value as used in dismax parser to adjust scores from low scoring fields
	private HashMap<String,String> translationMap;
	private String[] langArray 	      = {"en","de","fr","ru","ar"};	
	private HashMap<String,Integer> maxIndexSizeData;
	private Properties prop = null;
	HashTagTrends hashTagTrends;
	String langFilter		=null;
	String sourceFilter		=null;
	String verifiedFilter	=null;
	String isFilterRequired	=null;
	
	
    /**
	* Initialization of the authentication with Microsoft translation API
	*/
	public void init(ServletConfig config) throws ServletException {
	    super.init(config);
		langUnitObj      = new LanguageTranslationUnit();
		maxIndexSizeData = new HashMap<>();
        HelperFunctions.workingDirectory = getWorkingDirectory();
        this.prop        = loadConfig();
        getPropertiesData();
        int responseCode = HelperFunctions.testURL(HelperFunctions.twitterCoreURL+"/select");
        if (responseCode == 200) { //OK
            getCorpusDateFromSolr();
            hashTagTrends    = new HashTagTrends(this.prop);
        }    
	}
   /**
    * Load all the config
    * 
    * @return
    */
   private Properties loadConfig() {
       String propFileName = HelperFunctions.workingDirectory + "//DefaultProperties.properties";
       
       Properties props = new Properties();
       try ( InputStream inputStream = new FileInputStream(propFileName)) {
    	   props.load(inputStream);
       } catch (IOException e) {
          HelperFunctions.writeInformationIntoFile("Failed to load properties file. Exception " + e.getMessage());
       }
       return props;
   }
   /**
    */
	public String getWorkingDirectory() {
		URL myURL = getClass().getProtectionDomain().getCodeSource().getLocation();
		java.net.URI myURI = null;
		try {
		    myURI = myURL.toURI();
		} catch (URISyntaxException e1) 
		{}
		return java.nio.file.Paths.get(myURI).toFile().toString()	;	
	}	
	/**
	 *      
	 * @return
	 */
    public Set<Object> getAllKeys(){
        Set<Object> keys = prop.keySet();
        return keys;
    }
     
    public String getPropertyValue(String key){
        return this.prop.getProperty(key);
    }	
	/**
	 * 
	 * @param fileName
	 */
	public void getPropertiesData()
	{
		try {
			Set<Object> keys = getAllKeys();
			int nCount = 0; 
			for(Object k:keys){
				String key = (String)k;
				if (key.equalsIgnoreCase("SolrURL")) {
					HelperFunctions.twitterCoreURL = getPropertyValue(key);
				}

				if (key.equalsIgnoreCase("GuardianURL")) {
					HelperFunctions.guardianCoreURL = getPropertyValue(key);
				}

				if (key.equalsIgnoreCase("nytURL")) {
					HelperFunctions.nytCoreURL = getPropertyValue(key);
				}
			
			}
		} catch(Exception ex) {
		}		
	}
	
	/**
	* Constructs the translation map in format lang=translation
	*/
	public void getTranslationOfQuery(String query,String detectedLang) {
		translationMap = new HashMap<>();
		String translatedText;
		try {
			for(int i= 0 ; i < langArray.length; i++) {
				translatedText = langUnitObj.doTranslation(query,detectedLang,langArray[i]);
				if  (translatedText.equals("") || (translatedText.equals(null)) ) { //translation failed
				    translationMap.put(langArray[i], URLEncoder.encode(  query,HelperFunctions.charset));
				}	
				else {
					translatedText = URLEncoder.encode(translatedText,HelperFunctions.charset);
					translationMap.put(langArray[i], translatedText);
				}
			}	
		}
		catch(Exception ex) { //log line
			HelperFunctions.writeInformationIntoFile("Exception in getTranslationOfQuery :" + ex.getMessage());
		}	
	}	
	
	/**
	 * @throws IOException 
	 * Gets the maximum index size of each language, useful in retrieving the number of results to be displayed
	 * We also get the corpus created date and corpus end date for trendingHashTags purposes. 
	 * Doing asc sort, gives us the creation date in created_at field; "DESC" sort gives us end date in created_at field
	 */
	public boolean getMaximumIndexSizeBasedOnLanguage() {
		String response, query = "";
		for  (int i = 0 ; i < langArray.length; i++)
		{
			try {
				query = String.format("q=*:*&fq=lang:%s&start=0&rows=0&wt=json&indent=true",URLEncoder.encode(langArray[i],HelperFunctions.charset));
				response = HelperFunctions.fetchHTTPData(HelperFunctions.twitterCoreURL + "/select" , query);
				maxIndexSizeData.put(langArray[i],new Integer(Integer.parseInt(langUnitObj.parseJSONResponseFromSolr(response,"")	)));
			} catch (Exception ex) {
				HelperFunctions.writeInformationIntoFile("Exception occur in getMaximumIndexSizeBasedOnLanguage:" + ex.getMessage());				
			}
			
		}
		if (maxIndexSizeData.size() != langArray.length) {
			return false;
		}
		else
			return true;
	}
	/**
	 * Fetch the corpus start date and end date from SOLR instance
	 */
	public void getCorpusDateFromSolr() {
	    if ( !(HelperFunctions.corpusCreatedDate.equals("")) && !(HelperFunctions.corpusEndDate.equals(""))  ) 
	        return;
		String response, query = "";
		String[] sort = {"asc","desc"};
		for  (int i = 0 ; i < sort.length; i++)
		{
			try {
				query = "q=*:*&start=0&rows=1&fl=created_at&wt=json&indent=true&sort=created_at+" + sort[i];
				response = HelperFunctions.fetchHTTPData(HelperFunctions.twitterCoreURL + "/select" , query);
			    langUnitObj.parseJSONResponseFromSolr(response,sort[i]);
			    
			} catch (Exception ex) {
				HelperFunctions.writeInformationIntoFile("Exception occur in getCorpusDateFromSolr:" + ex.getMessage());				
			}
			
		}
	}
	/**
	* Generates the query for SOLR to execute
	*/
	public String generateQuery(String query,String detectedLang) throws UnsupportedEncodingException {
		String queryFields = "q=";
		String qfText = "&qf=";
		String pfText = "";		
		getTranslationOfQuery(query,detectedLang);
		int i = 0;
		for ( i = 0 ; i < langArray.length ; i++) 
		{
			queryFields = queryFields + translationMap.get(langArray[i]); 
			if (i < langArray.length-1)
				queryFields = queryFields + "+";
			//make the qf and pf fields
				
			if ((!HelperFunctions.isTermHashTag(query)) && (detectedLang.equals(langArray[i])) ) {
				if (langUnitObj.getIsLangDetectionReliable()) { //Boost only if lang detection was reliable				
					qfText = qfText + String.format("text_" + langArray[i] + "^%s",
							URLEncoder.encode(String.valueOf(originalLangBoost),HelperFunctions.charset));
								  
					pfText = "&pf=" + String.format("text_" + langArray[i] + "^%s",
							URLEncoder.encode(String.valueOf(originalLangBoost),HelperFunctions.charset));		  
				} else {
				    qfText = qfText + String.format("text_" + langArray[i] + "^%s", 
						URLEncoder.encode(String.valueOf(otherLangBoost),HelperFunctions.charset));
				}	
			}
			else {
				qfText = qfText + String.format("text_" + langArray[i] + "^%s",
						URLEncoder.encode(String.valueOf(otherLangBoost),HelperFunctions.charset));
			}	
			if (i < langArray.length-1)
				qfText = qfText + "+";
		}
		if ( maxIndexSizeData.containsKey(detectedLang)) {
			queryFields = queryFields + String.format("&wt=json&indent=true&start=0&rows=%s",String.valueOf(maxIndexSizeData.get(detectedLang)));
		}
		else {
		    try {
			    queryFields = queryFields + String.format("&wt=json&indent=true&start=0&rows=%s",String.valueOf(maxIndexSizeData.get("en")));
		    } catch(Exception ex) {
		        queryFields = queryFields + String.format("&wt=json&indent=true&start=0&rows=%s",String.valueOf(1000));
		    }    
		}		
		queryFields = queryFields + "&defType=dismax" +  qfText + pfText;
		return queryFields;	
	}
	/**
	 * Add the start date and end date fields to the query
	 * If the user did not specify anything, we take the corpusStartDate and corpusEndDate
	 * */
	public String addDateRangeFields(HttpServletRequest request,String query) {
	    String startDate = "", endDate = "";
		try {
        	startDate = request.getParameter("start_date");
    		endDate   = request.getParameter("end_date");
		} catch(Exception ex) {
		    
		}	
		
		if ( ((startDate == null) || (startDate.equals(""))  ) ||  ((endDate == null) || (endDate.equals(""))) )
		{
		   startDate = HelperFunctions.corpusCreatedDate;
		   endDate   = HelperFunctions.corpusEndDate;
		}	    
		try {
	        query = query + "&fq=created_at:[\"" + startDate + "/DAY\"" + "%20TO%20" + "\"" + endDate  + "/DAY\"" + "]";
		} catch(Exception ex) {
		    HelperFunctions.writeInformationIntoFile("Exception in addDateRangeFields :" + ex.getMessage());
		}
		
	    return query;
	}
	/**
	 * Adds additional boosts if the string qualifies to be a phrase, i.e. length exceeds constant phraseLength
	 */
	public String addRequiredPhraseBoosts(String origQuery,String query) {
	    if ( (origQuery.equals("")) || (query.equals("")) ) 
	        return query;
	    String[] origQueryArr = origQuery.split("\\+"); // URL encode for " " is '+'
	    try { 
    	    if (origQueryArr.length >= phraseLength) {
    	        query  = query + "&ps=" + URLEncoder.encode(String.valueOf(phraseQrySlop),HelperFunctions.charset) 
    	                + "&qs=" + URLEncoder.encode(String.valueOf(phraseQrySlop),HelperFunctions.charset);
    	    }
    	    
	    } catch ( Exception ex)    { 
	        HelperFunctions.writeInformationIntoFile("Exception in addRequiredPhraseBoosts :" + ex.getMessage());    
	    }   
	    return query;
	}
	/**
	 * Boost recent documents ( Document in a gap of one week are considered recent)
	 * the formula is f(x) = a/(mx + b) where recip(x,m,a,b)
	 * Recent document given twice as more importance than older documents
	 */ 
	public String boostRecentDocuments(String query) {
	    if (query.equals(""))
	        return query;
	        
	    query = query + "&bf=recip(ms(NOW,created_at)," + String.valueOf(TimeUnit.DAYS.toMillis(1) * 7) + ",2,1)";
	    return query;
	}
	/**
	*  Execute the query on VSM core on SOLR
	 * @throws IOException 
	 * 
	 */
	public String runQueriesOnSolr(String query,String detectedLang,HttpServletRequest request) throws Exception {
		String response = "";
		try {	
			String origQuery = query;
			query = generateQuery(query, detectedLang);	
		    query = addRequiredPhraseBoosts(origQuery,query);
			query = addFacetingParams(query, "", "");
			query = addStatsParams(query);
			query = addDateRangeFields(request,query);
			query = addRequiredPhraseBoosts(origQuery,query);
			query = boostRecentDocuments(query);
			/**
			 * We introduce boost based on created_at field, recent tweet is boosted
			 * tieBreaker score is used as well
			 */ 
			query= query + "&tie=0.1";
			//Now, we are ready to query SOLR with queryFields
			response = HelperFunctions.fetchHTTPData(HelperFunctions.twitterCoreURL + "/select",  query);
		} catch(Exception ex) {
			response = "exception in generateQuery:" + ex.getMessage();
		}	
		return response;	
	}
	/**
	* Adds the various faceting fields based on the users selection
	*/
	public String addFacetingParams(String query,String StartDate,String EndDate) throws ParseException,JSONException, UnsupportedEncodingException {
		//Add all the field names required for faceting in below request
		String facetQuery = "";
		
		facetQuery = facetQuery + "&facet=true&facet.field={!ex=lg}lang&facet.field={!ex=eth}entities_tweet_hashtags"
		             + "&facet.field={!ex=ctl}concepts_text_list&facet.field={!ex=etl}entities_text_list"
		             + "&facet.field={!ex=src}source&facet.field={!ex=uv}users_verified&facet.field={!ex=txt}text";		             
		//Adding facet limiting parameters
		facetQuery = facetQuery + "&f.source.facet.limit=10";
		//For range parameters we need date 
		if(StartDate.equals("")||StartDate.equals(null)||EndDate.equals("")||EndDate.equals(null)){	//Checking if start date and end date is passed from user, if not fetching it from corpus 
			StartDate = HelperFunctions.corpusCreatedDate;
			EndDate   = HelperFunctions.corpusEndDate;

		}
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Date sDate=null;Date eDate=null;
		sDate=df.parse(StartDate);
		eDate=df.parse(EndDate);
		long diff=eDate.getTime()-sDate.getTime();
		long diffDays = diff / (24 * 60 * 60 * 1000);		
		if  ((diffDays > 0) && ( diffDays<=30)) {  // If the difference in days is less than a month then we sample the data per day
			facetQuery  =facetQuery + "&facet.range=created_at&f.created_at.facet.range.start="
			             + StartDate
			             +"&f.created_at.facet.range.end="
			             + EndDate
			             +"&f.created_at.facet.range.gap=%2B1DAY";
		}
		else//We sample the data per month
			facetQuery= facetQuery
						 +"&facet.range=created_at&f.created_at.facet.range.start="
						 + StartDate
						 +"&f.created_at.facet.range.end="
						 + EndDate
						 +"&f.created_at.facet.range.gap=%2B1MONTH";
		if(isFilterRequired.equalsIgnoreCase("true")){
			if ((langFilter!=null)  && (!langFilter.equalsIgnoreCase(""))){
				String[] langFilterArray=langFilter.split(",");
				//For language filtering
				if(langFilterArray.length==1) {
					
					facetQuery= facetQuery + "&fq={!tag=lg}lang:"+ HelperFunctions.escapeQueryChars(langFilterArray[0]);
				}	
				else{
					facetQuery= facetQuery + "&fq={!tag=lg}lang:"+ HelperFunctions.escapeQueryChars(langFilterArray[0]);
					for(int i=1;i<langFilterArray.length;i++){
						facetQuery = facetQuery + "%20OR%20lang:"+ HelperFunctions.escapeQueryChars(langFilterArray[i]);
					}
				}
			}
			
			if ((sourceFilter!=null) && (!sourceFilter.equals(""))){
				String[] sourceFilterArray=sourceFilter.split(",");
				//For Source filtering
				if(sourceFilterArray.length==1) { 
					facetQuery= facetQuery +"&fq={!tag=src}source:\""
					            +URLEncoder.encode(HelperFunctions.escapeQueryChars(sourceFilterArray[0]),"UTF-8")+"\"";
				}	
				else{
					facetQuery = facetQuery 
					            + "&fq={!tag=src}source:\""
					            +URLEncoder.encode(HelperFunctions.escapeQueryChars(sourceFilterArray[0]),"UTF-8")+"\"";
					for(int i=1;i<sourceFilterArray.length;i++){
						facetQuery=  facetQuery +"%20OR%20source:\""
						           + URLEncoder.encode(HelperFunctions.escapeQueryChars(sourceFilterArray[i]),"UTF-8")+"\"";
					}
				}
			}
			
			if((verifiedFilter !=null) && (!verifiedFilter.equals(""))){
				String[] verifiedFilterArray=verifiedFilter.split(",");			
				
				//For verified users filtering
				if(verifiedFilterArray.length==1)
					facetQuery = facetQuery + "&fq={!tag=uv}users_verified:"+ HelperFunctions.escapeQueryChars(verifiedFilterArray[0]);
				else{
					facetQuery = facetQuery + "&fq={!tag=uv}users_verified:"+ HelperFunctions.escapeQueryChars(verifiedFilterArray[0]);
					for(int i=1;i<verifiedFilterArray.length;i++){
						facetQuery = facetQuery + "%20OR%20users_verified:"+ HelperFunctions.escapeQueryChars(verifiedFilterArray[i]);
					}
				}
			}
			
		}
	    query = query + facetQuery;
		return query;
	}
	/**
	* Adds stats faceting parameters on field docSentiment_score and docSentiment_type
	*/
	public static String addStatsParams(String query){
		return query+"&stats=true&stats.field=docSentiment_score"
		            +"&stats.facet=lang&stats.facet=docSentiment_type"
		            + "&stats.facet=users_verified&stats.field=users_followers_count";
	}
	
    /**
	* Gets trendingHashTags based on startDate and endDate parameter  
    */ 
	public String fetchTrendingHashtags(HttpServletRequest request,String resp) {
		/***********hashtags block*************/
		String temp="",startDate="",endDate="";
		try {
        	startDate = request.getParameter("start_date");
    		endDate = request.getParameter("end_date");
		} catch(Exception ex) {
		    
		}	
		
		if ( ((startDate == null) || (startDate.equals(""))  ) ||  ((endDate == null) || (endDate.equals(""))) )
		{
		   startDate = HelperFunctions.corpusCreatedDate;
		   endDate   = HelperFunctions.corpusEndDate;
		}    
		if ( ((startDate == null) || (startDate.equals(""))  ) ||  ((endDate == null) || (endDate.equals(""))) )
	    {
	        HelperFunctions.writeInformationIntoFile("Did not fetch hashtags as created_at fields could not be fetched !!");
	        return resp;
	    }     
    		List<String> trendingHashTags = hashTagTrends.getTrendingHashTags(startDate, endDate);
        temp = langUnitObj.addArrayToJSON(trendingHashTags,resp,"trending_hashtags");
		return temp;
	}
	
	// Fetch news snippet from Guardian and NYT query
	public String fetchNewsArticles(HttpServletRequest request, String resp) {
		String queryText = request.getParameter("q").trim();
		String query = "q=";
		String news = "";
		String newsJson = "";
		String core0NewsResp;
		String core1NewsResp;

		query = query + queryText + "&wt=json&indent=true&start=0&rows=10";
		try {
				// Get the news from core 0
				core0NewsResp = HelperFunctions.fetchHTTPData(HelperFunctions.guardianCoreURL+ "/select" , query);
				if (core0NewsResp.equals("")) {
                	HelperFunctions.writeInformationIntoFile("Failed to fetch news articles from guardianCoreURL!");
				} else {
					// append the result to the existing data
					JSONObject jnewsResp = new JSONObject(core0NewsResp);
    				JSONObject jResp = new JSONObject(resp);
					jResp = jResp.put("newsGuardian", jnewsResp);
					resp = jResp.toString();
				}

				// Get the news from core 1
				core1NewsResp = HelperFunctions.fetchHTTPData(HelperFunctions.nytCoreURL+ "/select" , query);
				if (core1NewsResp.equals("")) {
                	HelperFunctions.writeInformationIntoFile("Failed to fetch news articles from nytCoreURL :!");
				} else {
					// append the result to the existing data
					JSONObject jnewsResp = new JSONObject(core1NewsResp);
    				JSONObject jResp = new JSONObject(resp);
					jResp = jResp.put("newsNYT", jnewsResp);
					resp  = jResp.toString();
				}

				news = resp;
		} catch (Exception ex) {
				HelperFunctions.writeInformationIntoFile("Exception occur in fetchNewsArticles:" + ex.getMessage());				
		}
	
		return news;	
    }	
	
   // The doGet() runs once per HTTP GET request to this servlet.
   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response)
               throws ServletException, IOException {	
		// Set the MIME type for the response message
	
		response.setContentType("text/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
	    langFilter      = "";
	    sourceFilter    = "";
        verifiedFilter  = "";
	    isFilterRequired= "";
		// Get a output writer to write the response message into the network socket
		PrintWriter pWriter  = response.getWriter();
		HelperFunctions.pWriter = pWriter;

		int responseCode = HelperFunctions.testURL(HelperFunctions.twitterCoreURL+"/select");
		if ( responseCode != 200) {
			pWriter.println(String.format("{\"error\":{\"msg\":\"Connecting to %s failed.Please check SOLR instance is running\",\"code\":%s}}",HelperFunctions.twitterCoreURL+ "/select",String.valueOf(responseCode)));
			return;
		}	
		request.setCharacterEncoding("UTF-8");
		HelperFunctions.sBuilder = new StringBuilder();
	    
		String queryText = "";
	    if (request.getParameter("q") == null )  {
	        return;
	    }     
        queryText = request.getParameter("q");
		if (queryText.equalsIgnoreCase("")) {
		    return;
		}
	    String origQuery = queryText;     
	    
		isFilterRequired = "false";
		if (request.getParameter("isFilterRequired") != null) {
		    isFilterRequired = request.getParameter("isFilterRequired").trim();
		}
		if(isFilterRequired.equalsIgnoreCase("true")){
			if (request.getParameterMap().containsKey("lang")) {
			   
				langFilter = request.getParameter("lang").trim();
				langFilter=langFilter.substring(1, langFilter.length()-1);//Removing beginning and ending double quotes
	        }
			if (request.getParameterMap().containsKey("source")){
				sourceFilter = request.getParameter("source").trim();
				
				sourceFilter = sourceFilter.substring(1, sourceFilter.length()-1);//Removing beginning and ending double quotes
			}
			if (request.getParameterMap().containsKey("users_verified")){
				verifiedFilter = request.getParameter("users_verified").trim();
				verifiedFilter = verifiedFilter.substring(1, verifiedFilter.length()-1);//Removing beginning and ending double quotes
			}
			
		}
		try {
			langUnitObj.getLanguageOfQuery(queryText);
			if (!langUnitObj.getIsLangDetectionReliable()) { //if language detection wasn't ok, set "en" as default in case it did not detect any of the supported languages
				if (!( Arrays.asList(langArray).contains(new String(langUnitObj.getDetectedLang()))))
				    langUnitObj.setDetectedLang("en");
			}	
			try {
				
				if (!getMaximumIndexSizeBasedOnLanguage())
					HelperFunctions.writeInformationIntoFile("Could not find max index size");
				    String resp = runQueriesOnSolr(queryText,langUnitObj.getDetectedLang(),request);
                    String temp = fetchTrendingHashtags(request,resp);
                    if (!temp.equals(""))
                        resp = temp;
                        
    				if (langUnitObj.getDetectedLang().equals("en")) {
    					String news = fetchNewsArticles(request, resp);
    					if (!news.equals(""))
    						resp = news;
    				}
                        
        			pWriter.println(resp);	
				
			}
			catch(Exception ex) {
				HelperFunctions.writeInformationIntoFile("exception in outer body :" + ex.getMessage());
			}	
		}
		finally {
			pWriter.close();
		}	
	}
}	