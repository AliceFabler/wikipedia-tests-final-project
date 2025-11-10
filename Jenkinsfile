pipeline {
  agent any

  options {
    ansiColor('xterm')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 60, unit: 'MINUTES')
  }

  parameters {
    choice(name: 'RUN_TYPE', choices: ['ui', 'api', 'manual'], description: 'Что запускать: мобильные UI тесты, API тесты или ручной отчёт')
    choice(name: 'DEVICE_HOST', choices: ['local', 'remote'], description: 'Где гонять UI: локально (Appium 3) или BrowserStack')
    string(name: 'APPIUM_URL', defaultValue: 'http://127.0.0.1:4723', description: 'URL локального Appium (для DEVICE_HOST=local)')
    string(name: 'APP', defaultValue: '', description: 'Путь/URL к APK/APP (если пусто — берётся из properties)')
    string(name: 'BS_BUILD', defaultValue: 'Wikipedia UI Autotests', description: 'Имя билда в BrowserStack')
    string(name: 'BS_DEVICE', defaultValue: 'Google Pixel 7', description: 'Девайс в BrowserStack')
    string(name: 'BS_OS_VERSION', defaultValue: '13.0', description: 'OS version в BrowserStack')
    string(name: 'GRADLE_ARGS', defaultValue: '--no-daemon --stacktrace --info', description: 'Доп. флаги Gradle')
  }

  environment {
    JAVA_HOME = tool(name: 'JDK17', type: 'jdk')
    PATH = "${JAVA_HOME}/bin:${PATH}"
    BS_USER = credentials('bs_user')
    BS_KEY  = credentials('bs_key')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Env') {
      steps {
        dir('wikipedia-tests-final-project') {
          sh 'java -version || true'
          sh './gradlew --version || true'
        }
      }
    }

    stage('UI tests') {
      when { expression { params.RUN_TYPE == 'ui' } }
      steps {
        dir('wikipedia-tests-final-project') {
          sh """
            ./gradlew clean test \\
              --tests "guru.qa.ui.tests.*" \\
              -DdeviceHost=${params.DEVICE_HOST} \\
              -Dappium.url=${params.APPIUM_URL} \\
              -Dapp=${params.APP} \\
              -Dbstack.user=${BS_USER} -Dbstack.key=${BS_KEY} \\
              -Dbstack.buildName="${params.BS_BUILD}" \\
              -Dbstack.device="${params.BS_DEVICE}" \\
              -Dbstack.osVersion="${params.BS_OS_VERSION}" \\
              -Dallure.results.directory=.allure-results \\
              ${params.GRADLE_ARGS}
          """
        }
      }
      post {
        always {
          dir('wikipedia-tests-final-project') {
            archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
            allure includeProperties: false, jdk: '', results: [[path: '.allure-results']]
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
          }
        }
      }
    }

    stage('API tests') {
      when { expression { params.RUN_TYPE == 'api' } }
      steps {
        dir('wikipedia-tests-final-project') {
          sh """
            ./gradlew clean test \\
              --tests "guru.qa.api.tests.*" \\
              -Dallure.results.directory=.allure-results \\
              ${params.GRADLE_ARGS}
          """
        }
      }
      post {
        always {
          dir('wikipedia-tests-final-project') {
            archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
            allure includeProperties: false, jdk: '', results: [[path: '.allure-results']]
            junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
          }
        }
      }
    }

    stage('Manual (attach report)') {
      when { expression { params.RUN_TYPE == 'manual' } }
      steps {
        dir('wikipedia-tests-final-project') {
          sh 'mkdir -p .allure-results && echo "{\"manual\":true}" > .allure-results/executor.json'
        }
      }
      post {
        always {
          dir('wikipedia-tests-final-project') {
            archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
            allure includeProperties: false, jdk: '', results: [[path: '.allure-results']]
          }
        }
      }
    }
  }
}
