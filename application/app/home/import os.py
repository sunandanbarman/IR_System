import os
from os import listdir
from os.path import isfile, join

mypath = os.getcwd()
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]

#print onlyfiles

for individualFile in onlyfiles: 
    #print individualFile
    if individualFile.startswith("Twitter"):
        os.system('curl \'http://sunandan.koding.io:8983/solr/TwitterCore/update/json?commit=true\' --data-binary @$(echo individualFile) -H \'Content-type:application\')