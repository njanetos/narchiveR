show.databases = function() {

    if (!exists("mysql.connection")) {
        connect.database()
    }

    databases.list = dbGetQuery(mysql.connection, 'show databases;')$Database
    return(databases.list[grep("drugs_", databases.list)])

}

connect.database = function(dbname = NULL) {

    dbConnect(RMySQL::MySQL(), username = "R",
              password = "R",
              dbname = dbname,
              host = "njanetos.econ.upenn.edu")

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
