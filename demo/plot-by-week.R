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
mdma$price = 100 * as.numeric(mdma$price)
mdma$price.normalized = mdma$price / (mdma$mult * mdma$amount)
mdma$timestamp = mdma$date
mdma$day = as.Date(as.POSIXct(mdma$timestamp, origin = "1970-01-01"))
mdma$week = format(mdma$day, "%Y-%U")
# sort by date
mdma = mdma[order(mdma$timestamp),]

# group the data with binsize k
k = 1000
n = length(mdma$price)
mdma$bin = rep(1:ceiling(n/k), each = k)[1:n]

# create data table
mdma.dt = data.table(mdma)

# create prices by week
prices.by.week = data.frame(c(mdma.dt[,list(median = median(price.normalized)), by = week],
                              mdma.dt[,list(week.day = min(day)), by = week]))

# create prices by bin
prices.by.bin = data.frame(c(mdma.dt[,list(median = median(price.normalized)), by = bin],
                             mdma.dt[,list(bin.day = min(day)), by = bin]))

# plot prices by bin
plot(as.Date(prices.by.bin$bin.day), prices.by.bin$median, type = "l",  xlab = "Date",
     ylab = "Price in Dollars per 100mg", ylim = c(1.5,5))

# plot weekly prices
plot(as.Date(prices.by.week$week.day), prices.by.week$median, type = "l",  xlab = "Date",
     ylab = "Price in Dollars per 100mg", ylim = c(1.5,5))

