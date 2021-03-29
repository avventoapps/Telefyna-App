#!/bin/bash

cd /var/www/html/telefynaConfiguration/;
FILES=exports/$(date '+%Y-%m-%d')*.txt
if compgen -G "$FILES" > /dev/null; then
    echo "DiskUsage:__________________________" > metrics.txt
    df -H >> metrics.txt
    echo "CPUUsage:__________________________" >> metrics.txt
    mpstat >> metrics.txt
    echo "Uptime:__________________________" >> metrics.txt
    uptime | awk -F'( |,|:)+' '{print $6,$7",",$8,"hours,",$9,"minutes."}' >> metrics.txt

    cat mailHeader.txt metrics.txt exports/$(date '+%Y-%m-%d')*.txt mailFooter.txt > content.txt;
    cat content.txt | msmtp --debug -a gmail apps@avventohome.org;
    rm metrics.txt
    rm content.txt;
fi
cd -;