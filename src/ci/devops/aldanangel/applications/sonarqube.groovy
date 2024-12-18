package ci.devops.aldanangel.applications

def analysisWithQualityGates(Map _cfg) {
    //withSonarQubeEnv("${_cfg.server_id}") {
    sh("sonar-scanner -Dsonar.projectKey='${_cfg.appName}' -Dsonar.projectName='${_cfg.appName}' -Dsonar.projectVersion='${_cfg.version}' -Dsonar.host.url='${_cfg.host_url}' -Dsonar.login='${_cfg.token}'")
    //}

    timeout(time: 5, unit: 'MINUTES') {
        _qualityGates = waitForQualityGate()

        if (_qualityGates.status != 'OK') {
            error 'Pipeline aborted due quality gate failure.'
        }
    }
}

def analysisWithoutQualityGates(Map _cfg) {
    sh("sonar-scanner -Dsonar.projectKey='${_cfg.appName}' -Dsonar.projectName='${_cfg.appName}' -Dsonar.projectVersion='${_cfg.version}' -Dsonar.host.url='${_cfg.host_url}' -Dsonar.login='${_cfg.token}'")
}
