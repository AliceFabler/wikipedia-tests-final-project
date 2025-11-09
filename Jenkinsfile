/*
 * Jenkinsfile — wikipedia-tests-final-project (safe)
 * Минимальный и рабочий: без tools{jdk} и без ansiColor.
 * Требует только Git + Gradle wrapper на агенте.
 */
pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
		timeout(time: 60, unit: 'MINUTES')
	}

	parameters {
		string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch')
		choice(name: 'DEVICE_HOST', choices: ['local', 'remote'], description: 'Запуск: local (Appium) | remote (BrowserStack и т.п.)')
		choice(name: 'GRADLE_TASK', choices: ['test', 'clean test'], description: 'Gradle task')
		string(name: 'EXTRA_OPTS', defaultValue: '', description: 'Доп. -D свойства: -Denv=local -Dappium.url=http://127.0.0.1:4723 ...')
		booleanParam(name: 'UPLOAD_TO_TESTOPS', defaultValue: true, description: 'Грузить результаты в Allure TestOps через allurectl')
		string(name: 'ALLURE_ENDPOINT', defaultValue: 'https://allure.autotests.cloud', description: 'Allure TestOps endpoint')
		string(name: 'ALLURE_PROJECT_ID', defaultValue: '', description: 'ID проекта в TestOps (если пусто — upload пропускается)')
	}

	environment {
		GRADLE_USER_HOME    = "${WORKSPACE}/.gradle"
		ALLURE_RESULTS_PATH = ".allure-results"
		GIT_URL             = "https://github.com/AliceFabler/wikipedia-tests-final-project.git"
	}

	stages {
		stage('Checkout') {
			steps {
				git branch: "${params.BRANCH}", url: "${env.GIT_URL}"
			}
		}

		stage('Prepare') {
			steps {
				sh 'java -version || true'
				sh 'chmod +x gradlew'
				sh './gradlew --version'
			}
		}

		stage('Test') {
			steps {
				catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
					sh """
            ./gradlew ${params.GRADLE_TASK} \
              -DdeviceHost=${params.DEVICE_HOST} \
              ${params.EXTRA_OPTS} \
              --stacktrace --no-daemon --info
          """
				}
			}
			post {
				always {
					archiveArtifacts artifacts: "${ALLURE_RESULTS_PATH}/**", fingerprint: true, allowEmptyArchive: true
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
							echo "Allure Jenkins plugin не настроен — пропускаю публикацию отчёта в Jenkins."
						}
					}
				}
			}
		}

		stage('Upload to Allure TestOps') {
			when {
				allOf {
					expression { return params.UPLOAD_TO_TESTOPS }
					expression { return params.ALLURE_PROJECT_ID?.trim() }
				}
			}
			steps {
				withEnv(["ALLURE_ENDPOINT=${params.ALLURE_ENDPOINT}"]) {
					withCredentials([string(credentialsId: 'ALLURE_TOKEN', variable: 'ALLURE_TOKEN')]) {
						sh '''
              echo "Upload to Allure TestOps: ${ALLURE_ENDPOINT}, project ${ALLURE_PROJECT_ID}"
              curl -sL https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64 -o allurectl
              chmod +x allurectl

              ./allurectl --endpoint ${ALLURE_ENDPOINT} --token ${ALLURE_TOKEN} launch create \
                --project-id ${ALLURE_PROJECT_ID} \
                --launch-name "Jenkins #${BUILD_NUMBER} (${BRANCH_NAME}) - ${DEVICE_HOST}" \
                --hidden=false || true

              ./allurectl --endpoint ${ALLURE_ENDPOINT} --token ${ALLURE_TOKEN} results upload \
                --project-id ${ALLURE_PROJECT_ID} \
                --launch-name "Jenkins #${BUILD_NUMBER} (${BRANCH_NAME}) - ${DEVICE_HOST}" \
                --results "**/${ALLURE_RESULTS_PATH}" || true

              ./allurectl --endpoint ${ALLURE_ENDPOINT} --token ${ALLURE_TOKEN} launch close \
                --project-id ${ALLURE_PROJECT_ID} \
                --launch-name "Jenkins #${BUILD_NUMBER} (${BRANCH_NAME}) - ${DEVICE_HOST}" \
                --status passed || true
            '''
					}
				}
			}
		}
	}

	post {
		always {
			echo "Готово. В артефактах — .allure-results и JUnit XML; Allure в Jenkins — если плагин установлен; TestOps — если включен upload."
		}
	}
}
