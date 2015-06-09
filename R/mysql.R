#' Lists all available databases.
show.databases = function() {
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    databases.list = dbGetQuery(mysql.connection, 'show databases;')$Database
    return(databases.list[grep("drugs_", databases.list)])
    
}

#' Opens a connection to the SQL server, and optionally connects to the
#' database.
#' 
#' @param dbname The name of the database. A complete list of names can be found
#'   using show.databases after running connect.database().
#' @examples
#' connect.database("drugs_agora")
connect.database = function(dbname = NULL) {
    if (!exists("mysql.connection")) {
        mysql.connection = dbConnect(
            RMySQL::MySQL(), username = "R",
            password = "R",
            host = "njanetos.econ.upenn.edu"
        )
        
        assign("mysql.connection", mysql.connection, envir = .GlobalEnv)
    }
    
    if (!is.null(dbname)) {
        select.database(dbname)
    }
    
}

#' Connects to the database.
#' 
#' @param dbname The name of the database. A complete list of names can be found
#'   using show.databases after running connect.database().
#' @examples
#' select.database("drugs_agora")
select.database = function(dbname) {
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    if (any(dbname == show.databases())) {
        dbGetQuery(mysql.connection, sprintf("use %s", dbname))
        return(TRUE)
    } else {
        stop(sprintf("database '%s' does not exist", dbname))
    }
    
}

#' Disconnects from the SQL server.
disconnect.database = function() {
    if (!exists("mysql.connection")) {
        return(TRUE)
    }
    
    dbDisconnect(mysql.connection)
    rm(mysql.connection, envir = .GlobalEnv)
    
}

#' Returns the results of a SQL query. By default, performs an inner join on
#' listings and prices and returns everything.
#' 
#' @param query The SQL query to run.
#' @examples
#' get.query("SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id")
get.query = function(query = "SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id") {
    
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    if (is.na(get.selected.database())) {
        throw("Not using to a database. Call select.database() before running queries.");
    }
    
    rs <- dbSendQuery(mysql.connection, query);
    result <- dbFetch(rs, n = -1);
    dbClearResult(rs);
    
    return(result);
    
}

#' Returns the database currently connected to.
get.selected.database = function() {
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    return(as.character(dbGetQuery(
        mysql.connection, "select database();"
    )))
    
}