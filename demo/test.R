require("RMySQL")

# connect to agora
con = connect("drugs_agora")

# get listings
rs <- dbSendQuery(con, "SELECT * FROM Listing_prices")
d1 <- dbFetch(rs, n = 10)
# clear result
dbClearResult(rs)
print(d1)

# disconnect
disconnect(con)
