#!/bin/bash

set -e

echo "🚀 Deploying CypherShare to Kubernetes..."

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl not found. Please install kubectl first."
    exit 1
fi

# Create namespace
echo "📁 Creating namespace..."
kubectl apply -f k8s/namespace.yaml

# Deploy databases
echo "💾 Deploying databases..."
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/postgres-deployment.yaml

echo "⏳ Waiting for databases to be ready..."
kubectl wait --for=condition=ready pod -l app=redis -n cyphershare --timeout=120s
kubectl wait --for=condition=ready pod -l app=postgres -n cyphershare --timeout=120s

echo "✅ Databases ready"

# Deploy application
echo "🎯 Deploying application..."
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml

echo "⏳ Waiting for application to be ready..."
kubectl wait --for=condition=ready pod -l app=cyphershare-backend -n cyphershare --timeout=180s
kubectl wait --for=condition=ready pod -l app=cyphershare-frontend -n cyphershare --timeout=120s

echo "✅ Application deployed"

# Deploy autoscaling
echo "📈 Deploying autoscaling..."
kubectl apply -f k8s/hpa.yaml

# Deploy ingress
echo "🌐 Deploying ingress..."
kubectl apply -f k8s/ingress.yaml

echo ""
echo "🎉 Deployment complete!"
echo ""
echo "Status:"
kubectl get pods -n cyphershare
echo ""
echo "Services:"
kubectl get svc -n cyphershare
echo ""
echo "Ingress:"
kubectl get ingress -n cyphershare
