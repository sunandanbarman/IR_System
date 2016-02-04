'use strict';

// Declare app level module which depends on views, and components
angular.module('myApp', [
	'ngRoute',
	'myApp.home',
	'myApp.view2',
	'myApp.version'
	//'chart.js',
	//'ui.bootstrap'
	]).
config(['$routeProvider', function($routeProvider) {
	$routeProvider.otherwise({redirectTo: '/home'});
}]).

config(['$httpProvider', function($httpProvider) {
	$httpProvider.defaults.useXDomain = true;
	$httpProvider.defaults.withCredentials = true;
	delete $httpProvider.defaults.headers.common["X-Requested-With"];
	$httpProvider.defaults.headers.common["Accept"] = "application/json";
	$httpProvider.defaults.headers.common["Content-Type"] = "application/json";
}
]).

filter('startFrom', function() {
return function(input, start) {
    if(input) {
        start = +start; //parse to int
        var appended = input.slice(0,start);
        var initialArray = input.slice(start);
        var finalArray= initialArray.concat(appended);
        return finalArray;
    }
    return [];
}
}).

directive('onFinishRender', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            if (scope.$last === true) {
                $timeout(function () {
                    scope.$emit('ngRepeatFinished');
                    twttr.widgets.load(); 
                });
            }
        }
    }
});


window.twttr = (function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0],
    t = window.twttr || {};
  if (d.getElementById(id)) return t;
  js = d.createElement(s);
  js.id = id;
  js.src = "https://platform.twitter.com/widgets.js";
  fjs.parentNode.insertBefore(js, fjs);
 
  t._e = [];
  t.ready = function(f) {
    t._e.push(f);
  };
 
  return t;
}(document, "script", "twitter-wjs"));
twttr.ready(function (twttr) {

    twttr.widgets.load();
});
