
plugins {
    id ("io.github.rodm.teamcity-environments") version "1.5"
}

teamcity {
    environments {
        register("teamcity2022.10") {
            version = "2022.10.3"
        }
    }
}
