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
					// Репозиторий содержит код либо в корне, либо в подкаталоге wikipedia-tests-final-project
					env.PRJ_DIR = fileExists('wikipedia-tests-final-project') ? 'wikipedia-tests-final-project' : '.'
				}
			}
		}

		stage('Env') {
			steps {
				dir("${env.PRJ_DIR}") {
					// Используем системную Java. Если Jenkins проставил JAVA_HOME — добавим его в PATH.
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            if [ -n "$JAVA_HOME" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
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
            if [ -n "$JAVA_HOME" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi

            # ⚙️ ВАЖНО: только remote. Параметры BrowserStack и девайса берутся из:
            #   src/test/resources/auth.properties (userName, key, remoteUrl)
            #   src/test/resources/remote.properties (device, os_version, project, build, name, app)
            ./gradlew clean test \
              --tests "guru.qa.ui.tests.*" \
              -DdeviceHost=remote \
              -Denv=remote \
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

		stage('API tests') {
			when { expression { params.RUN == 'api' } }
			steps {
				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            export JAVA_HOME="${JAVA_HOME:-}"
            if [ -n "$JAVA_HOME" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi

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
				dir("${env.PRJ_DIR}") {
					// Читаем src/test/resources/testops.properties и грузим .allure-results в TestOps
					sh '''
            set -e
            if [ ! -d ".allure-results" ]; then
              echo "No .allure-results directory — skip TestOps upload"
              exit 0
            fi

            PROP="src/test/resources/testops.properties"
            if [ ! -f "$PROP" ]; then
              echo "No testops.properties found — skip TestOps upload"
              exit 0
            fi

            EP=$(grep -E '^allure.endpoint=' "$PROP" | cut -d= -f2-)
            PID=$(grep -E '^allure.project.id=' "$PROP" | cut -d= -f2-)
            TOK=$(grep -E '^allure.token=' "$PROP" | cut -d= -f2-)
            LN=$(grep -E '^allure.launch.name=' "$PROP" | cut -d= -f2-)

            # Подставляем Jenkins-переменные в шаблон launch name
            LN="${LN//\\${env.JOB_NAME}/$JOB_NAME}"
            LN="${LN//\\${env.BUILD_NUMBER}/$BUILD_NUMBER}"

            mkdir -p tools
            if [ ! -x tools/allurectl ]; then
              echo "Downloading allurectl..."
              curl -sSL -o tools/allurectl \
                https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64
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
