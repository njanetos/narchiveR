.onLoad = function(libname, pkgname) {

    packageStartupMessage("connecting to database ...")
    connect.database()
    
}
