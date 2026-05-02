# Job-Cupid — AWS Cloud Deployment Strategy

**Region:** `ap-southeast-1` (Singapore)
**Deployment model:** Kubernetes on EKS with rolling updates, blue/green promotion for major releases

---

## Architecture Overview

```mermaid
graph TD
    subgraph Internet
        Users([Users / Mobile])
        Stripe([Stripe Webhooks])
    end

    subgraph AWS ap-southeast-1
        subgraph Edge
            R53[Route 53]
            CF[CloudFront\nCDN for S3 assets]
            WAF[AWS WAF\nrate-limit · OWASP rules]
            ALB[Application Load Balancer\nHTTPS · cert via ACM]
        end

        subgraph VPC - Public Subnets
            NAT[NAT Gateway]
        end

        subgraph VPC - Private Subnets
            subgraph EKS Cluster
                API1[API Pod]
                API2[API Pod]
                API3[API Pod]
                HPA[Horizontal Pod Autoscaler]
            end

            subgraph Data Tier
                RDS[(RDS PostgreSQL\nMulti-AZ)]
                REDIS[(ElastiCache Redis\ncluster mode off)]
                MSK[(Amazon MSK\nKafka — 3 brokers)]
            end

            subgraph Storage
                S3[(S3\njob-cupid-assets-prod)]
            end
        end

        subgraph CI/CD
            ECR[ECR\nContainer Registry]
            GHA[GitHub Actions]
        end

        subgraph Observability
            CW[CloudWatch Logs]
            AMP[Amazon Managed\nPrometheus]
            AMG[Amazon Managed\nGrafana]
        end

        subgraph Secrets
            SM[AWS Secrets Manager]
        end
    end

    Users --> R53
    Stripe --> ALB
    R53 --> CF
    R53 --> ALB
    CF --> S3
    WAF --> ALB
    ALB --> API1
    ALB --> API2
    ALB --> API3
    HPA --> API1
    API1 --> RDS
    API1 --> REDIS
    API1 --> MSK
    API1 --> S3
    API1 --> SM
    GHA --> ECR
    ECR --> EKS Cluster
    API1 --> CW
    API1 --> AMP
    AMP --> AMG
```

---

## AWS Services

| Service | Purpose | Tier |
|---------|---------|------|
| **EKS** | Runs the Spring Boot API pods | Compute |
| **RDS PostgreSQL** (Multi-AZ) | Primary database with automatic failover | Data |
| **ElastiCache Redis** | JWT blacklist, refresh token store, swipe rate limiter | Cache |
| **Amazon MSK** | Managed Kafka — swipe events, match events, notifications | Messaging |
| **S3** | Profile photo storage, resume uploads | Storage |
| **CloudFront** | CDN in front of S3 for low-latency asset delivery | Edge |
| **ALB** | HTTPS termination, health-check routing to EKS | Networking |
| **Route 53** | DNS, latency-based routing | DNS |
| **AWS WAF** | Rate limiting, OWASP Top 10 rules, bot protection | Security |
| **ACM** | TLS certificates (auto-renewed) | Security |
| **Secrets Manager** | DB credentials, JWT secret, Stripe keys, Redis auth | Secrets |
| **ECR** | Private Docker image registry | Registry |
| **IAM + IRSA** | Pod-level IAM roles — no long-lived access keys in pods | Auth |
| **CloudWatch** | Centralised log aggregation | Observability |
| **Amazon Managed Prometheus** | Scrapes `/actuator/prometheus` from pods | Metrics |
| **Amazon Managed Grafana** | Dashboards — latency, error rate, match throughput | Dashboards |

---

## Network Topology

```
VPC: 10.0.0.0/16

Public subnets  (10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24)
  └─ ALB, NAT Gateway, Bastion (SSM Session Manager — no SSH port open)

Private subnets (10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24)
  └─ EKS node groups
  └─ RDS Multi-AZ (primary + standby across two AZs)
  └─ ElastiCache Redis
  └─ MSK brokers

No public IP on any data-tier resource.
EKS pods reach the internet (for Stripe API calls, etc.) via NAT Gateway.
```

**Security groups:**
- ALB → API pods: port 8080 only
- API pods → RDS: port 5432 only
- API pods → Redis: port 6379 only
- API pods → MSK: port 9092 (SASL/TLS) only
- All inter-tier traffic stays inside the VPC

---

## Deployment Pipeline (CI/CD)

```
GitHub push to main
    │
    ▼
GitHub Actions workflow
    ├─ 1. mvn verify          (unit + integration tests)
    ├─ 2. docker build        (multi-stage: build → slim runtime image)
    ├─ 3. docker push → ECR   (tagged :sha-<commit> and :latest)
    ├─ 4. Flyway migrate       (runs against target RDS before rollout)
    └─ 5. kubectl set image    (rolling update — EKS replaces pods one by one)
```

**Rolling update settings (Kubernetes Deployment):**
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # one extra pod during rollout
    maxUnavailable: 0  # never drop below desired replica count
