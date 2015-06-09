.onLoad = function(libname, pkgname) {

    print("connecting to database ...")
    mysql.connection = connect.database()

}
