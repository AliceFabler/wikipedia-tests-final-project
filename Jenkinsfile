// Jenkinsfile — minimal (JDK 17, Gradle 8.10 via wrapper)
pipeline {
	agent any

	tools {
		// Manage Jenkins → Global Tool Configuration → JDK → name: jdk17
		jdk 'jdk17'
	}

	options {
		timestamps()
		ansiColor('xterm')
	}

	parameters {
		choice(name: 'DEVICE_HOST', choices: ['local', 'remote'], description: 'local (Appium) или remote (BrowserStack)')
		choice(name: 'GRADLE_TASK', choices: ['test', 'clean test'], description: 'Gradle task')
		string(name: 'EXTRA_OPTS', defaultValue: '', description: 'Доп. -D: напр. -Denv=local -Dappium.url=http://127.0.0.1:4723')
	}

	environment {
		ALLURE_RESULTS_PATH = '.allure-results' // корень проекта
	}

	stages {
		stage('Checkout') {
			steps { checkout scm }
		}

		stage('Build & Test') {
			steps {
				sh 'java -version || true'
				sh 'chmod +x gradlew'
				sh './gradlew --version'  // должен показать Gradle 8.10

				catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
					sh """
            ./gradlew ${params.GRADLE_TASK} \
              -DdeviceHost=${params.DEVICE_HOST} \
              ${params.EXTRA_OPTS} \
              --no-daemon --stacktrace
          """
				}
			}
			post {
				always {
					archiveArtifacts artifacts: "${ALLURE_RESULTS_PATH}/**", allowEmptyArchive: true, fingerprint: true
					junit testResults: "**/build/test-results/test/*.xml", allowEmptyResults: true

					script {
						try {
							allure([
								includeProperties: false,
								jdk: '',
								results: [[path: "${ALLURE_RESULTS_PATH}"]],
								reportBuildPolicy: 'ALWAYS'
							])
						} catch (ignored) {
							echo 'Allure Jenkins plugin не найден — отчёт пропущен.'
						}
					}
				}
			}
		}
	}

	post {
		always {
			echo 'Готово: .allure-results и JUnit заархивированы. Если плагин есть — Allure отчёт построен.'
		}
	}
}
