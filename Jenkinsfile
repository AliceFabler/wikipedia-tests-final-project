/*
 * Jenkinsfile — wikipedia-tests-final-project
 * Gradle + Java 21 + Selenide-Appium + Rest-Assured.
 * Публикует Allure-отчёт в Jenkins и (опционально) грузит в Allure TestOps.
 */
pipeline {
	agent any

	tools {
		// В Jenkins → Manage Jenkins → Global Tool Configuration
		// добавь JDK 21 с именем 'jdk21' (или поменяй имя здесь)
		jdk 'jdk21'
	}

	options {
		timestamps()
		ansiColor('xterm')
		buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
		timeout(time: 60, unit: 'MINUTES')
	}

	parameters {
		string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch')
		choice(name: 'DEVICE_HOST', choices: ['local', 'remote'], description: 'Запуск: local (Appium) | remote (BrowserStack и т.п.)')
		string(name: 'ALLURE_ENDPOINT', defaultValue: 'https://allure.autotests.cloud', description: 'Allure TestOps endpoint')
		string(name: 'ALLURE_PROJECT_ID', defaultValue: '', description: 'ID проекта в TestOps (если пусто — upload пропускается)')
		booleanParam(name: 'UPLOAD_TO_TESTOPS', defaultValue: true, description: 'Грузить результаты в TestOps через allurectl')
		choice(name: 'GRADLE_TASK', choices: ['test', 'clean test'], description: 'Gradle task')
		string(name: 'EXTRA_OPTS', defaultValue: '', description: 'Доп. -D свойства: -Denv=local -Dappium.url=http://127.0.0.1:4723 и т.п.')
	}

	environment {
		GRADLE_USER_HOME   = "${WORKSPACE}/.gradle"
		ALLURE_RESULTS_PATH = ".allure-results"
		// при необходимости замени URL на свой (или оставь SCM-конфиг в job-е)
		GIT_URL = "https://github.com/AliceFabler/wikipedia-tests-final-project.git"
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
				// Даже при падениях тестов дойдём до публикации отчетов
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
					// Сырые результаты и JUnit XML
					archiveArtifacts artifacts: "${ALLURE_RESULTS_PATH}/**", fingerprint: true, allowEmptyArchive: true
					junit testResults: "**/build/test-results/test/*.xml", allowEmptyResults: true

					// Публикация Allure в Jenkins (нужен Allure Jenkins Plugin)
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
					// Создай в Jenkins секрет 'ALLURE_TOKEN' (Secret text) со значением API-токена TestOps
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
			echo "Готово. В артефактах — .allure-results и JUnit XML; отчёт Allure в Jenkins (если плагин есть); TestOps — если включен Upload."
		}
	}
}
