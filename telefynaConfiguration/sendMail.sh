#!/bin/bash

cd /var/www/html/telefynaConfiguration/;
FILES=exports/$(date '+%Y-%m-%d')*.txt
if compgen -G "$FILES" > /dev/null; then
    cat mailHeader.txt exports/$(date '+%Y-%m-%d')*.txt mailFooter.txt > content.txt;
    cat content.txt | msmtp --debug -a gmail apps@avventohome.org;
    rm content.txt;
fi
cd -;