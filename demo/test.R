library("narchiveR")

# show databases
show.databases()

# select database
select.database("drugs_evolution")

# get listings from selected database
rs <- dbSendQuery(get.connection(), "SELECT * FROM Listing_prices")
d1 <- dbFetch(rs, n = 10)
# clear results
dbClearResult(rs)
print(d1)

# disconnect
disconnect.database()