```

Kubernetes readiness probe hits `/actuator/health/readiness` — a pod only receives traffic once Spring Boot reports it ready (DB connected, Flyway done, Redis reachable).

**Blue/green for major releases** (schema changes, breaking API changes):
- Deploy new version as a separate Deployment (`job-cupid-green`)
- Smoke-test via internal service endpoint
- Switch ALB target group from `blue` to `green` (single API call, ~0 downtime)
- Keep `blue` alive for 15 minutes — instant rollback if error rate spikes

---

## Environment Strategy

| Environment | EKS | RDS | MSK | Notes |
|-------------|-----|-----|-----|-------|
| **dev** | 1 pod, `t3.small` nodes | `db.t3.micro` single-AZ | 1 broker | Docker Compose locally preferred; cloud dev only for integration testing |
| **staging** | 2 pods, `t3.medium` nodes | `db.t3.medium` single-AZ | 2 brokers | Mirrors prod config; used for pre-release smoke tests |
| **prod** | 3–10 pods (HPA), `t3.large` nodes | `db.r6g.large` Multi-AZ | 3 brokers | Auto-scaling, WAF on, CloudFront, full observability |

Separate VPCs per environment. No shared resources between staging and prod.

---

## Scaling Strategy

**Horizontal Pod Autoscaler:**
```yaml
minReplicas: 3
maxReplicas: 10
metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
```

**RDS:**
- Read replicas added when read load exceeds 70% of primary CPU
- `PgBouncer` sidecar (or RDS Proxy) for connection pooling — Hikari pool × N pods can exhaust PostgreSQL's `max_connections` quickly

**Redis:**
- ElastiCache cluster mode disabled (single shard) is sufficient for token blacklist + rate limiting
- Upgrade to cluster mode if memory approaches 75% of node capacity

**MSK / Kafka:**
- Start with 3 brokers, `kafka.t3.small`
- Scale broker count as partition lag grows; MSK makes this a non-disruptive operation

**S3:** Effectively unlimited. CloudFront cache-control headers reduce origin requests.

---

## Security Posture

| Control | Implementation |
|---------|----------------|
| No secrets in code or env vars | All secrets in AWS Secrets Manager, injected at pod startup via External Secrets Operator |
| No long-lived AWS credentials in pods | IRSA (IAM Roles for Service Accounts) — each pod gets a scoped IAM role via OIDC |
| TLS everywhere | ACM cert on ALB; MSK TLS + SASL/SCRAM; Redis `in-transit-encryption` enabled |
| Encryption at rest | RDS storage encrypted (AES-256); S3 SSE-S3; Redis at-rest encryption |
| WAF rules | AWS Managed Rules (OWASP Top 10) + rate limit: 1000 req/5 min per IP |
| JWT blacklist | Redis — revoked tokens blocked within milliseconds |
| Image scanning | ECR image scanning on push; block deployment if CRITICAL CVEs found |
| Least privilege | Each pod's IAM role scoped to only the S3 bucket + Secrets Manager paths it needs |

---

## Observability

**Logs** — structured JSON (already configured in `application.yml`):
```
Spring Boot → stdout (JSON) → CloudWatch Logs → Log Insights queries
```

**Metrics** — Prometheus scrape via `/actuator/prometheus`:
```
Amazon Managed Prometheus (scrape every 15s) → Grafana dashboards
```

Key dashboards:
- API latency (p50 / p95 / p99)
- Match creation rate (Kafka consumer lag)
- Active swipes per minute
- DB connection pool utilisation (Hikari metrics)
- Error rate by endpoint

**Alerts (via Grafana):**
- p99 latency > 500 ms for 2 consecutive minutes → PagerDuty
- Kafka consumer lag > 10 000 messages → PagerDuty
- RDS CPU > 80% for 5 minutes → Slack warning
- Pod crash loop → PagerDuty immediately

---

## Disaster Recovery

| Scenario | RTO | RPO | Response |
|----------|-----|-----|----------|
| Pod crash | < 30 s | 0 | EKS reschedules pod automatically |
| AZ outage | < 2 min | 0 | RDS Multi-AZ automatic failover; EKS nodes span 3 AZs |
| RDS primary failure | < 60 s | 0 | RDS Multi-AZ promotes standby automatically |
| Full region outage | 4–8 hrs | < 15 min | Manual failover to `ap-east-1` (Hong Kong) using RDS snapshot + MSK mirror |
| Accidental data deletion | < 1 hr | < 5 min | RDS automated backups (7-day retention) + point-in-time restore |

---

## Cost Estimates (prod, steady-state)

| Service | Instance | Est. monthly |
|---------|----------|-------------|
| EKS (3 nodes `t3.large`) | On-Demand | ~$180 |
| RDS (`db.r6g.large` Multi-AZ) | Reserved 1-yr | ~$200 |
| ElastiCache (`cache.t3.medium`) | On-Demand | ~$50 |
| MSK (3 × `kafka.t3.small`) | On-Demand | ~$150 |
| ALB | — | ~$25 |
| CloudFront | 1 TB transfer | ~$85 |
| S3 + ECR | 500 GB | ~$15 |
| CloudWatch + AMP + AMG | — | ~$60 |
| NAT Gateway | 1 TB transfer | ~$45 |
| **Total** | | **~$810 / month** |

Switching EKS nodes and MSK to **Savings Plans / Reserved** cuts compute cost by ~30%.
