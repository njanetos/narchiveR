#' Constructs a price index by binning price data.
#'
#' @param market The name of the marketplace to draw data from.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param binsize The size of the bins used to compute median prices.
#' @param scale Optional scaling factor.
#' @param percentile The percentile to use, e.g., 0.5 returns the median price.
#' @examples
#' construct.index(market = "agora",
#'                 category = "2361707",
#'                 units = "mg",
#'                 additionalQuery = "amount > 50")
construct.index = function(market = "agora",
                           category = '2361707',
                           units = NULL,
                           additionalQuery = "",
                           binsize = 1000,
                           scale = 1,
                           percentile = 0.5) {
    select.database(paste("drugs_", market, sep = ""))
    
    query = sprintf(
        "SELECT * FROM Listing L
        INNER JOIN Listing_prices P
        ON L.id = P.Listing_id
        WHERE category = %s
        AND units = '%s'", category, units
    )
    
    if (additionalQuery != "") {
        query = paste(query, " AND ", additionalQuery, ";")
    }
    
    prices <- get.query(query)
    prices$normalized = scale * as.numeric(prices$price) / (prices$mult * prices$amount)
    
    # Throw away prices which are 2 orders of magnitude away from the median.
    med.price = median(prices$normalized);
    prices = prices[prices$normalized > med.price * 0.01 &
                        prices$normalized < med.price * 100,]
    
    prices$day = as.Date(as.POSIXct(prices$date, origin = "1970-01-01"))
    
    # sort by date
    prices = prices[order(prices$date),]
    
    # group the data with binsize k
    n = length(prices$price)
    prices$bin = rep(1:ceiling(n / binsize), each = binsize)[1:n]
    
    # create data table
    prices.dt = data.table(prices)
    
    # create prices by bin
    prices.by.bin = data.frame(c(prices.dt[,list(percentile = quantile(normalized, percentile), 
                                                    first.quartile = quantile(normalized, 0.25),
                                                    second.quartile = quantile(normalized, 0.5),
                                                    third.quartile = quantile(normalized, 0.75)), by = bin],
                                 prices.dt[,list(bin.day = min(day)), by = bin]))
    
    return(prices.by.bin)
    
}

#' Plots price indices.
#'
#' @param market The name of the marketplace to draw data from, if a vector,
#'   then plots multiple lines.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param binsize The size of the bins used to compute median prices.
#' @param dateRange The dates which form the endpoints of the bins. If a single
#'   number, then the full date range is used, and evenly split according to the
#'   number.
#' @param scale Optional scaling factor.
#' @param percentile Defaults to 0.5, the median.
#' @param type The plot type, defaults to lines.
#' @param quantiles Whether to plot 25\% and 75\% quantiles. 
#' @examples
#' plot.index(market = c("agora", "evolution"),
#'                 category = "2361707",
#'                 units = "mg",
#'                 additionalQuery = "amount > 50")
plot.index = function(market = "agora",
                      category = '2361707',
                      units = NULL,
                      additionalQuery = "",
                      binsize = 1000,
                      dateRange = NULL,
                      scale = 1,
                      percentile = 0.5,
                      type = 'l',
                      quantiles = TRUE) {
    all.indices = list();
    
    # Fetch all the binned data
    for (i in 1:length(market)) {
        all.indices[[length(all.indices) + 1]] <-
            construct.index(
                market = market[i],
                category = category,
                units = units,
                additionalQuery = additionalQuery,
                binsize = binsize,
                scale = scale,
                percentile = percentile
            );
    }
    
    # Find min and max values
    priceRange = c(Inf,-Inf)
    for (i in 1:length(market)) {
        if (priceRange[1] > min(all.indices[[i]]$first.quartile))
            priceRange[1] = min(all.indices[[i]]$first.quartile)
        if (priceRange[2] < max(all.indices[[i]]$third.quartile))
            priceRange[2] = max(all.indices[[i]]$third.quartile)
    }
    
    plot(
        as.Date(all.indices[[1]]$bin.day),
        all.indices[[1]]$percentile,
        type = type,
        lwd = 4,
        ylim = priceRange,
        xlab = "Date",
        ylab = paste(c("$ / ", scale, " ", units), collapse = ""),
        col = 2
    )
    
    
    if (quantiles) {
        lines(as.Date(all.indices[[1]]$bin.day),
              all.indices[[1]]$first.quartile,
              type = type,
              lwd = 2,
              lty = 2,
              col = 2)
        
        lines(as.Date(all.indices[[1]]$bin.day),
              all.indices[[1]]$third.quartile,
              type = type,
              lwd = 2,
              lty = 2,
              col = 2)
    }
    
    if (length(all.indices) > 1) {
        for (i in 2:length(all.indices)) {
            lines(
                as.Date(all.indices[[i]]$bin.day),
                all.indices[[i]]$percentile,
                type = type,
                lwd = 4,
                col = i + 1
            );
            
            if (quantiles) {
                lines(as.Date(all.indices[[i]]$bin.day),
                      all.indices[[i]]$first.quartile,
                      type = type,
                      lwd = 2,
                      lty = 2,
                      col = i + 1)
                
                lines(as.Date(all.indices[[i]]$bin.day),
                      all.indices[[i]]$third.quartile,
                      type = type,
                      lwd = 2,
                      lty = 2,
                      col = i + 1)
            }
        }
    }
    
    legend(
        "topright",
        bty = "n",
        legend = market,
        col = 2:(length(market) + 1),
        lwd = 3,
        cex = 1,
        xpd = TRUE,
        ncol = 1
    );
}

get.code = function(id) {
    switch(id, 
            mdma = {return('2361707')},
            heroin = {return('1658792374')},
            cocaine = {return('2127542943')},
            cannabis = {return('669483177')},
            lsd = {return('75677')},
            amphetamine = {return('-1443047135')},
            ketamine = {return('1142244890')}
        )
    
    stop("Unrecognized category.")
}