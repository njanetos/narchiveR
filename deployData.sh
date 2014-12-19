#!/bin/bash

cd /home/njanetos/Dropbox/Codes

scp -i narchiver.pem /home/njanetos/Downloads/drugs_Listing.csv ubuntu@54.68.121.67:/home/ubuntu/narchiver
scp -i narchiver.pem /home/njanetos/Downloads/drugs_Listing_prices.csv ubuntu@54.68.121.67:/home/ubuntu/narchiver
scp -i narchiver.pem /home/njanetos/Downloads/drugs_Listing_reviews.csv ubuntu@54.68.121.67:/home/ubuntu/narchiver
scp -i narchiver.pem /home/njanetos/Downloads/drugs_Vendor.csv ubuntu@54.68.121.67:/home/ubuntu/narchiver