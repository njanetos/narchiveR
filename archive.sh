 #!/bin/bash

cd /home/njanetos/Dropbox/Codes;

ssh -i narchiver.pem ubuntu@54.68.121.67 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/zip.sh;

d=`date +"%m-%d-%y"`;

scp -i narchiver.pem ubuntu@54.68.121.67:/home/ubuntu/narchiver/"archive""$d"".zip" /home/njanetos

ssh -i narchiver.pem ubuntu@54.68.121.67 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/cleanup.sh;

cd /home/njanetos/Dropbox/Programming/Narchiver

java -jar glacier.jar --endpoint https://glacier.us-west-2.amazonaws.com --vault NarchiverData --upload /home/njanetos/"archive""$d"".zip" --credentials /home/njanetos/.glacier/credentials;