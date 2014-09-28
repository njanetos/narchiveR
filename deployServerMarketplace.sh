#!/bin/bash

cd /home/njanetos/Dropbox/Codes
scp -i narchiver.pem /home/njanetos/Dropbox/Programming/Narchiver/target/Narchiver-1.0.jar ubuntu@54.68.215.35:/home/ubuntu/narchiver
scp -i narchiver.pem /home/njanetos/Dropbox/Programming/Narchiver/initialize.json ubuntu@54.68.215.35:/home/ubuntu/narchiver
