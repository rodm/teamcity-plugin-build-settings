
plugins {
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

teamcity {
    environments {
        register("teamcity2021.1") {
            version = "2021.1"
        }
    }
}
