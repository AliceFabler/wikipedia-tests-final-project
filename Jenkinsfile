/*
 * Jenkinsfile — robust (Java 17 + Gradle 8.10)
 * — Автопоиск корня проекта (settings.gradle/build.gradle в подпапке)
 * — Автопоиск gradlew; fallback на системный gradle
 * — Архивация Allure/JUnit из любой подпапки
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
		choice(name: 'DEVICE_HOST', choices: ['local', 'remote'], description: 'Запуск: local | remote')
		choice(name: 'GRADLE_TASK', choices: ['test', 'clean test'], description: 'Gradle task')
		string(name: 'EXTRA_OPTS', defaultValue: '', description: 'Доп. -D: -Denv=local -Dappium.url=http://127.0.0.1:4723 ...')
	}

	environment {
		GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
		// .allure-results может лежать в подпапке → используем **:
		ALLURE_RESULTS_GLOB = "**/.allure-results/**"
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
				script {
					// 1) Найти корень проекта (если код в подпапке)
					env.PROJECT_DIR = sh(
						script: '''
              set -e
              CAND="$(for d in . $(find . -maxdepth 3 -type d); do
                        if [ -f "$d/settings.gradle" ] || [ -f "$d/settings.gradle.kts" ] || \
                           [ -f "$d/build.gradle" ]    || [ -f "$d/build.gradle.kts" ]; then
                          echo "$d"; break
                        fi
                      done)"
              echo "${CAND:-.}"
            ''',
						returnStdout: true
					).trim()
					echo "Detected project dir: ${env.PROJECT_DIR}"

					// 2) Найти gradlew (если лежит в подпапке)
					env.WRAPPER_PATH = sh(
						script: "find '${env.PROJECT_DIR}' -maxdepth 2 -type f -name gradlew | head -n1 || true",
						returnStdout: true
					).trim()
					if (env.WRAPPER_PATH) {
						sh "chmod +x '${env.WRAPPER_PATH}'"
						echo "Using wrapper: ${env.WRAPPER_PATH}"
					} else {
						echo "gradlew not found → will try system 'gradle' binary"
						sh 'gradle --version || true'
					}

					// 3) Показать Gradle версию из проекта
					sh """
            cd '${env.PROJECT_DIR}'
            if [ -n '${env.WRAPPER_PATH}' ]; then
              '${env.WRAPPER_PATH}' --version || true
            else
              gradle --version || true
            fi
          """
				}
			}
		}

		stage('Test') {
			steps {
				catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
					sh """
            cd '${PROJECT_DIR}'
            CMD="${WRAPPER_PATH}"
            if [ -z "$CMD" ]; then
              if ! command -v gradle >/dev/null 2>&1; then
                echo "ERROR: gradle не найден и gradlew отсутствует. Установи Gradle 8.10 в Jenkins/на агенте ИЛИ закоммить gradle wrapper."
                exit 1
              fi
              CMD="gradle"
            fi
            echo "Run: $CMD ${GRADLE_TASK}"
            $CMD ${GRADLE_TASK} \
              -DdeviceHost=${DEVICE_HOST} \
              ${EXTRA_OPTS} \
              --stacktrace --no-daemon --info
          """
				}
			}
			post {
				always {
					archiveArtifacts artifacts: "${ALLURE_RESULTS_GLOB}", fingerprint: true, allowEmptyArchive: true
					junit testResults: "**/build/test-results/test/*.xml", allowEmptyResults: true
					script {
						try {
							// Публикация Allure в Jenkins (если плагин установлен)
							allure([
								includeProperties: false,
								jdk: '',
								results: [[path: "**/.allure-results"]],
								reportBuildPolicy: 'ALWAYS'
							])
						} catch (ignored) {
							echo "Allure Jenkins plugin не настроен — пропускаю публикацию отчёта."
						}
					}
				}
			}
		}
	}

	post {
		always {
			echo "Done. Если упало на поиске gradle/gradlew — см. инструкции ниже."
		}
	}
}
