package src.ci.devops.aldanangel.pipelines

import ci.devops.aldanangel.applications.sonarqube
import ci.devops.aldanangel.utils.set
import ci.devops.aldanangel.utils.credentials
import ci.devops.aldanangel.tools.sam
import ci.devops.aldanangel.tools.python

def execute(Map _cfg) {
  def _mapenv = [:]
  def _sonarqube = new sonarqube()
  def _set = new set()
  def _credentials = new credentials()
  def _sam = new sam()
  def _python = new python()

  def aws_access_key = _credentials.get_secret_text(secrets.AWS_ACCESS_KEY_ID)
  def aws_secret_access_key = _credentials.get_secret_text(secrets.AWS_SECRET_ACCESS_KEY)

  properties([
    buildDiscarder(logRotator(
        daysToKeepStr: '30',   // Guardar builds de los últimos 30 días
        numToKeepStr: '10',    // Guardar las últimas 10 ejecuciones
    )),
    pipelineTriggers([githubPush()]),
])
  node('') {
    timeout(time: 30, unit: 'MINUTES') {
      try {
        stage('Preparation') {
          checkout scm
          docker.image(images.GIT).inside() {
            _mapenv = _set.map_enviroments(appName: _cfg.appName, repository: _cfg.repository,
             type: _cfg.type,template: _cfg.template, test_directory:  _cfg.test_directory, version: _cfg.version)
          }
          stage('Unit Test') {
            docker.image(images.PYTHON).inside("-v ${WORKSPACE}:/app") {
              _python.test_with_coverage(test_directory: _mapenv.test_directory)
            }
          }
          stage('SonarQube Analysis') {
            echo 'Realizando análisis de SonarQube...'
            docker.image('sonarsource/sonar-scanner-cli:11.1').inside("--network sonarqube_sonar-network -v ${WORKSPACE}:/app") {
              SONAR_TOKEN = _credentials.get_secret_text(secrets.SONAR_SCANNER_TOKEN)
              _sonarqube.analysisWithoutQualityGates(server_id: environments.SONAR_SCANNER_ID, host_url: environments.SONAR_SCANNER_URL ,token: SONAR_TOKEN, appName: _mapenv.appName, version: _mapenv.version)
            }
          }
          stage('Build Lambda with SAM') {
            docker.image(images.SAM).inside("-v ${WORKSPACE}:/app") {
              _sam.build(template: _mapenv.template)
            }
          }
          stage('Publish Lambda with SAM') { _mapenv.appName
            docker.image(images.SAM).inside("-e AWS_ACCESS_KEY_ID=${aws_access_key} -e AWS_SECRET_ACCESS_KEY=${aws_secret_access_key} -v ${WORKSPACE}:/app") {
              _sam.publish(bucket: _cfg.bucket, region: _cfg.region, appName: _cfg.appName, file: _cfg.file)
              sh """
              cd ${WORKSPACE}/.aws-sam/build/HelloWorldFunction
              zip -r HelloWorldFunction.zip .
              """
            }
          }
          stage('Upload to GitHub Packages') {
            docker.image(images.GITHUB).inside("-v ${WORKSPACE}:/workspace") {
              withCredentials([string(credentialsId: 'gh_container_token', variable: 'GITH_TOKEN')]) {
                sh """
                  echo "$GITH_TOKEN" | gh auth login --with-token
                  unset GITH_TOKEN
                  gh auth status

                  gh release create v1.0.${BUILD_NUMBER} \
                  ${WORKSPACE}/.aws-sam/build/HelloWorldFunction/HelloWorldFunction.zip \
                  -t "Lambda Artifact v1.0.${BUILD_NUMBER}" \
                  -n "ZIP file for version 1.0.${BUILD_NUMBER}"

                  gh release upload v1.0.${BUILD_NUMBER} ${WORKSPACE}/packaged.yaml
                """
              }
            }
          }
        }
      } catch (Exception e) {
            // Manejo de errores
            currentBuild.result = 'FAILURE'
            echo "La ejecución del pipeline falló: ${e.getMessage()}"
            throw e
        } finally {
          //cleanWs()
          sh'ls'
      }
    }
  }
}
