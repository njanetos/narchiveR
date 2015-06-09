construct.index = function(market = "agora", cutNum = 20, category = '2361707', units = NULL, additionalQuery="") {
    select.database(paste("drugs_", market, sep = ""))
    
    if (is.na(units)) {
        stop("Required field units missing.");
    }
    
    
    
    query = sprintf("SELECT * FROM Listing L
                        INNER JOIN Listing_prices P
                        ON L.id = P.Listing_id
                        WHERE category = %s
                        AND denomination = 'USD'
                        AND units = '%s'", category, units)
    
    if (additionalQuery!="") {
        query = paste(query, " AND ", additionalQuery, ";");
    }

    price <- get.query(query)

    price$normalized = as.numeric(price$price) / (price$mult * price$amount)

    cutY = price$normalized
    cutX = price$date
    cuts = tapply(cutY, cut(cutX, cutNum), median)
    
    return(cuts)

}

plot.index = function(market = "agora", cutNum = 20, category = '2361707', units = NULL, additionalQuery="") {
    allcuts = list();
    
    for (i in 1:length(market)) {
        allcuts[[length(allcuts)+1]] <- construct.index(market = market[i], cutNum = cutNum, category = category, units = units, additionalQuery="");
    }
    
    # Find max/min dates
    stDate = -Inf;
    enDate = Inf;
    for (i in 1:length(market)) {
        dates = get.date.range();
    }

}

