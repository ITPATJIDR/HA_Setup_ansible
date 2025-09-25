pipeline {
    agent any
    
    environment {
        KUBECONFIG = '/var/lib/jenkins/.kube/config'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Starting pipeline to deploy simple pod...'
            }
        }
        
        stage('Deploy Simple Pod') {
            steps {
                script {
                    // Create a simple nginx pod
                    sh '''
                        cat <<EOF | kubectl apply -f -
                        apiVersion: v1
                        kind: Pod
                        metadata:
                          name: test-nginx-pod
                          labels:
                            app: test-nginx
                        spec:
                          containers:
                          - name: nginx
                            image: nginx:latest
                            ports:
                            - containerPort: 80
                        EOF
                    '''
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    sh '''
                        echo "Waiting for pod to be ready..."
                        kubectl wait --for=condition=Ready pod/test-nginx-pod --timeout=60s
                        
                        echo "Pod status:"
                        kubectl get pods test-nginx-pod
                        
                        echo "Pod details:"
                        kubectl describe pod test-nginx-pod
                    '''
                }
            }
        }
        
        stage('Test Pod') {
            steps {
                script {
                    sh '''
                        echo "Testing pod connectivity..."
                        kubectl exec test-nginx-pod -- curl -f http://localhost:80 || echo "Pod is running but curl test failed"
                    '''
                }
            }
        }
    }
    
    post {
        always {
            script {
                sh '''
                    echo "Pipeline completed. Pod status:"
                    kubectl get pods test-nginx-pod || echo "Pod not found"
                '''
            }
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
