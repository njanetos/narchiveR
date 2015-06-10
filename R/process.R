#' Constructs a price index by binning price data.
#' 
#' @param market The name of the marketplace to draw data from.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param dates The dates which form the endpoints of the bins. If a single
#'   number, then the full date range is used, and evenly split according to the
#'   number.
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
                           dates = NULL) {
    select.database(paste("drugs_", market, sep = ""))
    
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
        AND denomination = 'USD'
        AND units = '%s'", category, units
    )
    
    if (additionalQuery != "") {
        query = paste(query, " AND ", additionalQuery, ";")
    }
    
    price <- get.query(query)
    
    price$normalized = as.numeric(price$price) / (price$mult * price$amount)
    
    cuts = tapply(price$normalized, cut(price$date, dates), median)

    return(cuts)
    
}

#' Plots price indices.
#' 
#' @param market The name of the marketplace to draw data from, if a vector,
#'   then plots multiple lines.
#' @param category The category of drug to use.
#' @param units Units the drug is measured in.
#' @param additionalQuery Any additional SQL constraints on the price data.
#' @param dates The dates which form the endpoints of the bins. If a single
#'   number, then the full date range is used, and evenly split according to the
#'   number.
#' @examples
#' plot.index(market = "agora",
#'                 category = "2361707",
#'                 units = "mg",
#'                 additionalQuery = "amount > 50",
#'                 dates = 20)
plot.index = function(market = "agora",
                      cutNum = 20,
                      category = '2361707',
                      units = NULL,
                      additionalQuery = "",
                      dateRange = NULL) {
    allcuts = list();
    
    for (i in 1:length(market)) {
        allcuts[[length(allcuts) + 1]] <-
            construct.index(
                market = market[i],
                cutNum = cutNum,
                category = category,
                units = units,
                additionalQuery = ""
            );
    }
    
    # If no date range is provided, find the minimum and maximum dates.
    if (is.null(dateRange)) {
        dateRange = c(Inf,-Inf);
        for (i in 1:length(market)) {
            dates = get.date.range();
        }
    }
    
    
}
