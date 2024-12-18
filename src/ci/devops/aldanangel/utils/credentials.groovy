package ci.devops.aldanangel.utils

def get_secret_text(String _credential_id) {
    withCredentials([string(credentialsId: "${_credential_id}", variable: '_secret_value')]) {
        return _secret_value
    }
}
