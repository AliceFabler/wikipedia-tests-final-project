pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '20'))
		timeout(time: 60, unit: 'MINUTES')
	}

	parameters {
		choice(name: 'RUN', choices: ['ui', 'api', 'manual'], description: 'Что запускать')
		string(name: 'UI_TESTS', defaultValue: 'guru.qa.ui.tests.*', description: 'Фильтр для UI: --tests "<mask>"')
	}

	stages {
		stage('Checkout') {
			steps { checkout scm }
		}

		stage('Init project dir') {
			steps {
				script {
					// код либо в корне, либо в подкаталоге wikipedia-tests-final-project
					env.PRJ_DIR = fileExists('wikipedia-tests-final-project') ? 'wikipedia-tests-final-project' : '.'
				}
			}
		}

		stage('Env') {
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH" || true
            java -version || true
            chmod +x ./gradlew || true
            ./gradlew --version || true
          '''
				}
			}
		}

		stage('UI tests (BrowserStack / remote_test)') {
			when { expression { params.RUN == 'ui' } }
			steps {
				script {
					def uiTests = params.UI_TESTS ?: 'guru.qa.ui.tests.*'
					dir("${env.PRJ_DIR}") {
						sh '''
              set -e
              export JAVA_HOME="${JAVA_HOME:-}"
              [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH" || true

              # Только remote через BrowserStack. Конфиги — из auth.properties / remote.properties.
              ./gradlew clean remote_test \
                --tests "''' + uiTests + '''" \
                -Dallure.results.directory=.allure-results \
                --no-daemon --stacktrace --info
            '''
					}
				}
			}
			post {
				always {
					dir("${env.PRJ_DIR}") {
						archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
						junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
					}
				}
			}
		}

		stage('API tests') {
			when { expression { params.RUN == 'api' } }
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH" || true

            ./gradlew clean test \
              --tests "guru.qa.api.tests.*" \
              -Dallure.results.directory=.allure-results \
              --no-daemon --stacktrace --info
          '''
				}
			}
			post {
				always {
					dir("${env.PRJ_DIR}") {
						archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
						junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
					}
				}
			}
		}

		stage('Manual (prepare results)') {
			when { expression { params.RUN == 'manual' } }
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            mkdir -p .allure-results
            echo '{"manual":true}' > .allure-results/executor.json
          '''
				}
			}
			post {
				always {
					dir("${env.PRJ_DIR}") {
						archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
					}
				}
			}
		}

		stage('Upload to Allure TestOps') {
			steps {
				script {
					def projDir   = env.PRJ_DIR
					def results   = "${projDir}/.allure-results"
					def propsPath = "${projDir}/src/test/resources/testops.properties"

					if (!fileExists(results))  { echo "No .allure-results — skip TestOps upload"; return }
					if (!fileExists(propsPath)){ echo "No testops.properties — skip TestOps upload"; return }

					// читаем свойства
					def propsText = readFile(file: propsPath, encoding: 'UTF-8')
					def lines = propsText.readLines()
					def val = { key ->
						def line = lines.find { it.trim().startsWith("${key}=") }
						line ? line.substring(line.indexOf('=') + 1).trim() : ""
					}

					env.ALLURE_ENDPOINT    = val('allure.endpoint')
					env.ALLURE_PROJECT_ID  = val('allure.project.id')
					env.ALLURE_TOKEN       = val('allure.token')

					def ln = val('allure.launch.name')
					ln = ln.replace('${env.JOB_NAME}', env.JOB_NAME ?: '')
					ln = ln.replace('${env.BUILD_NUMBER}', env.BUILD_NUMBER ?: '')
					env.ALLURE_LAUNCH_NAME = ln

					env.ALLURE_INSECURE = (val('allure.insecure')?.toLowerCase() == 'true') ? 'true' : 'false'

					dir("${env.PRJ_DIR}") {
						int rc = sh(returnStatus: true, script: '''
          set -e

          date -u || true
          java -version || true

          if [ ! -d .allure-results ] || [ -z "$(ls -A .allure-results 2>/dev/null)" ]; then
            echo "No results to upload — skip TestOps upload"
            exit 0
          fi

          mkdir -p tools
          if [ ! -x tools/allurectl ]; then
            echo "Downloading allurectl..."
            curl -fsSL -o tools/allurectl \
              https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64
            chmod +x tools/allurectl
          fi

          INSECURE_FLAG=""
          if [ "${ALLURE_INSECURE}" = "true" ]; then
            INSECURE_FLAG="-i"
            echo "TLS verification is DISABLED (ALLURE_INSECURE=true)."
          fi

          if command -v openssl >/dev/null 2>&1; then
            host="$(echo "$ALLURE_ENDPOINT" | sed -E 's#https?://([^/]+)/?.*#\\1#')"
            echo "TLS debug for $host"
            echo | openssl s_client -servername "$host" -connect "$host:443" 2>/dev/null | openssl x509 -noout -dates -issuer -subject || true
          fi

          echo "Uploading results to Allure TestOps: endpoint=$ALLURE_ENDPOINT project=$ALLURE_PROJECT_ID launch='$ALLURE_LAUNCH_NAME'"

          tools/allurectl $INSECURE_FLAG \
            --endpoint "$ALLURE_ENDPOINT" \
            --token "$ALLURE_TOKEN" \
            upload \
            --project-id "$ALLURE_PROJECT_ID" \
            --launch-name "$ALLURE_LAUNCH_NAME" \
            --ci-type jenkins \
            --job-name "${JOB_NAME:-}" \
            --job-run-id "${BUILD_NUMBER:-}" \
            --job-run-url "${BUILD_URL:-}" \
            --job-uid "${JOB_NAME:-}#${BUILD_NUMBER:-}" \
            .allure-results
        ''')
						if (rc != 0) {
							echo "allurectl upload failed with code ${rc} — marking build UNSTABLE."
							currentBuild.result = 'UNSTABLE'
						}
					}
				}
			}
		}
	}
}
