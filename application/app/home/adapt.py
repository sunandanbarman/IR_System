import os
from os import listdir
from os.path import isfile, join
import json
mypath = os.getcwd()
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]

#print onlyfiles

for individualFile in onlyfiles: 
    print individualFile
    if individualFile.startswith("Twitter"):
        file = open(individualFile, 'r')
        print file.read()
        