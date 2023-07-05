pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
}
rootProject.name = "TicketManager-Extra-Databases"
include("pure-h2")
include("SQL-Parser")
include("Common")
include("Pure-MySQL")
