pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    timeout(time: 45, unit: 'MINUTES')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build + Test') {
      steps {
        sh '''
          set -eux
          if command -v mvn >/dev/null 2>&1; then
            MVN=mvn
          elif [ -x "./mvnw" ]; then
            MVN=./mvnw
          else
            echo "Maven not found on agent (install mvn or add mvnw)" >&2
            exit 1
          fi
          $MVN -B -ntp -pl bgssai-media-common,bgssai-media-user,bgssai-media-admin -am clean test
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }
  }
}
