test_that("Construct indices", {
    result = construct.index(market = "agora", category = "2361707", units = "mg", additionalQuery = "amount > 50")
    expect_that(length(result) > 6)
})

test_that("Plot graph", {
    plot.index(market = c("agora", "evolution"), category = "2361707", units = "mg", additionalQuery = "amount > 50")
})