#!/bin/bash

cd /var/www/html/telefynaConfiguration/;
cat mailer.txt exports/$(date '+%Y-%m-%d')*.json exports/$(date '+%Y-%m-%d')*.txt  > content.txt;
cat content.txt | msmtp --debug -a gmail apps@avventohome.org;
rm exports/$(date '+%Y-%m-%d')*.json;
rm exports/$(date '+%Y-%m-%d')*.txt;
rm content.txt;
cd -;