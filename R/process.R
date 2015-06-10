#' Constructs a price index by binning price data.
#'
#' @param market The name of the marketplace to draw data from.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param dates The dates which form the endpoints of the bins. If a single
#'   number, then the full date range is used, and evenly split according to the
#'   number.
#' @param scale Optional scaling factor.
#' @examples
#' construct.index(market = "agora",
#'                 category = "2361707",
#'                 units = "mg",
#'                 additionalQuery = "amount > 50",
#'                 dates = 20)
construct.index = function(market = "agora",
                           category = '2361707',
                           units = NULL,
                           additionalQuery = "",
                           dates = NULL,
                           scale = 1) {
    select.database(paste("drugs_", market, sep = ""))
    
    require("Quandl");
    
    if (is.na(units)) {
        stop("Required field 'units' missing.");
    }
    
    if (is.na(dates)) {
        stop("Required field 'dates' missing");
    }
    
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
    
    price <- get.query(query)

    price$normalized = as.numeric(price$price) / (price$mult * price$amount)
    
    # Convert BTC to USD, if necessary
    btcData = Quandl("BITCOIN/BTC24USD");
    
    btcData$unixDate = as.numeric(as.POSIXct(btcData$Date, format="%Y-%m-%d"));
    
    for (i in 1:length(price$normalized)) {
        if (price$denomination[i] == "BTC") {
            conv = which.min(abs(btcData$unixDate - price$normalized[i]))
            price$normalized = price$normalized * btcData$"Weighted Price"[conv];
        }
    }
    
    cuts = scale*tapply(price$normalized, cut(price$date, dates), median)
    
    return(cuts)
    
}

#' Plots price indices.
#'
#' @param market The name of the marketplace to draw data from, if a vector,
#'   then plots multiple lines.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param dateRange The dates which form the endpoints of the bins. If a single
#'   number, then the full date range is used, and evenly split according to the
#'   number.
#' @param scale Optional scaling factor.
#' @examples
#' plot.index(market = "agora",
#'                 category = "2361707",
#'                 units = "mg",
#'                 additionalQuery = "amount > 50",
#'                 dates = 20)
plot.index = function(market = "agora",
                      category = '2361707',
                      units = NULL,
                      additionalQuery = "",
                      dateRange = 20,
                      scale = 1) {
    allcuts = list();
    
    # If no date range is provided, find the minimum and maximum dates and bin the data.
    if (length(dateRange) == 1) {
        tempRange = c(Inf,-Inf);
        for (i in 1:length(market)) {
            select.database(paste("drugs_", market[i], sep = ""));
            dates = get.date.range();
            if (dates[1] < tempRange[1])
                tempRange[1] = dates[1];
            if (dates[2] > tempRange[2])
                tempRange[2] = dates[2];
        }
        
        dateRange = seq(tempRange[1], tempRange[2], length = dateRange);
    }
    
    # Fetch all the binned data
    for (i in 1:length(market)) {
        allcuts[[length(allcuts) + 1]] <-
            construct.index(
                market = market[i],
                category = category,
                units = units,
                additionalQuery = "",
                dates = dateRange,
                scale = scale
            );
    }

    col = c('#397ab3', '#d9863a', '#7e468b');
    
    plot(
        dateRange[1:(length(dateRange)-1)],
        allcuts[[1]],
        type = 'l',
        ylab = "",
        xlab = "",
        xlim = c(min(dateRange), max(dateRange)),
        lwd = 4,
        axes = F,
        col = col[1]
    );
    
    for (i in 2:length(allcuts)) {
        lines(dateRange[1:(length(dateRange)-1)],
              allcuts[[i]],
              type = 'l',
              lwd = 4,
              col = col[i])
    }
    
    axis(
        1,
        at = seq(min(dateRange),
                 max(dateRange),
                 length = 20),
        lab = format(as.POSIXct(seq(
            min(dates),
            max(dates),
            length = 20
        ),
        origin = "1970-01-01"),
        "%m-%d"),
        lwd = 2,
        mtext(1,
              text = "Date",
              line = 2.5)
    )
    
    axis(
        2,
        lwd = 2,
        las = 2,
        mtext(
            2,
            text = paste(c("$ / ", scale, " ", units), collapse = ""),
            line = 3)
    )
    
}