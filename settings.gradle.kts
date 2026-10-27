pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Game"
include(":app")
include(":core")
include(":games:trainsurvivor")
include(":games:battleship")
include(":games:dreadnought")
include(":games:basedefender")
include(":games:keyreader")
include(":games:memorymatch")
include(":games:snake")
include(":games:snakeladder")
include(":games:roadrush")
include(":games:arcshot")
include(":games:minesweeper")
include(":games:tunnelcrew")
include(":games:numbercruncher")
include(":games:cratecrew")
include(":games:missilecommand")
include(":games:spaceinvaders")
include(":games:breakout")
