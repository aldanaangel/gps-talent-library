package ci.devops.aldanangel.utils

def map_enviroments(Map _cfg) {
    def _envs = [:]

    _envs['template'] = "${_cfg.template}"
    _envs['test_directory'] = "${_cfg.test_directory}"
    _envs['appName'] = _cfg.appName
    _envs['artifactTargetPath'] = "${_cfg.repository}_${_cfg.type}"
    _envs['version'] = "${_cfg.version}"

    return _envs
}
