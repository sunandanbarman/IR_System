import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

public class HashTagTrends {
    
    private Map<String, int[]> counts = new HashMap<String, int[]>();
    private String solrURLString;
    private Date corpusStartDate;
    private Date corpusEndDate;
    
    private int k = 10; // top k trending hashtags
    
    public HashTagTrends(Properties config) {
        
        loadConfig(config);
        
        init();
    }
    @SuppressWarnings("deprecation")    
    private void init(){
        
        int NUM_OF_DAYS = getDateDiffInDays(corpusStartDate, corpusEndDate);
        Date start_date = (Date) corpusStartDate.clone(); // dirty, but works
        Date end_date = (Date) corpusStartDate.clone();
        end_date.setDate(end_date.getDate() + 1); // set end date to next day

        try {

            // get all the hashtags
            JSONArray hashTagAndCounts;
            String response = HelperFunctions.fetchHTTPData(HelperFunctions.twitterCoreURL + "/terms",
                              "wt=json&terms.fl=entities_tweet_hashtags&terms.sort=count&terms.limit=-1");
            JSONObject resp = null;
            if (!response.equals(""))
            {
                resp = new JSONObject(response);
                hashTagAndCounts = ((JSONObject) resp.get("terms")).getJSONArray("entities_tweet_hashtags");
                for (int i = 0; i < hashTagAndCounts.length(); i += 2) {
                    counts.put(hashTagAndCounts.getString(i), new int[NUM_OF_DAYS]);
                }    
                
            } else {
                HelperFunctions.writeInformationIntoFile("Failed to fetch anything !");
            }

        } catch (JSONException e) {
            HelperFunctions.writeInformationIntoFile("Exception in init of hashtags trends. Reason :" + e.getMessage());
        }
        
        // get hashtag counts for individual day
        for (int day = 0; day < NUM_OF_DAYS; ++day) {
                       
            try {
                
                String response = HelperFunctions.fetchHTTPData(HelperFunctions.twitterCoreURL + "/select","q=*:*&fq=created_at:["
                        + URLEncoder.encode(formatDate(start_date) + " TO " + formatDate(end_date), "UTF-8")
                        + "]&wt=json&facet=true&facet.field=entities_tweet_hashtags&facet.sort=count");
                JSONObject resp;
                if (!response.equals("")) {
                    resp = new JSONObject(response);
                    JSONArray hashTagAndCounts = ((JSONObject) resp.get("facet_counts")).getJSONObject("facet_fields")
                            .getJSONArray("entities_tweet_hashtags");
    
                    for (int i = 0; i < hashTagAndCounts.length(); i += 2) {
                        if ( !(hashTagAndCounts.getString(i).equals("[]")) ) {
                            int[] dayCounts = counts.get(hashTagAndCounts.getString(i));
                            dayCounts[day] = hashTagAndCounts.getInt(i + 1);
                        }    
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            
            start_date.setDate(start_date.getDate() + 1); // dirty, but works
            end_date.setDate(end_date.getDate() + 1);
        }

    }
    
    /**
     * Method to get top-k trending hashtags
     * 
     * Note: The date format expected is "yyyy-MM-dd'T'HH:mm:ss'Z'"
     * 
     * @param sd  Start date
     * @param ed  End date
     * @return
     */
    public List<String> getTrendingHashTags(String sd, String ed){
        
        Date startDate = parseDate(sd);
        Date endDate = parseDate(ed);
        
        List<String> ret = new ArrayList<String>(counts.size());
        
        int numOfDays = getDateDiffInDays(startDate, endDate);
        int start = getDateDiffInDays(corpusStartDate, startDate); // diff b/w req start date and corpus data start date
        
        List<Score> scores = new ArrayList<Score>(counts.size());
        
        // compute scores for each hash tag - basically tf-idf weighted on days
        Set<String> keys = counts.keySet();
        for (String key : keys) {
            int daysAppeared = 0;
            double score = 0.0;
            double weight = 0.01;
            int[] dayCounts = counts.get(key);
            for (int i = start; i < start + numOfDays; ++i) {
                
                if (dayCounts[i] > 0) {
                    score += (double)dayCounts[i] * weight;
                    weight *= 3;
                    ++daysAppeared;
                }
            }
            
            // if numOfDays == daysAppeared, then idf does not make sense
            if (daysAppeared > 0 && numOfDays >1) {
                score *= Math.log((double)numOfDays/daysAppeared);
            }
            
            scores.add(new Score(key, score));
        }
        
        // sort in non-ascending order
        Collections.sort(scores);
        
        // return top-k trending hashtags
        for (int i = 0; i < k; ++i) {
            ret.add(scores.get(i).getHashTag());
        }
        
        return ret;
    }

    /**
     * Method to read the search keywords from the file
     * 
     * @return
     */
    private void loadConfig(Properties props) {
        this.corpusStartDate = parseDate(HelperFunctions.corpusCreatedDate);
        this.corpusEndDate   = parseDate(HelperFunctions.corpusEndDate);
    }
    
    /**
     * 
     * @param dateInString
     * @return
     */
    private Date parseDate(String dateInString) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        try {
            return formatter.parse(dateInString);
        } catch (ParseException e) {
            HelperFunctions.writeInformationIntoFile("Exception in parse date. Reason :" + e.getMessage());
            return null;
        }
    }

    /**
     * 
     * @param ipUrl
     * @return
     * @throws JSONException
     */
    private JSONObject getResponse(String ipUrl) throws JSONException {
        JSONObject jsonResponse = null;

        StringBuilder response = new StringBuilder();
        URLConnection con;
        try {
            URL url = new URL(ipUrl);
            con = url.openConnection();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),"UTF-8"))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                jsonResponse = new JSONObject(response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e1) {
            HelperFunctions.writeInformationIntoFile("Exception in getResponse. Reason :" + e1.getMessage());
        }

        return jsonResponse;
    }
    
    private String formatDate(Date date){
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
    
    private int getDateDiffInDays(Date startDate, Date endDate) {
        // casting to int, since this diff will be a small value
        return (int)TimeUnit.DAYS.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
    }

}
