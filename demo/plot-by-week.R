# test2
rm(list = ls())
library("drugs")
library("data.table")
select.database("drugs_agora");

# query the price data
mdma = get.query("SELECT * FROM Listing L
                            INNER JOIN Listing_prices P
                            ON L.id = P.Listing_id
                            WHERE category = 2361707
                            AND denomination = 'USD'
                            AND units = 'mg'")
mdma$price = as.numeric(mdma$price)
mdma$price.normalized = mdma$price / (mdma$mult * mdma$amount)
mdma$timestamp = mdma$date
rm(mdma$date)
mdma$day = as.Date(as.POSIXct(mdma$timestamp, origin = "1970-01-01"))
mdma$week = format(mdma$day, "%Y-%U")
# sort by date
mdma = mdma[order(mdma$timestamp),]

# create data table
mdma.dt = data.table(mdma)
prices.by.day = data.frame(mdma.dt[,list(median = median(price.normalized)), by = day])
prices.by.week = data.frame(mdma.dt[,list(median = median(price.normalized)), by = week])

# plot weekly prices
plot(prices.by.week$median, type = "l",  xaxt = 'n', xlab = "Week", ylab = "Price in Dollars per 100mg")
axis(side = 1, at = seq(from = 1, to = length(prices.by.week$week), length.out = 10),
             labels = prices.by.week$week[seq(from = 1, to = length(prices.by.week$week), length.out = 10)])

