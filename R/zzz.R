.onLoad = function(libname, pkgname) {

    packageStartupMessage("connecting to database ...")
    mysql.connection = connect.database()

}
