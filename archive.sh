 #!/bin/bash

cd /home/njanetos/Dropbox/Codes;
ssh -i narchiver.pem ubuntu@54.68.121.67 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/zip.sh;
d=`date +"%m-%d-%y"`;
scp -i narchiver.pem ubuntu@54.68.121.67:/home/ubuntu/narchiver/"archive""$d"".zip" /home/njanetos/Data
ssh -i narchiver.pem ubuntu@54.68.121.67 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/cleanup.sh;
mv /home/njanetos/Data/"archive""$d"".zip" /home/njanetos/Data/"archive""$d""_oregon.zip"
cd /home/njanetos/Dropbox/Programming/Narchiver
java -jar glacier.jar --endpoint https://glacier.us-west-2.amazonaws.com --vault NarchiverData --upload /home/njanetos/Data/"archive""$d""_oregon.zip" --credentials /home/njanetos/.glacier/credentials;
cp /home/njanetos/Data/"archive""$d""_oregon.zip" /media/njanetos/139A0086189CFE15

cd /home/njanetos/Dropbox/Codes;
ssh -i narchiver_virginia.pem ubuntu@54.173.238.106 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/zip.sh;
d=`date +"%m-%d-%y"`;
scp -i narchiver_virginia.pem ubuntu@54.173.238.106:/home/ubuntu/narchiver/"archive""$d"".zip" /home/njanetos/Data
ssh -i narchiver_virginia.pem ubuntu@54.173.238.106 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/cleanup.sh;
mv /home/njanetos/Data/"archive""$d"".zip" /home/njanetos/Data/"archive""$d""_virginia.zip"
cd /home/njanetos/Dropbox/Programming/Narchiver
java -jar glacier.jar --endpoint https://glacier.us-west-2.amazonaws.com --vault NarchiverData --upload /home/njanetos/Data/"archive""$d""_virginia.zip" --credentials /home/njanetos/.glacier/credentials;
cp /home/njanetos/Data/"archive""$d""_virginia.zip" /media/njanetos/139A0086189CFE15

cd /home/njanetos/Dropbox/Codes;
ssh -i narchiver_california.pem ubuntu@54.67.75.9 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/zip.sh;
d=`date +"%m-%d-%y"`;
scp -i narchiver_california.pem ubuntu@54.67.75.9:/home/ubuntu/narchiver/"archive""$d"".zip" /home/njanetos/Data
ssh -i narchiver_california.pem ubuntu@54.67.75.9 'bash -s' < /home/njanetos/Dropbox/Programming/Narchiver/cleanup.sh;
mv /home/njanetos/Data/"archive""$d"".zip" /home/njanetos/Data/"archive""$d""_california.zip"
cd /home/njanetos/Dropbox/Programming/Narchiver
java -jar glacier.jar --endpoint https://glacier.us-west-2.amazonaws.com --vault NarchiverData --upload /home/njanetos/Data/"archive""$d""_california.zip" --credentials /home/njanetos/.glacier/credentials;
cp /home/njanetos/Data/"archive""$d""_california.zip" /media/njanetos/139A0086189CFE15