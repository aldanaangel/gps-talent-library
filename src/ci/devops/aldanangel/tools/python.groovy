package ci.devops.aldanangel.tools

def test(Map _cfg) {
    sh("pytest ${_cfg.test_directory}")
}

def test_with_coverage(Map _cfg) {
    sh("pytest ${_cfg.test_directory} --cov=hello_world --cov-report=xml --cov-report=term-missing")
}
