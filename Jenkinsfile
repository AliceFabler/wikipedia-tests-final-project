/*
 * Jenkinsfile — wikipedia-tests-final-project (Java 17)
 * Режимы: api | ui-remote | manual | all
 * Allure: пишет в .allure-results и (опционально) грузит в TestOps через allurectl upload
 */
pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '30'))
		disableConcurrentBuilds()
		timeout(time: 60, unit: 'MINUTES')
	}

	parameters {
		choice(name: 'RUN_TARGET', choices: ['api', 'ui-remote', 'manual', 'all'], description: 'Что запускать')
		booleanParam(name: 'RUN_PARALLEL', defaultValue: true, description: 'RUN_TARGET=all — параллельный запуск')
		string(name: 'ALLURE_RESULTS_PATH', defaultValue: '.allure-results', description: 'Куда писать Allure')
		string(name: 'GRADLE_ARGS', defaultValue: '--rerun-tasks --no-daemon', description: 'Доп. аргументы Gradle')

		// UI (remote) overrides; если пусто — берутся из remote.properties/auth.properties
		string(name: 'APP', defaultValue: '', description: 'bs://… или URL APK/APP')
		string(name: 'DEVICE', defaultValue: '', description: 'Например Google Pixel 7')
		string(name: 'OS_VERSION', defaultValue: '', description: 'Например 13.0')
		string(name: 'REMOTE_URL', defaultValue: '', description: 'Переопределить remoteUrl (если пусто — из auth.properties)')
		booleanParam(name: 'UPLOAD_TO_TESTOPS', defaultValue: true, description: 'Загрузить результаты в Allure TestOps')
	}

	environment {
		ALLURE_RESULTS = "${params.ALLURE_RESULTS_PATH}"
		GRADLE_ARGS    = "${params.GRADLE_ARGS}"
	}

	stages {

		stage('Prep') {
			steps {
				sh '''
          echo "Java: $(java -version 2>&1 | head -n1)"
          echo "Gradle Wrapper: $(./gradlew -v | head -n1)"

          rm -rf "${ALLURE_RESULTS}" && mkdir -p "${ALLURE_RESULTS}"

          for f in allure.properties api.properties auth.properties remote.properties; do
            test -f "src/test/resources/$f" && echo "$f ✓" || { echo "$f MISSING!"; exit 1; }
          done

          if [ -f src/test/resources/testops.properties ]; then
            echo "testops.properties ✓"
          else
            echo "testops.properties not found — UPLOAD_TO_TESTOPS may be skipped"
          fi
        '''
			}
		}

		// ---------- Параллельный запуск (когда RUN_TARGET=all и RUN_PARALLEL=true) ----------
		stage('Tests (parallel)') {
			when {
				allOf {
					expression { params.RUN_TARGET == 'all' }
					expression { params.RUN_PARALLEL }
				}
			}
			parallel {
				stage('API tests') {
					steps {
						sh '''
              ./gradlew test \
                -Ptags=api \
                -Dallure.results.directory="${ALLURE_RESULTS}" \
                ${GRADLE_ARGS}
            '''
					}
					post {
						always {
							script {
								def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
								echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
							}
							allure results: [[path: "${ALLURE_RESULTS}"]]
							archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
							junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
						}
					}
				}

				stage('UI tests (remote)') {
					steps {
						script {
							def overrides = []
							if (params.APP?.trim())        { overrides << "-Dapp='${params.APP.trim()}'" }
							if (params.DEVICE?.trim())     { overrides << "-Ddevice='${params.DEVICE.trim()}'" }
							if (params.OS_VERSION?.trim()) { overrides << "-Dos_version='${params.OS_VERSION.trim()}'" }
							if (params.REMOTE_URL?.trim()) { overrides << "-DremoteUrl='${params.REMOTE_URL.trim()}'" }
							def o = overrides.join(' ')
							sh """
                ./gradlew test \\
                  -Ptags='android,remote,wikipedia' \\
                  -DdeviceHost=remote -Denv=remote \\
                  ${o} \\
                  -Dallure.results.directory='${ALLURE_RESULTS}' \\
                  ${GRADLE_ARGS}
              """
						}
					}
					post {
						always {
							script {
								def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
								echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
							}
							allure results: [[path: "${ALLURE_RESULTS}"]]
							archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
							junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
						}
					}
				}

				stage('Manual tests (tag=manual)') {
					steps {
						sh '''
              ./gradlew test \
                -Ptags=manual \
                -Dallure.results.directory="${ALLURE_RESULTS}" \
                ${GRADLE_ARGS}
            '''
					}
					post {
						always {
							script {
								def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
								echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
							}
							allure results: [[path: "${ALLURE_RESULTS}"]]
							archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
							junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
						}
					}
				}
			}
		}

		// ---------- Последовательные стадии (когда RUN_TARGET != all или RUN_PARALLEL=false) ----------
		stage('API tests') {
			when {
				anyOf {
					expression { params.RUN_TARGET == 'api' }
					allOf { expression { params.RUN_TARGET == 'all' }; expression { !params.RUN_PARALLEL } }
				}
			}
			steps {
				sh '''
          ./gradlew test \
            -Ptags=api \
            -Dallure.results.directory="${ALLURE_RESULTS}" \
            ${GRADLE_ARGS}
        '''
			}
			post {
				always {
					script {
						def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
						echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
					}
					allure results: [[path: "${ALLURE_RESULTS}"]]
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
				}
			}
		}

		stage('UI tests (remote)') {
			when {
				anyOf {
					expression { params.RUN_TARGET == 'ui-remote' }
					allOf { expression { params.RUN_TARGET == 'all' }; expression { !params.RUN_PARALLEL } }
				}
			}
			steps {
				script {
					def overrides = []
					if (params.APP?.trim())        { overrides << "-Dapp='${params.APP.trim()}'" }
					if (params.DEVICE?.trim())     { overrides << "-Ddevice='${params.DEVICE.trim()}'" }
					if (params.OS_VERSION?.trim()) { overrides << "-Dos_version='${params.OS_VERSION.trim()}'" }
					if (params.REMOTE_URL?.trim()) { overrides << "-DremoteUrl='${params.REMOTE_URL.trim()}'" }
					def o = overrides.join(' ')
					sh """
            ./gradlew test \\
              -Ptags='android,remote,wikipedia' \\
              -DdeviceHost=remote -Denv=remote \\
              ${o} \\
              -Dallure.results.directory='${ALLURE_RESULTS}' \\
              ${GRADLE_ARGS}
          """
				}
			}
			post {
				always {
					script {
						def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
						echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
					}
					allure results: [[path: "${ALLURE_RESULTS}"]]
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
				}
			}
		}

		stage('Manual tests (tag=manual)') {
			when {
				anyOf {
					expression { params.RUN_TARGET == 'manual' }
					allOf { expression { params.RUN_TARGET == 'all' }; expression { !params.RUN_PARALLEL } }
				}
			}
			steps {
				sh '''
          ./gradlew test \
            -Ptags=manual \
            -Dallure.results.directory="${ALLURE_RESULTS}" \
            ${GRADLE_ARGS}
        '''
			}
			post {
				always {
					script {
						def cnt = sh(script: 'ls -1 "${ALLURE_RESULTS}" 2>/dev/null | wc -l || echo 0', returnStdout: true).trim()
						echo "Files in ${env.ALLURE_RESULTS}: ${cnt}"
					}
					allure results: [[path: "${ALLURE_RESULTS}"]]
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
				}
			}
		}

		stage('Upload to Allure TestOps') {
			when { expression { params.UPLOAD_TO_TESTOPS } }
			steps {
				sh '''
          set -Eeuo pipefail

          if [ ! -d "${ALLURE_RESULTS}" ]; then
            echo "No results dir: ${ALLURE_RESULTS}"; exit 1
          fi
          echo "Results count: $(ls -1 "${ALLURE_RESULTS}" | wc -l)"
          ls -1 "${ALLURE_RESULTS}" | head -n 20 || true

          if [ ! -f src/test/resources/testops.properties ]; then
            echo "testops.properties not found — skip upload"
            exit 0
          fi

          ENDPOINT=$(grep -E '^allure\\.endpoint=' src/test/resources/testops.properties | sed 's/^allure\\.endpoint=//' | tr -d '\\r' | xargs)
          PROJECT_ID=$(grep -E '^allure\\.project\\.id=' src/test/resources/testops.properties | sed 's/^allure\\.project\\.id=//' | tr -d '\\r' | xargs)
          TOKEN=$(grep -E '^allure\\.token=' src/test/resources/testops.properties | sed 's/^allure\\.token=//' | tr -d '\\r' | xargs)
          LAUNCH=$(grep -E '^allure\\.launch\\.name=' src/test/resources/testops.properties | sed 's/^allure\\.launch\\.name=//' | tr -d '\\r' | xargs || true)
          [ -z "${LAUNCH:-}" ] && LAUNCH="wiki-mobile:${JOB_NAME} #${BUILD_NUMBER}"

          [ -z "$ENDPOINT" ] && { echo "endpoint empty"; exit 1; }
          [ -z "$PROJECT_ID" ] && { echo "projectId empty"; exit 1; }
          [ -z "$TOKEN" ] && { echo "token empty"; exit 1; }

          os=$(uname -s | tr 'A-Z' 'a-z')
          curl -sSL "https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_${os}_amd64" -o allurectl
          chmod +x allurectl
          ./allurectl --version || true

          # CLI v2: upload <dir>
          export ALLURE_ENDPOINT="$ENDPOINT"
          export ALLURE_TOKEN="$TOKEN"
          export ALLURE_PROJECT_ID="$PROJECT_ID"

          ./allurectl upload "${ALLURE_RESULTS}" \
            --launch-name "$LAUNCH" \
            --ci-type jenkins \
            --job-name "${JOB_NAME}" \
            --job-id "${JOB_NAME}" \
            --job-run-name "#${BUILD_NUMBER}" \
            --job-run-id "${BUILD_NUMBER}" \
            --job-url "${JOB_URL}" \
            --job-run-url "${BUILD_URL}"

          echo "Upload done."
        '''
			}
		}
	}

	post {
		always {
			echo "Готово. Если Allure пуст — проверь, что тесты реально выполнялись (а не up-to-date) и что в ${env.ALLURE_RESULTS} есть файлы."
		}
	}
}
