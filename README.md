# IR_System
Collaboration folder for creating IR system on social networks with analytics information 

This project was done under course requirement "Information retrieval" by "Prof. Rohini Srihari".

The main objective of this project is to implement and evaluate a multi-lingual search system using twitter data. 
We named our product accordingly as “Social curry”. Social Curry is an interactive search system which presents multi-lingual results 
from twitter based on user query, along with interesting charts and graphs pertinent to the query result set.

The primary source of the data consists of more than 10000 tweets collected from twitter over a period of a few weeks, plus more than 10K
news articles from Guardian and NYT.

Using Vector space model as the search model in Apache Solr, we created a robust, scalable, and reliable search system which
along with displaying the tweets in native format ( as seen on actual twitter.com ), also gives the users the faceted search option
on the left, and various interesting analytics of the query results such as trending hashtags, sentiment analysis, data source etc.
among others, which are presented using attractive graphs.

We created the project for 5 languages, namely, English, French, German, Russian and Arabic on the topic of "Refugee crisis in Europe".
The data indexed gives a broader perspective to this topic by generating the summaries on the topics searched from Wikipedia, and 
fetching relevant news articles from Guardian and NY Times.

Collaborators :

1. Jaideep Bhoosreddy @jaideepcoder
2. Suhas Subaramanyam @suhassubramanyam
3. Vinay Goyal @vinaygoyal
4. Sunandan Barman @sunandanbarman

Screenshots below :

**Welcome screen**
![welcome screen](../master/screenshots/IntroScreen.png?raw=true)
This is the welcome screen for the user.

**Search results**
![Search results](../master/screenshots/SearchResults.png?raw=true)
This is the search results shown to the user.
List of features :

1. Faceting based on langugage of tweet, Source of tweet, and verified/unverified users. ( See left sidebar )
2. Fetching the summary from Wikipedia, uses the top entity found in Tweets data returned as part of search 
3. Trending hashtags (both based on the whole corpus), and also from the found results
4. Fetching top news articles from "The Guardian" on the searched topic
5. Show the tweet in native twitter format, including support for Video, GIFs etc.

**Word cloud and Content tagging**
![Word cloud and Content tagging](../master/screenshots/WordCloudAndContentTag.png?raw=true)

Shows the word cloud from the generated data, plus sentiment analysis of the results.
Also shows the entities in each tweet along with the results

**Time series graph**
![More analysis graphs](../master/screenshots/GraphAnalysis.png?raw=true)

Time series graph showing the rise/fall of the searched topic. Can be used to track the interest in a topic during
a particular time period.
