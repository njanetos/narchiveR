connect = function(dbname = NULL) {
    con <- dbConnect(RMySQL::MySQL(), username = "R", dbname = dbname,
                     password = "R", host = "njanetos.econ.upenn.edu")
    return(con)
}

show.databases = function(con) {
    databases.list = dbGetQuery(con, 'show databases;')$Database
    return(databases.list[grep("drugs_", databases.list)])
}

select.database = function(con, dbname) {
    return(dbGetQuery(con, sprintf("use %s", dbname)))
}

disconnect = function(con) {
    return(dbDisconnect(con))
}