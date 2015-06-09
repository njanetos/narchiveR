require("RMySQL")

# connect to agora
con = connect()

# show databases
show.databases(con)

# select database
select.database(con, "drugs_all")

# get listings from selected database
rs <- dbSendQuery(con, "SELECT * FROM Listing_prices")
d1 <- dbFetch(rs, n = 10)
# clear results
dbClearResult(rs)
print(d1)

# select another database
select.database(con, "drugs_evolution")

# get listings from selected database
rs <- dbSendQuery(con, "SELECT * FROM Listing_prices")
d2 <- dbFetch(rs, n = 10)
# clear results
dbClearResult(rs)
print(d2)

# disconnect
disconnect(con)
