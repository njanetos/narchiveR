connect = function(dbname = "drugs_agora") {
    con <- dbConnect(RMySQL::MySQL(), dbname = dbname, username = "R",
                     password = "R", host = "njanetos.econ.upenn.edu")
    return(con)
}

disconnect = function(con) {
    dbDisconnect(con)
}