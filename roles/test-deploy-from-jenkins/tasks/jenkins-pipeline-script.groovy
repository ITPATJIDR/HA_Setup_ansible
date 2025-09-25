pipeline {
    agent {
        kubernetes {
            cloud 'test-integrate-kube' // Name of your configured Kubernetes cloud
            idleMinutes 5
            retries 1
            slaveConnectTimeout 300
            yaml '''
                apiVersion: v1
                kind: Pod
                metadata:
                  labels:
                    jenkins: agent
                spec:
                    containers:
                    - name: jnlp
                      image: 'jenkins/inbound-agent:latest'
                      resources:
                          requests:
                              cpu: '100m'
                              memory: '128Mi'
                          limits:
                              cpu: '500m'
                              memory: '256Mi'
                    - name: kubectl
                      image: 'bitnami/kubectl:latest'
                      command:
                      - cat
                      tty: true
                      resources:
                          requests:
                              cpu: '100m'
                              memory: '128Mi'
                          limits:
                              cpu: '500m'
                              memory: '256Mi'
            '''
        }
    }

    environment {
        KUBECONFIG_CREDENTIALS = '92e62619-a72a-46ce-a024-f96315273906'
        POD_NAME = 'busybox-test-pod'
    }

    options {
        timeout(time: 10, unit: 'MINUTES')
    }

    stages {
        stage('Wait for Agent') {
            steps {
                echo 'Waiting for Jenkins agent to be ready'
                sleep 30
            }
        }
        
        stage('Deploy Busybox Pod') {
            steps {
                echo 'Deploying busybox pod to Kubernetes'
                script {
                    withKubeConfig([credentialsId: "${KUBECONFIG_CREDENTIALS}"]) {
                        container('kubectl') {
                            sh '''
                                echo "=== Deploying Busybox Pod ==="
                                echo "Pod name: $POD_NAME"
                                echo ""
                                
                                echo "1. Cleaning up any existing pod..."
                                kubectl delete pod $POD_NAME --ignore-not-found=true || echo "No existing pod to delete"
                                sleep 5
                                
                                echo "2. Deploying busybox pod..."
                                cat <<EOF | kubectl apply -f -
                                apiVersion: v1
                                kind: Pod
                                metadata:
                                  name: $POD_NAME
                                  labels:
                                    app: busybox-test
                                    environment: jenkins-test
                                spec:
                                  containers:
                                  - name: busybox
                                    image: busybox:latest
                                    command: ['sleep', '3600']
                                    resources:
                                      requests:
                                        memory: "32Mi"
                                        cpu: "100m"
                                      limits:
                                        memory: "64Mi"
                                        cpu: "200m"
                                EOF
                                
                                echo "3. Waiting for pod to be ready..."
                                kubectl wait --for=condition=Ready pod/$POD_NAME --timeout=120s
                                
                                echo "4. Pod status:"
                                kubectl get pod $POD_NAME -o wide
                                
                                echo "5. Pod details:"
                                kubectl describe pod $POD_NAME
                            '''
                        }
                    }
                }
            }
        }

        stage('Verify Pod Deployment') {
            steps {
                echo 'Verifying busybox pod deployment'
                script {
                    withKubeConfig([credentialsId: "${KUBECONFIG_CREDENTIALS}"]) {
                        container('kubectl') {
                            sh '''
                                echo "=== Pod Deployment Verification ==="
                                echo "Pod name: $POD_NAME"
                                echo ""
                                
                                echo "Pod status:"
                                kubectl get pod $POD_NAME -o wide
                                echo ""
                                
                                echo "Pod details:"
                                kubectl describe pod $POD_NAME
                            '''
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline completed'
            script {
                withKubeConfig([credentialsId: '92e62619-a72a-46ce-a024-f96315273906']) {
                    container('kubectl') {
                        sh '''
                            echo "=== Final Pod Status ==="
                            kubectl get pod busybox-test-pod || echo "Pod not found"
                            echo ""
                            echo "=== Pod Events ==="
                            kubectl get events --field-selector involvedObject.name=busybox-test-pod || echo "No events found"
                        '''
                    }
                }
            }
        }
        success {
            echo '✅ Busybox pod deployment successful!'
            script {
                withKubeConfig([credentialsId: '92e62619-a72a-46ce-a024-f96315273906']) {
                    container('kubectl') {
                        sh '''
                            echo "Busybox pod busybox-test-pod is running successfully!"
                            kubectl get pod busybox-test-pod -o wide
                            echo ""
                            echo "To access the pod shell, run:"
                            echo "kubectl exec -it busybox-test-pod -- sh"
                        '''
                    }
                }
            }
        }
        failure {
            echo '❌ Busybox pod deployment failed!'
            script {
                withKubeConfig([credentialsId: '92e62619-a72a-46ce-a024-f96315273906']) {
                    container('kubectl') {
                        sh '''
                            echo "Pod deployment failed. Debugging information:"
                            kubectl get pods | grep busybox-test-pod || echo "Pod not found"
                            kubectl describe pod busybox-test-pod || echo "Cannot describe pod"
                            kubectl logs busybox-test-pod || echo "Cannot get pod logs"
                        '''
                    }
                }
            }
        }
    }
}
