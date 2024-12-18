package ci.devops.aldanangel.tools

def build(Map _cfg) {
    sh("sam build --no-cached --template ${_cfg.template}")
}

def publish(Map _cfg) {
    sh("sam package --debug --s3-bucket '${_cfg.bucket}' --region '${_cfg.region}' --s3-prefix '${_cfg.appName}/${BUILD_NUMBER}' --output-template-file ${_cfg.file}.yaml")
}
