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
	environment {
		JAVA_HOME = tool(name: 'JDK17', type: 'jdk')
		PATH = "${JAVA_HOME}/bin:${PATH}"
	}
	stages {
		stage('Checkout') { steps { checkout scm } }
		stage('Env') {
			steps {
				dir('wikipedia-tests-final-project') {
					sh 'java -version || true'
					sh './gradlew --version || true'
				}
			}
		}
		stage('Run') {
			steps {
				script {
					if (params.RUN == 'ui') {
						dir('wikipedia-tests-final-project') {
							sh """
                ./gradlew clean test \\
                  --tests "guru.qa.ui.tests.*" \\
                  -DdeviceHost=remote \\
                  -Denv=remote \\
                  -Dallure.results.directory=.allure-results \\
                  --no-daemon --stacktrace --info
              """
						}
					} else if (params.RUN == 'api') {
						dir('wikipedia-tests-final-project') {
							sh """
                ./gradlew clean test \\
                  --tests "guru.qa.api.tests.*" \\
                  -Dallure.results.directory=.allure-results \\
                  --no-daemon --stacktrace --info
              """
						}
					} else {
						dir('wikipedia-tests-final-project') {
							sh 'mkdir -p .allure-results && echo "{\\\"manual\\\":true}" > .allure-results/executor.json'
						}
					}
				}
			}
			post {
				always {
					dir('wikipedia-tests-final-project') {
						archiveArtifacts artifacts: '.allure-results/**', fingerprint: true, allowEmptyArchive: true
						junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
					}
				}
			}
		}
		stage('Upload to Allure TestOps') {
			steps {
				dir('wikipedia-tests-final-project') {
					sh '''#!/usr/bin/env bash
            set -e
            PROP=src/test/resources/testops.properties
            EP=$(grep -E '^allure.endpoint=' "$PROP" | cut -d= -f2-)
            PID=$(grep -E '^allure.project.id=' "$PROP" | cut -d= -f2-)
            TOK=$(grep -E '^allure.token=' "$PROP" | cut -d= -f2-)
            LN=$(grep -E '^allure.launch.name=' "$PROP" | cut -d= -f2-)
            LN="${LN//\${env.JOB_NAME}/$JOB_NAME}"
            LN="${LN//\${env.BUILD_NUMBER}/$BUILD_NUMBER}"

            mkdir -p tools
            if [[ ! -x tools/allurectl ]]; then
              echo "Downloading allurectl..."
              curl -sSL -o tools/allurectl https://github.com/allure-framework/allurectl/releases/latest/download/allurectl_linux_amd64
              chmod +x tools/allurectl
            fi

            export ALLURE_ENDPOINT="$EP"
            export ALLURE_PROJECT_ID="$PID"
            export ALLURE_TOKEN="$TOK"
            export ALLURE_LAUNCH_NAME="$LN"

            tools/allurectl upload -r .allure-results
          '''
				}
			}
		}
	}
}
