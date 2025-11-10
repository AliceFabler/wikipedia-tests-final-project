\
/*
 * Jenkinsfile — Wikipedia Mobile tests (Java 17 nodes, creds from src/test/resources/auth.properties)
 * Runs API tests and UI (remote) tests separately or together.
 * Assumes:
 *  - Gradle Wrapper present in repo
 *  - Agent nodes use Java 17 (JAVA_HOME points to JDK 17)
 *  - Allure Jenkins plugin installed (for `allure` step)
 *  - BrowserStack credentials are stored in the repo in src/test/resources/auth.properties
 *    and resolved by Owner (@Config.Sources system->env->classpath:${env}.properties->classpath:auth.properties).
 *
 *  SECURITY NOTE: keeping real credentials in VCS is strongly discouraged. Prefer Jenkins credentials.
 */
pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '30'))
    disableConcurrentBuilds()
    timeout(time: 60, unit: 'MINUTES')
  }

  // Nodes already run Java 17; omit tools.
  // tools { jdk 'jdk-17' }

  parameters {
    choice(name: 'RUN_TARGET', choices: ['api', 'ui-remote', 'both'], description: 'What to run')
    string(name: 'GRADLE_ARGS', defaultValue: '', description: 'Extra args for Gradle (e.g., --no-daemon --stacktrace)')
    string(name: 'ALLURE_RESULTS_PATH', defaultValue: '.allure-results', description: 'Where tests write Allure results')
    // UI (remote) overrides; leave empty to use src/test/resources/*.properties
    string(name: 'APP', defaultValue: '', description: 'Remote app id or URL (e.g., bs://... or https://...)')
    string(name: 'DEVICE', defaultValue: '', description: 'Device name override (e.g., Google Pixel 7)')
    string(name: 'OS_VERSION', defaultValue: '', description: 'OS version override (e.g., 13.0)')
    string(name: 'REMOTE_URL', defaultValue: '', description: 'Override remote Appium URL (leave empty to use auth.properties)')
    string(name: 'UI_TAGS', defaultValue: 'android,remote,wikipedia', description: 'JUnit tags for UI run (comma-separated)')
    string(name: 'API_TAGS', defaultValue: 'api', description: 'JUnit tags for API run (comma-separated)')
  }

  environment {
    ALLURE_RESULTS = "${params.ALLURE_RESULTS_PATH}"
  }

  stages {
    stage('Prep') {
      steps {
        sh 'echo "Java: $(java -version 2>&1 | head -n1)"'
        sh 'echo "Gradle Wrapper: $(./gradlew -v | head -n3 | tr \"\\n\" \" | \")"; true'
        sh 'rm -rf "${ALLURE_RESULTS}" && mkdir -p "${ALLURE_RESULTS}"'
        sh 'test -f src/test/resources/auth.properties && echo "auth.properties found" || (echo "auth.properties NOT found!" && exit 1)'
      }
    }

    stage('API tests') {
      when { anyOf { expression { params.RUN_TARGET == 'api' }; expression { params.RUN_TARGET == 'both' } } }
      steps {
        sh """
          ./gradlew clean test \\
            -Ptags='${API_TAGS}' \\
            -Dallure.results.directory='${ALLURE_RESULTS}' \\
            ${GRADLE_ARGS}
        """
      }
      post {
        always {
          allure([results: [[path: "${ALLURE_RESULTS}"]]])
          archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
          junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
        }
      }
    }

    stage('UI tests (remote)') {
      when { anyOf { expression { params.RUN_TARGET == 'ui-remote' }; expression { params.RUN_TARGET == 'both' } } }
      steps {
        script {
          // Build optional -D overrides from parameters; DO NOT pass user/key — they are read from classpath:auth.properties
          def overrides = []
          if (params.APP?.trim())        { overrides << "-Dapp='${params.APP.trim()}'" }
          if (params.DEVICE?.trim())     { overrides << "-Ddevice='${params.DEVICE.trim()}'" }
          if (params.OS_VERSION?.trim()) { overrides << "-Dos_version='${params.OS_VERSION.trim()}'" }
          if (params.REMOTE_URL?.trim()) { overrides << "-DremoteUrl='${params.REMOTE_URL.trim()}'" }
          def overridesStr = overrides.join(' ')

          sh """
            ./gradlew test \\
              -Ptags='${UI_TAGS}' \\
              -DdeviceHost=remote \\
              -Denv=remote \\
              ${overridesStr} \\
              -Dallure.results.directory='${ALLURE_RESULTS}' \\
              ${GRADLE_ARGS}
          """
        }
      }
      post {
        always {
          allure([results: [[path: "${ALLURE_RESULTS}"]]])
          archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
          junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
        }
      }
    }
  }

  post {
    always {
      echo "Done. If the Allure report is empty, ensure tests actually produced results in ${env.ALLURE_RESULTS}."
    }
  }
}
