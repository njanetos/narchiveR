# test_process.R
# test process.R

test_that("Construct indices", {
    result = construct.index(market = "agora", category = "2361707", units = "mg", additionalQuery = "amount > 50")
    expect_true(length(result) > 6)
})

test_that("Plot graph", {
    expect_true({plot.index(market = c("agora", "evolution"), category = "2361707", units = "mg", additionalQuery = "amount > 50"); TRUE})
})

test_that("Process errors", {
    expect_error(construct.index())
})

test_that("Category code conversions", { 
    expect_true(get.code('ketamine') == '1142244890')
    expect_error(get.code('hotdogs'))
})