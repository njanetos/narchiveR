# create new package environment pkg.env with variable mysql.connection
pkg.env = new.env()
assign("mysql.connection", NULL, envir = pkg.env)

#' Opens a connection to the SQL server, and optionally connects to the
#' database.
#'
#' @param dbname The name of the database. A complete list of names can be found
#'   using show.databases after running connect.database().
#' @examples
#' connect.database("drugs_agora")
connect.database = function(dbname = NULL) {
    if (is.null(get.connection())) {
        mysql.connection = dbConnect(
            RMySQL::MySQL(), username = "R",
            password = "yaQYhnRsypwSDKvL",
            host = "narchiverdatabase.cswralfao6vc.us-west-1.rds.amazonaws.com"
        )
        
        # store the connection in the package environment
        assign("mysql.connection", mysql.connection, envir = pkg.env)
    }
    
    if (!is.null(dbname)) {
        select.database(dbname)
    }
    
}

#' Disconnects from the SQL server.
disconnect.database = function() {
    if (is.null(get.connection())) {
        return(TRUE)
    }
    mysql.connection = get.connection()
    
    dbDisconnect(mysql.connection)
    assign("mysql.connection", NULL, envir = pkg.env)
    
}

#' Get database connection from the package environment
get.connection = function() {
    mysql.connection = get("mysql.connection", envir = pkg.env)
    return(mysql.connection)
}

#' Connects to the database.
#'
#' @param dbname The name of the database. A complete list of names can be found
#'   using show.databases after running connect.database().
#' @examples
#' select.database("drugs_agora")
select.database = function(dbname) {
    if (is.null(get.connection())) {
        connect.database()
    }
    mysql.connection = get.connection()
    
    if (any(dbname == show.databases())) {
        dbGetQuery(mysql.connection, sprintf("use %s", dbname))
        return(TRUE)
    } else {
        stop(sprintf("database '%s' does not exist", dbname))
    }
    
}

#' Lists all available databases.
show.databases = function() {
    if (is.null(get.connection())) {
        connect.database()
    }
    mysql.connection = get.connection()
    
    databases.list = dbGetQuery(mysql.connection, 'show databases;')$Database
    return(databases.list[grep("drugs_", databases.list)])
    
}

#' Returns the date that scraping began and ended for this marketplace.
get.date.range = function() {
    if (is.null(get.connection())) {
        connect.database()
    }
    mysql.connection = get.connection()
    
    if (is.na(get.selected.database())) {
        stop("Not using a database. Call select.database() before running queries.");
    }
    
    rs <-
        dbSendQuery(mysql.connection, "SELECT MIN(date) FROM Listing_prices");
    resultMin <- dbFetch(rs, n = -1)$"MIN(date)";
    dbClearResult(rs);
    
    rs <-
        dbSendQuery(mysql.connection, "SELECT MAX(date) FROM Listing_prices");
    resultMax <- dbFetch(rs, n = -1)$"MAX(date)";
    dbClearResult(rs);
    
    return(c(resultMin, resultMax));
}

#' Returns the results of a SQL query. By default, performs an inner join on
#' listings and prices and returns the first 10 results.
#'
#' @param query The SQL query to run.
#' @examples
#' select.database("drugs_agora")
#' get.query("SELECT * FROM Listing L
#'                  INNER JOIN Listing_prices P
#'                  ON L.id = P.Listing_id
#'                  WHERE category = '2361707'
#'                  AND denomination = 'USD'
#'                  AND units = 'mg'")
get.query = function(query = "SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id LIMIT 10") {
    if (is.null(get.connection())) {
        connect.database()
    }
    mysql.connection = get.connection()
    
    if (is.na(get.selected.database())) {
        stop("Not using a database. Call select.database() before running queries.");
    }
    
    rs <- dbSendQuery(mysql.connection, query);
    result <- dbFetch(rs, n = -1);
    dbClearResult(rs);
    
    return(result);
    
}

#' Returns the database currently connected to.
get.selected.database = function() {
    if (is.null(get.connection())) {
        connect.database()
    }
    mysql.connection = get.connection()
    
    return(as.character(dbGetQuery(
        mysql.connection, "select database();"
    )))
    
}

#' Downloads from the STRIDE database. Currently supports 
download.stride = function(category = "cocaine") {
    base.url = "http://www.dea.gov/resource-center/stride_";
    
    download.file(
        url = paste(c(base.url, category, ".xls"), collapse = ""), destfile = "temp", method = "auto"
    )
    
    res = read.xls("temp")
    fin.res = data.frame(category = integer(length(res$Drug)))
    fin.res$category = res$Drug
    fin.res$state = res$State
    fin.res$country =- res$Country
    fin.res$potency = res$Potency
    fin.res$weight = res$Nt.Wt
    
    return(fin.res)
}