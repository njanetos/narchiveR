.onLoad = function(libname, pkgname) {

    print("Connecting to database ...")
    mysql.connection = connect.database()

}
