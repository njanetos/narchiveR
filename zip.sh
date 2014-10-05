 #!/bin/bash

cd /home/ubuntu/narchiver

d=`date +"%m-%d-%y"`;

mkdir archive"$d";

for file in marketplace hydra silkroad agora
do
	mkdir tmp;
	cd $file;

	for f in */; do
	        g=`echo "$f" | cut -d'/' -f1`;
	        zip -r "$g"".zip" $g;
	        mv "$g"".zip" /home/ubuntu/narchiver/tmp;
	done

	cd ..

	cd tmp

	zip -r "/home/ubuntu/narchiver/""archive""$d""/""$file""$d"".zip" .;

	cd ..

	rm -r tmp;
	rm -r $file;
done

zip -r "archive""$d"".zip" "archive""$d";
rm -r "archive""$d";