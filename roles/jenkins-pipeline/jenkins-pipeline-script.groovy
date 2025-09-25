pipeline {
    agent any

    environment {
        KUBE_CREDENTIAL = 'ad8590f3-3946-47a8-80c3-f371769c8621'
        POD_NAME = 'busybox-test-pod'
    }

    options {
        timeout(time: 10, unit: 'MINUTES')
    }

    stages {
        stage('Deploy Busybox Pod') {
            steps {
                echo 'Deploying busybox pod to Kubernetes'
                withCredentials([file(credentialsId: "${KUBE_CREDENTIAL}", variable: 'KUBECONFIG')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$KUBECONFIG"
                        
                        echo "=== Deploying Busybox Pod ==="
                        echo "Pod name: $POD_NAME"
                        echo ""
                        
                        echo "1. Testing kubectl connection..."
                        kubectl cluster-info || echo "❌ kubectl cluster-info failed"
                        kubectl get nodes || echo "❌ kubectl get nodes failed"
                        echo ""
                        
                        echo "2. Cleaning up any existing pod..."
                        kubectl delete pod $POD_NAME --ignore-not-found=true || echo "No existing pod to delete"
                        sleep 5
                        
                        echo "3. Deploying busybox pod..."
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
                        
                        echo "4. Waiting for pod to be ready..."
                        kubectl wait --for=condition=Ready pod/$POD_NAME --timeout=120s
                        
                        echo "5. Pod status:"
                        kubectl get pod $POD_NAME -o wide
                        
                        echo "6. Pod details:"
                        kubectl describe pod $POD_NAME
                    '''
                }
            }
        }

        stage('Verify Pod Deployment') {
            steps {
                echo 'Verifying busybox pod deployment'
                withCredentials([file(credentialsId: "${KUBE_CREDENTIAL}", variable: 'KUBECONFIG')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$KUBECONFIG"
                        
                        echo "=== Pod Deployment Verification ==="
                        echo "Pod name: $POD_NAME"
                        echo ""
                        
                        echo "Pod status:"
                        kubectl get pod $POD_NAME -o wide
                        echo ""
                        
                        echo "Pod details:"
                        kubectl describe pod $POD_NAME
                        echo ""
                        
                        echo "All pods in default namespace:"
                        kubectl get pods
                    '''
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline completed'
            withCredentials([file(credentialsId: "${KUBE_CREDENTIAL}", variable: 'KUBECONFIG')]) {
                sh '''
                    set -eu
                    export KUBECONFIG="$KUBECONFIG"
                    
                    echo "=== Final Pod Status ==="
                    kubectl get pod busybox-test-pod || echo "Pod not found"
                    echo ""
                    echo "=== Pod Events ==="
                    kubectl get events --field-selector involvedObject.name=busybox-test-pod || echo "No events found"
                '''
            }
        }
        success {
            echo '✅ Busybox pod deployment successful!'
            withCredentials([file(credentialsId: "${KUBE_CREDENTIAL}", variable: 'KUBECONFIG')]) {
                sh '''
                    set -eu
                    export KUBECONFIG="$KUBECONFIG"
                    
                    echo "Busybox pod busybox-test-pod is running successfully!"
                    kubectl get pod busybox-test-pod -o wide
                    echo ""
                    echo "To access the pod shell, run:"
                    echo "kubectl exec -it busybox-test-pod -- sh"
                '''
            }
        }
        failure {
            echo '❌ Busybox pod deployment failed!'
            withCredentials([file(credentialsId: "${KUBE_CREDENTIAL}", variable: 'KUBECONFIG')]) {
                sh '''
                    set -eu
                    export KUBECONFIG="$KUBECONFIG"
                    
                    echo "Pod deployment failed. Debugging information:"
                    kubectl get pods | grep busybox-test-pod || echo "Pod not found"
                    kubectl describe pod busybox-test-pod || echo "Cannot describe pod"
                    kubectl logs busybox-test-pod || echo "Cannot get pod logs"
                '''
            }
        }
    }
}
