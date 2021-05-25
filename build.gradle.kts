
plugins {
    id ("com.github.rodm.teamcity-environments") version "1.4-beta-2"
}

teamcity {
    environments {
        register("teamcity2021.1") {
            version = "2021.1"
        }
    }
}
