/*
 * Jenkinsfile — wikipedia-tests-final-project
 * Java 17 + Gradle Wrapper 8.10.x
 * Параметры: BRANCH, DEVICE_HOST, GRADLE_TASK, EXTRA_OPTS, UPLOAD_TO_TESTOPS, ALLURE_*
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
		string (name: 'BRANCH',      defaultValue: 'develop', description: 'Git branch')
		choice (name: 'DEVICE_HOST', choices: ['local','remote'], description: 'local (Appium) | remote (BrowserStack)')
		choice (name: 'GRADLE_TASK', choices: ['test','clean test'], description: 'Gradle task')
		string (name: 'EXTRA_OPTS',  defaultValue: '', description: 'Доп. -D: -Denv=local -Dappium.url=http://127.0.0.1:4723 ...')
		booleanParam(name: 'UPLOAD_TO_TESTOPS', defaultValue: true, description: 'Загрузка результатов в Allure TestOps через allurectl')
		string (name: 'ALLURE_ENDPOINT',  defaultValue: 'https://allure.autotests.cloud', description: 'Allure TestOps endpoint')
		string (name: 'ALLURE_PROJECT_ID', defaultValue: '', description: 'ID проекта в TestOps (если пусто — upload пропускается)')
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
				script {
					// Находим корень Gradle-проекта и gradlew
					env.PROJ_DIR = sh(script: '''
            set -e
            for d in . */; do
              [ -f "$d/settings.gradle" ] || [ -f "$d/settings.gradle.kts" ] && { echo "$d"; break; }
            done
          ''', returnStdout: true).trim()
					if (!env.PROJ_DIR) { env.PROJ_DIR = '.' }

					env.WRAPPER = sh(script: 'find "${PROJ_DIR}" -maxdepth 2 -type f -name gradlew | head -n1', returnStdout: true).trim()
					if (!env.WRAPPER) { error 'gradlew не найден. Добавь wrapper в репозиторий.' }

					sh "chmod +x ${env.WRAPPER}"
					echo "Project dir: ${env.PROJ_DIR}"
					echo "Using wrapper: ${env.WRAPPER}"
					sh "cd ${env.PROJ_DIR} && ${env.WRAPPER} --version"
				}
			}
		}

		stage('Test') {
			steps {
				catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
					script {
						def extra = params.EXTRA_OPTS?.trim()
						def cmd = """
              cd ${env.PROJ_DIR} && ${env.WRAPPER} ${params.GRADLE_TASK} \
                -DdeviceHost=${params.DEVICE_HOST} \
                ${extra} \
                --stacktrace --no-daemon --info
            """.stripIndent().trim()
						sh label: 'Run Gradle tests', script: cmd
					}
				}
			}
			post {
				always {
					archiveArtifacts artifacts: "${ALLURE_RESULTS_PATH}/**", fingerprint: true, allowEmptyArchive: true
					junit testResults: "**/build/test-results/test/*.xml", allowEmptyResults: true

					script {
						if (fileExists("${ALLURE_RESULTS_PATH}")) {
							try {
								allure([
									includeProperties: false,
									jdk: '',
									results: [[path: "${ALLURE_RESULTS_PATH}"]],
									reportBuildPolicy: 'ALWAYS'
								])
							} catch (ignored) {
								echo "Allure Jenkins plugin не настроен — пропускаю публикацию отчёта."
							}
						} else {
							echo "Папка ${ALLURE_RESULTS_PATH} не найдена — пропускаю публикацию Allure."
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
              ./allurectl --endpoint "${ALLURE_ENDPOINT}" --token "${ALLURE_TOKEN}" results upload \
                --project-id "${ALLURE_PROJECT_ID}" \
                --launch-name "Jenkins #${BUILD_NUMBER} (${BRANCH_NAME}) - ${DEVICE_HOST}" \
                --results "**/.allure-results" || true
            '''
					}
				}
			}
		}
	}

	post {
		always {
			echo "Готово. Если отчёта нет — проверь, что тесты действительно стартовали и сформировали ${ALLURE_RESULTS_PATH}/"
		}
	}
}
