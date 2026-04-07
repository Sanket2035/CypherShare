# CypherShare Kubernetes Deployment

## Prerequisites
- Kubernetes cluster (1.24+)
- kubectl configured
- Helm 3 (optional, for cert-manager)
- Docker for building images

## Quick Start

### 1. Build Docker Images
```bash
# Build backend
cd cyphershare-backend
mvn clean package -DskipTests
docker build -t cyphershare/backend:latest .

# Build frontend
cd ../frontend
docker build -t cyphershare/frontend:latest .
```

### 2. Push to Registry (if using remote cluster)
```bash
docker tag cyphershare/backend:latest your-registry/cyphershare/backend:latest
docker push your-registry/cyphershare/backend:latest

docker tag cyphershare/frontend:latest your-registry/cyphershare/frontend:latest
docker push your-registry/cyphershare/frontend:latest
```

### 3. Deploy to Kubernetes
```bash
# Create namespace
kubectl apply -f namespace.yaml

# Deploy databases
kubectl apply -f redis-deployment.yaml
kubectl apply -f postgres-deployment.yaml

# Wait for databases to be ready
kubectl wait --for=condition=ready pod -l app=redis -n cyphershare --timeout=120s
kubectl wait --for=condition=ready pod -l app=postgres -n cyphershare --timeout=120s

# Deploy application
kubectl apply -f backend-deployment.yaml
kubectl apply -f frontend-deployment.yaml

# Deploy autoscaling
kubectl apply -f hpa.yaml

# Deploy ingress (update domain in ingress.yaml first)
kubectl apply -f ingress.yaml
```

### 4. Verify Deployment
```bash
# Check all pods
kubectl get pods -n cyphershare

# Check services
kubectl get svc -n cyphershare

# Check ingress
kubectl get ingress -n cyphershare

# View logs
kubectl logs -f deployment/cyphershare-backend -n cyphershare
```

## Local Development with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild after code changes
docker-compose up -d --build
```

## Scaling

```bash
# Manual scaling
kubectl scale deployment cyphershare-backend --replicas=5 -n cyphershare

# Autoscaling is configured via HPA (CPU/Memory based)
# View HPA status
kubectl get hpa -n cyphershare
```

## Monitoring

```bash
# Resource usage
kubectl top pods -n cyphershare
kubectl top nodes

# Health checks
curl http://your-domain.com/api/relay/health
```

## Production Checklist

- [ ] Update ingress domain in `ingress.yaml`
- [ ] Change PostgreSQL password in `postgres-deployment.yaml`
- [ ] Configure persistent volumes for your cloud provider
- [ ] Set up cert-manager for SSL certificates
- [ ] Configure monitoring (Prometheus/Grafana)
- [ ] Set up centralized logging (ELK/Loki)
- [ ] Configure backup strategy for databases
- [ ] Set resource limits based on load testing
- [ ] Configure network policies for security
- [ ] Set up CI/CD pipeline