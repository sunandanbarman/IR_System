'use strict';

angular.module('myApp.home', ['ui.bootstrap', 'ngRoute', 'chart.js', 'angular-underscore', 'angular-jqcloud', 'daterangepicker'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/home', {
    templateUrl: 'home/home.html',
    controller: 'HomeCtrl'
  });
}])

.config(function($sceProvider) {
  // Completely disable SCE.  For demonstration purposes only!
  // Do not use in new projects.
  $sceProvider.enabled(false);
})

.controller('HomeCtrl', function($scope, $http, $timeout) {
$scope.pacers = ['barber-shop', 'big-counter', 'bounce', 'center-atom', 'center-circle', 'center-radar', 'center-simple', 'corner-indicator', 'fill-left', 'flash', 'flat-top', 'loading-bar', 'mac-osx', 'minimal'];
$scope.searchFlag = false;
$scope.members = [
    {'name': 'Jaideep Bhoosreddy',
     'ubmail': 'jaideepb@buffalo.edu',
 	 'personnumber': '50169077'}
  ];
$scope.words = [];
$scope.result = {};
$scope.pageSize = 10;
$scope.datePicker = {};
$scope.datePicker.date = {startDate: '2015-11-24',
							endDate: '2015-12-06'
						};

$scope.words = $scope.words;
$scope.lang = {};
$scope.source = {};
$scope.verified = {};
$scope.favorited = {};

$scope.submit = function() {
	console.log($scope.datePicker.date);
	// return;
	var params = {
		'q': $scope.parseQuery($scope.query),
		'isFilterRequired': false
	};
	if($scope.getTrueKeys($scope.lang).toString() != "") {
		params['lang'] = '"'+$scope.getTrueKeys($scope.lang).toString()+'"';
		params['isFilterRequired'] = true;
	}
	if($scope.getTrueKeys($scope.source).toString() != "") {
		params['source'] = '"'+$scope.getTrueKeys($scope.source).toString()+'"';
		params['isFilterRequired'] = true;
	}
	if($scope.getTrueKeys($scope.verified).toString() != "") {
		params['users_verified'] = '"'+$scope.getTrueKeys($scope.verified).toString()+'"';
		params['isFilterRequired'] = true;
	}
	if($scope.getTrueKeys($scope.favorited).toString() != "") {
		params['favorited'] = '"'+$scope.getTrueKeys($scope.favorited).toString()+'"';
		params['isFilterRequired'] = true;
	}
	if($scope.datePicker.date.startDate != '2015-11-24' && $scope.datePicker.date.endDate != '2015-12-06') {
		params['start_date'] = $scope.datePicker.date.startDate.format('YYYY-MM-DDTHH:mm:ss')+'Z';
		params['end_date'] = $scope.datePicker.date.endDate.format('YYYY-MM-DDThh:mm:ss')+'Z';
		params['isFilterRequired'] = true;
	}
	$scope.currentPage=0;
	$scope.url = 'http://jaideep.koding.io:8080/IRHttpServer/query';
	console.log($scope.url);
	$scope.facet = {};
	$http.get($scope.url, {
		params: params
	}).success(function(data) {
			if(data.error == undefined) {
				$scope.searchFlag = true;
				$scope.currentGuardian = 0;
				$scope.currentPage = 0;
				$scope.result = data;
				$scope.guardianNews = data['newsGuardian']['response']['docs'];
				if($scope.guardianNews.length > 10) {
					$scope.guardianNews.splice(0,10);
				}
				console.log($scope.guardianNews);
				$scope.facet['lang'] = $scope.getFacet(data['facet_counts']['facet_fields']['lang'], null);
				$scope.facet['source'] = $scope.getFacet(data['facet_counts']['facet_fields']['source'], 10);
				$scope.facet['users_verified'] = $scope.getFacet(data['facet_counts']['facet_fields']['users_verified'], null);
				$scope.facet['hashtags'] = Object.keys($scope.getTopHashTags($scope.getFacet(data['facet_counts']['facet_fields']['entities_tweet_hashtags'], 11)));

				// $scope.facet['favorited'] = $scope.getFacet(data['facet_counts']['facet_fields']['favorited'], null);
				$scope.facet['entities_text_list'] = $scope.getFacet(data['facet_counts']['facet_fields']['entities_text_list'], null);
				$scope.facet['created_at'] = $scope.getFacet(data['facet_counts']['facet_ranges']['created_at']['counts'], null);
				console.log(data['facet_counts']['facet_fields']['users_verified']);
				$scope.numberOfPages=function(){
		        	return Math.ceil($scope.result['response']['numFound']/$scope.pageSize);                
		    	}
				$scope.pielabels = Object.keys($scope.facet['lang']);
				var sentimentpie = data['stats']['stats_fields']['docSentiment_score']['facets']['docSentiment_type'];
				var sentlabels = [];
				var sentdata = [];
				for(var key in sentimentpie) {
					if(key == 'null') continue;
					sentlabels.push(key);
					sentdata.push(sentimentpie[key]['count']);
				}
				$scope.sentlabels = sentlabels;
				console.log($scope.sentlabels);
				console.log($scope.sentdata);
				$scope.pielabels = Object.keys($scope.facet['lang']);
	  			$scope.barlabels = $scope.parseSourceArray(Object.keys($scope.facet['source']));
	  			$scope.linelabels = $scope.parseCreatedArray(Object.keys($scope.facet['created_at']));
				$scope.summaryTerm = Object.keys($scope.getFacet(data['facet_counts']['facet_fields']['entities_text_list'], 1))[0];
	  			$timeout(function () {
	  				$scope.piedata = $scope.values($scope.facet['lang']);
	  				$scope.bardata = [$scope.values($scope.facet['source'])];
	  				$scope.words = $scope.getWC($scope.facet['entities_text_list']);
	  				$scope.linedata = [$scope.values($scope.facet['created_at'])];
					$scope.sentdata = sentdata;
	  			}, 2000);
			}
			else {
				// alert(data.error.msg);
			}
  	}).error(function(data) {
  		// alert({'myerror':data});
  	});
  	$scope.summary = "Fetching summary from dBpedia.org...";
	$timeout(function () {
		$scope.summary = $scope.getSummary($scope.summaryTerm);
	}, 10000);

  	console.log(params);
}


$scope.parseQuery = function(query) {
	query = query.replace(/ /g, '+');
	// query = encodeURI(query);
	// query = query.replace(/:/g, '\\:');
	// query = "text_en:".concat(query)+" text_de:".concat(query)+" text_ru:".concat(query)+" text_fr:".concat(query)+" text_ar:".concat(query);
	return query;
}

$scope.getText = function(array) {
	for (var i = 0; i < array.length; i++) {
		if(array[i] != "") {
			return array[i];
		}
	};
}

$scope.getTopHashTags = function(object) {
	var output = {};
	for(var key in object) {
		if (object[key] > 0) {
			if (key == '[]') continue;
			output[key] = object[key];
			console.log(object);
		}
		else {
			console.log('output: ');
			console.log(output);
			return output;
		}
	}
	return output;
}

$scope.getTweetHTML = function(tweet) {
	if(display[tweet.id]) {
		return tweet.html;
	}
	else {
		if(tweet.html != undefined) {
			return tweet.html.replace('<script async src="//platform.twitter.com/widgets.js" charset="utf-8"></script>', '');
		}	
	}
}

$scope.getSummary = function(q) {
	var limit = 500;
	$http.get('http://jaideep.koding.io/getsummary.php?q='+q.capitalize()).success(function (data) {
		if(data.length > limit) {
			data = data.substring(0,limit)+'...';
		}
		console.log(data);
		$scope.summary = data;
		$scope.summaryURL = 'https://en.wikipedia.org/wiki/'+q.capitalize();
		$scope.summaryTag = 'Read More on Wikipedia';
		// $scope.summaryURL = '<a href="https://en.wikipedia.org/wiki/'+q.capitalize()+'">Read More on Wikipedia</a>';
	});
}

$scope.getLang = function(data) {
	data = data['response']['docs'];
	var output = {};
	for (var i = 0; i < data.length; i++) {
		var lang = $scope.langMap[data[i].lang];
		if (output[lang] == undefined) {
			output[lang] = 1;
		}
		else {
			output[lang] += 1;
		}
	};
	return output;
}

$scope.getEntities = function(tweet) {
	var entities = [];
	// console.log(tweet['entities_text_list']);
	if(tweet['entities_text_list'] != undefined) {
		for (var i = 0; i < tweet['entities_text_list'].length; i++) {
			var entity = {};
			entity['text'] =tweet['entities_text_list'][i];
			entity['type'] =tweet['entities_type_list'][i];
			if (tweet['entities_dbpedia_list'][i] != 'null') {
				entity['url'] = tweet['entities_dbpedia_list'][i].replace('http://dbpedia.org/resource/', 'http://en.wikipedia.org/wiki/');
			}
			else {
				entity['url'] = 'http://en.wikipedia.org/wiki/'+entity.text.replace(' ', '_').replace('#', '').replace('@', '');
			}
			if(tweet['entities_type_list'][i] == "Hashtag") {
				entity['url'] = 'https://twitter.com/hashtag/'+tweet['entities_text_list'][i].replace('#', '');
			}
			else if(tweet['entities_type_list'][i] == "TwitterHandle") {
				entity['url'] = 'https://twitter.com/'+tweet['entities_text_list'][i].replace('@', '');
			}
			entities.push(entity);
		};
	}
	return entities;
	
}

$scope.getFacet = function(data, limit) {
	// alert(data);
	var output = {};
	if(limit==null) {
		limit = data.length;
	}
	else {
		limit = limit*2;
	}
	for (var i = 0; i < data.length && i < limit; i=i+2) {
		output[data[i]] = data[i+1];
	};
	return output;
}
$scope.searchHashTag = function(hashtag) {
	$scope.query = hashtag;
	$scope.clear();
	$scope.submit();
}
$scope.getTrueKeys = function(object) {
	var output = [];
	for (var key in object) {
		if (object[key] == true) {
			output.push(key);
		}
	}
	return output;
}

$scope.getWC = function(object) {
	// console.log(object);
	var output = [];
	var obj = {};
	for(var key in object) {
		obj = {
			text: key,
			weight: object[key],
			handlers: {
				click: function() {
					$scope.query = $scope.query +' '+ key;
					$scope.submit();
				}
			}
		}
		// obj['text'] = key;
		// obj['weight'] = object[key];
		// obj['handlers'] = click: function() {
		// 	$scope.query = $scope.query +' '+ key;
		// },
		output.push(obj);
		obj = {};
	}
	// console.log(output);
	return output;
}

$scope.parseSource = function(source) {
	var index = source.indexOf('">');
	source = source.substring(index+2);
	index = source.indexOf('</a>');
	source = source.substring(0, index);
	return source;
}

$scope.parseSourceArray = function(data) {
	for (var i = 0; i < data.length; i++) {
		var source = data[i];
		var index = source.indexOf('">');
		source = source.substring(index+2);
		index = source.indexOf('</a>');
		source = source.substring(0, index);
		data[i] = source;
	};
	return data;
}

$scope.parseCreatedArray =  function(data) {
	for (var i = 0; i < data.length; i++) {
		var date = data[i];
		data[i] = jQuery.timeago(date);
	};
	return data;
}

$scope.getTimeago = function(date) {
	return jQuery.timeago(date);
}

$scope.values = function(object) {
	var output = [];

	for (var key in object) {
		output.push(object[key]);
	};
	return output;
}

$scope.clear = function() {
	$scope.lang = {};
	$scope.source = {};
	$scope.verified = {};
	$scope.favorited = {};
}

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

$scope.langMap = {'en':'English', 'ru':'Russian', 'de':'German', 'fr':'French', 'ar':'Arabic'};
$scope.verifiedMap = {'true': 'Verified Users', 'false': 'Standard Users'};
$scope.favoriteMap = {'true': 'Favorited Tweets', 'false': 'Non Favorited Tweets'};
$scope.labels = ["January", "February", "March", "April", "May", "June", "July"];
$scope.series = ['Volume', 'Series B'];
  $scope.data = [
	    [65, 59, 80, 81, 56, 55, 40],
	    [28, 48, 40, 19, 86, 27, 90]
  ];
  $scope.onClick = function (points, evt) {
  	$scope.lang[points[0]['label']] = true;
  	$scope.submit();
  };

  // Simulate async data update
  $timeout(function () {
    $scope.data = [
      [28, 48, 40, 19, 86, 27, 90],
      [65, 59, 80, 81, 56, 55, 40]
    ];
  }, 3000);

  

});