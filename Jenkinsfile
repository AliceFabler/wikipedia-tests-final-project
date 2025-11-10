pipeline {
	agent any

	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr: '20'))
		timeout(time: 60, unit: 'MINUTES')
	}

	parameters {
		choice(name: 'RUN', choices: ['ui', 'api', 'manual'], description: 'Что запускать')
	}

	stages {
		stage('Checkout') {
			steps { checkout scm }
		}

		stage('Init project dir') {
			steps {
				script {
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

		stage('UI tests (BrowserStack / remote only)') {
			when { expression { params.RUN == 'ui' } }
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH" || true

            # only remote: BrowserStack creds/remoteUrl + device/os_version берутся из auth.properties / remote.properties
            ./gradlew clean test               --tests "guru.qa.ui.tests.*"               -DdeviceHost=remote               -Denv=remote               -Dallure.results.directory=.allure-results               --no-daemon --stacktrace --info
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

		stage('API tests') {
			when { expression { params.RUN == 'api' } }
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            [ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH" || true

            ./gradlew clean test               --tests "guru.qa.api.tests.*"               -Dallure.results.directory=.allure-results               --no-daemon --stacktrace --info
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
				dir("${env.PRJ_DIR}") {
					// POSIX sh-safe вариант без bash-замен
					sh '''
            set -e
            [ ! -d ".allure-results" ] && { echo "No .allure-results — skip TestOps upload"; exit 0; }

            PROP="src/test/resources/testops.properties"
            [ ! -f "$PROP" ] && { echo "No testops.properties — skip TestOps upload"; exit 0; }

            EP=$(grep -E '^allure.endpoint=' "$PROP" | cut -d= -f2-)
            PID=$(grep -E '^allure.project.id=' "$PROP" | cut -d= -f2-)
            TOK=$(grep -E '^allure.token=' "$PROP" | cut -d= -f2-)
            LN=$(grep -E '^allure.launch.name=' "$PROP" | cut -d= -f2-)

            # Подставляем Jenkins-переменные, не используя bash-расширения:
            # заменим литералы ${env.JOB_NAME} и ${env.BUILD_NUMBER}
            LN=$(printf '%s' "$LN" | sed 's/${env\.JOB_NAME}/__JOB__/g; s/${env\.BUILD_NUMBER}/__BUILD__/g')
            LN=$(printf '%s' "$LN" | sed "s|__JOB__|${JOB_NAME}|g; s|__BUILD__|${BUILD_NUMBER}|g")

            mkdir -p tools
            if [ ! -x tools/allurectl ]; then
              echo "Downloading allurectl..."
              curl -sSL -o tools/allurectl                 https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64
              chmod +x tools/allurectl
            fi

            export ALLURE_ENDPOINT="$EP"
            export ALLURE_PROJECT_ID="$PID"
            export ALLURE_TOKEN="$TOK"
            export ALLURE_LAUNCH_NAME="$LN"

            echo "Uploading results to Allure TestOps: endpoint=$EP project=$PID launch='$LN'"
            tools/allurectl upload -r .allure-results
          '''
				}
			}
		}
	}
}
