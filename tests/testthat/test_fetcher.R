test_that("Check we can connect to database", {
    disconnect.database()
    expect_true(is.null(get.connection()))
    expect_true(disconnect.database())
    connect.database(dbname = 'drugs_agora')    
    expect_false(is.null(get.connection()))
})

test_that("Check if we can select a database", {
    disconnect.database()
    databases.list = show.databases()
    select.database(databases.list[1])
    expect_equal(databases.list[1], get.selected.database())
    expect_error(select.database('drugs_hotdogs'))
})

test_that("Check if we can run a query", {
    databases.list = show.databases()
    disconnect.database()
    expect_error(get.query(query = "SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id LIMIT 10"))
    select.database(databases.list[1])
    listings = get.query(query = "SELECT * FROM Listing L INNER JOIN Listing_prices P ON L.id = P.Listing_id LIMIT 10")
    expect_equal(10, NROW(listings))
    disconnect.database()
    expect_error(get.date.range())
    select.database('drugs_agora')
    expect_true(length(get.date.range()) == 2)
    disconnect.database()
    get.selected.database()
})

test_that("Make sure we don't have write access to any of the database", {
    databases.list = show.databases()
    for(database.name in databases.list) {
        select.database(database.name)
        listings1 = get.query(query = "SELECT * FROM Listing L LIMIT 1")    
        mysql.connection = get.connection()
        expect_error(dbSendQuery(mysql.connection, sprintf("DELETE FROM Listing WHERE id=%.0d", listings1$id)),
                     "* DELETE command denied *")
    }
})