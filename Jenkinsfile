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
				script {
					def projDir = env.PRJ_DIR
					def resultsDir = "${projDir}/.allure-results"
					def propsPath = "${projDir}/src/test/resources/testops.properties"

					if (!fileExists(resultsDir)) {
						echo "No .allure-results — skip TestOps upload"
						return
					}
					if (!fileExists(propsPath)) {
						echo "No testops.properties — skip TestOps upload"
						return
					}

					def propsText = readFile(file: propsPath, encoding: 'UTF-8')
					def lines = propsText.readLines()

					def val = { key ->
						def line = lines.find { it.trim().startsWith("${key}=") }
						line ? line.substring(line.indexOf('=') + 1).trim() : ""
					}

					env.ALLURE_ENDPOINT    = val('allure.endpoint')
					env.ALLURE_PROJECT_ID  = val('allure.project.id')
					env.ALLURE_TOKEN       = val('allure.token')
					def ln                 = val('allure.launch.name')

					// Подстановка Jenkins-переменных из шаблона файла
					ln = ln.replace('${env.JOB_NAME}', env.JOB_NAME ?: '')
					ln = ln.replace('${env.BUILD_NUMBER}', env.BUILD_NUMBER ?: '')
					env.ALLURE_LAUNCH_NAME = ln
				}

				dir("${env.PRJ_DIR}") {
					sh '''
            set -e
            mkdir -p tools
            if [ ! -x tools/allurectl ]; then
              echo "Downloading allurectl..."
              curl -sSL -o tools/allurectl                 https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64
              chmod +x tools/allurectl
            fi

            echo "Uploading results to Allure TestOps: endpoint=$ALLURE_ENDPOINT project=$ALLURE_PROJECT_ID launch='$ALLURE_LAUNCH_NAME'"
            tools/allurectl upload -r .allure-results
          '''
				}
			}
		}
	}
}
