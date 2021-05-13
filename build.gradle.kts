
plugins {
    id ("com.github.rodm.teamcity-environments") version "1.4-beta-2"
}

teamcity {
    environments {
        register("teamcity2020.2") {
            version = "2020.2.4"
        }
    }
}
