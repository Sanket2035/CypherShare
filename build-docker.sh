#!/bin/bash

set -e

echo "🏗️  Building CypherShare Docker Images..."

# Build backend
echo "📦 Building Java backend..."
cd cyphershare-backend
mvn clean package -DskipTests
docker build -t cyphershare/backend:latest .
echo "✅ Backend image built"

cd ..

# Build frontend  
echo "🎨 Building React frontend..."
cd frontend
docker build -t cyphershare/frontend:latest .
echo "✅ Frontend image built"

cd ..

echo ""
echo "🎉 All images built successfully!"
echo ""
echo "Next steps:"
echo "1. Local testing: docker-compose up -d"
echo "2. Deploy to K8s: cd k8s && kubectl apply -f ."
echo "3. Push to registry: docker push cyphershare/backend:latest && docker push cyphershare/frontend:latest"
