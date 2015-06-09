#' Lists all available databases.
show.databases = function() {
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    databases.list = dbGetQuery(mysql.connection, 'show databases;')$Database
    return(databases.list[grep("drugs_", databases.list)])
    
}

#' Connect to a particular database.
#'
#' @param dbname The name of the database. A complete list of names can be found using show.databases.
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

disconnect.database = function() {
    if (!exists("mysql.connection")) {
        return(TRUE)
    }
    
    dbDisconnect(mysql.connection)
    rm(mysql.connection, envir = .GlobalEnv)
    
}

get.prices.query = function(query = "SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id") {
    rs <- dbSendQuery(mysql.connection, query);
    result <- dbFetch(rs, n = -1);
    dbClearResult(rs);
    
    return(result);
    
}

get.selected.database = function() {
    if (!exists("mysql.connection")) {
        connect.database()
    }
    
    dat = as.character(dbGetQuery(mysql.connection, "select database();"));
    
    return(dat)
    
}