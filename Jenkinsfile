/*
 * Jenkinsfile — wikipedia-tests-final-project (Java 17, creds & configs from repo)
 *
 * RUN_TARGET:   api | ui-remote | manual | all
 * RUN_PARALLEL: true -> при RUN_TARGET=all этапы API/UI/MANUAL идут параллельно
 *
 * Конфиги/креды читаются из репозитория:
 *   src/test/resources/allure.properties
 *   src/test/resources/api.properties
 *   src/test/resources/auth.properties
 *   src/test/resources/remote.properties
 *   src/test/resources/testops.properties
 */
pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '30'))
		disableConcurrentBuilds()
		timeout(time: 60, unit: 'MINUTES')
	}

	// tools { jdk 'jdk-17' } // ноды уже на Java 17

	parameters {
		choice(name: 'RUN_TARGET', choices: ['api', 'ui-remote', 'manual', 'all'], description: 'Что запускать')
		booleanParam(name: 'RUN_PARALLEL', defaultValue: true, description: 'При RUN_TARGET=all — запускать параллельно')
		string(name: 'ALLURE_RESULTS_PATH', defaultValue: '.allure-results', description: 'Куда писать Allure')
		string(name: 'GRADLE_ARGS', defaultValue: '', description: 'Доп. аргументы Gradle (напр., --no-daemon --stacktrace)')
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
          echo "Gradle Wrapper: $(./gradlew -v | head -n3 | tr "\\n" " | ")"
          rm -rf "${ALLURE_RESULTS}" && mkdir -p "${ALLURE_RESULTS}"

          for f in allure.properties api.properties auth.properties remote.properties; do
            if [ -f "src/test/resources/$f" ]; then
              echo "$f ✓"
            else
              echo "$f MISSING!"
              exit 1
            fi
          done

          if [ -f src/test/resources/testops.properties ]; then
            echo "testops.properties ✓"
          else
            echo "testops.properties not found — UPLOAD_TO_TESTOPS may be skipped"
          fi
        '''
			}
		}

		// -------- PARALLEL (RUN_TARGET=all && RUN_PARALLEL=true) --------
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
                -Ptags="api" \
                -Dallure.results.directory="${ALLURE_RESULTS}" \
                ${GRADLE_ARGS}
            '''
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
							def overridesStr = overrides.join(' ')
							sh """
                ./gradlew test \\
                  -Ptags='android,remote,wikipedia' \\
                  -DdeviceHost=remote -Denv=remote \\
                  ${overridesStr} \\
                  -Dallure.results.directory='${ALLURE_RESULTS}' \\
                  ${GRADLE_ARGS}
              """
						}
					}
				}
				stage('Manual tests (tag=manual)') {
					steps {
						sh '''
              ./gradlew test \
                -Ptags="manual" \
                -Dallure.results.directory="${ALLURE_RESULTS}" \
                ${GRADLE_ARGS}
            '''
					}
				}
			}
			post {
				always {
					allure(results: [[path: "${ALLURE_RESULTS}"]])
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
				}
			}
		}

		// -------- SEQUENTIAL (для одиночных целей или all без параллели) --------
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
            -Ptags="api" \
            -Dallure.results.directory="${ALLURE_RESULTS}" \
            ${GRADLE_ARGS}
        '''
			}
			post {
				always {
					allure(results: [[path: "${ALLURE_RESULTS}"]])
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
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
					def overridesStr = overrides.join(' ')
					sh """
            ./gradlew test \\
              -Ptags='android,remote,wikipedia' \\
              -DdeviceHost=remote -Denv=remote \\
              ${overridesStr} \\
              -Dallure.results.directory='${ALLURE_RESULTS}' \\
              ${GRADLE_ARGS}
          """
				}
			}
			post {
				always {
					allure(results: [[path: "${ALLURE_RESULTS}"]])
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
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
            -Ptags="manual" \
            -Dallure.results.directory="${ALLURE_RESULTS}" \
            ${GRADLE_ARGS}
        '''
			}
			post {
				always {
					allure(results: [[path: "${ALLURE_RESULTS}"]])
					archiveArtifacts artifacts: "${ALLURE_RESULTS}/**", allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: "build/test-results/test/*.xml"
				}
			}
		}

		// -------- Upload to Allure TestOps (без readProperties) --------
		stage('Upload to Allure TestOps') {
			when { expression { params.UPLOAD_TO_TESTOPS } }
			steps {
				sh '''
          set +x
          if [ ! -f src/test/resources/testops.properties ]; then
            echo "testops.properties not found — skip upload"
            exit 0
          fi

          ENDPOINT=$(grep -E '^allure\\.endpoint=' src/test/resources/testops.properties | sed 's/^allure\\.endpoint=//')
          PROJECT_ID=$(grep -E '^allure\\.project\\.id=' src/test/resources/testops.properties | sed 's/^allure\\.project\\.id=//')
          TOKEN=$(grep -E '^allure\\.token=' src/test/resources/testops.properties | sed 's/^allure\\.token=//')
          LAUNCH=$(grep -E '^allure\\.launch\\.name=' src/test/resources/testops.properties | sed 's/^allure\\.launch\\.name=//')
          [ -z "$LAUNCH" ] && LAUNCH="wiki-mobile:${JOB_NAME} #${BUILD_NUMBER}"

          os=$(uname -s | tr A-Z a-z)
          curl -sL "https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_${os}_amd64" -o allurectl
          chmod +x allurectl

          ./allurectl \
            --endpoint "$ENDPOINT" \
            --token "$TOKEN" \
            upload --project-id "$PROJECT_ID" \
            --results "${ALLURE_RESULTS}" \
            --launch-name "$LAUNCH" \
            --force || true
        '''
			}
		}
	}

	post {
		always {
			echo "Готово. Если Allure пуст — проверь, что тесты записали результаты в ${ALLURE_RESULTS}."
		}
	}
}
