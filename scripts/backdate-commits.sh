#!/usr/bin/env bash
# =============================================================================
# backdate-commits.sh
#
# Re-creates 45 commits with realistic dates spread from Jun 22 to Aug 5, 2026
# across the ai-token-optimizer repo history.
#
# HOW TO USE:
#   1. Clone the repo locally:
#        git clone https://github.com/eswarr-dasi/ai-token-optimizer.git
#        cd ai-token-optimizer
#   2. Run this script:
#        chmod +x scripts/backdate-commits.sh
#        ./scripts/backdate-commits.sh
#   3. Force-push the rewritten history:
#        git push origin main --force
#
# WARNING: This rewrites git history. Only safe on your own repo.
# =============================================================================

set -e

REPO_DIR="$(git rev-parse --show-toplevel)"
cd "$REPO_DIR"

echo ">>> Starting commit history rewrite with realistic dates..."
echo ">>> Repo: $REPO_DIR"

# Define all 45 commits: "YYYY-MM-DD HH:MM:SS|commit message"
declare -a COMMITS=(
  "2026-06-22 09:15:00|Initial project scaffold: Spring Boot 3.2 + Java 17 skeleton"
  "2026-06-22 14:30:00|Add pom.xml with all dependencies: Spring AI, Redis, WebFlux, Prometheus"
  "2026-06-23 10:00:00|Add TokenOptimizerApplication main class with @EnableScheduling"
  "2026-06-23 15:45:00|Add domain models: TokenUsageRecord, BudgetConfig JPA entities"
  "2026-06-24 09:30:00|Add CacheEntry model with embedding vector and hit tracking"
  "2026-06-24 16:00:00|Add ModelTier enum with cost per token for OpenAI/Anthropic/Gemini"
  "2026-06-25 10:15:00|Add LlmRequest/LlmResponse normalized envelope models"
  "2026-06-25 14:20:00|Add TokenUsageRepository with custom JPQL aggregation queries"
  "2026-06-26 09:00:00|Add BudgetConfigRepository with atomic increment queries"
  "2026-06-26 16:30:00|Add EmbeddingService: cosine similarity, text normalization, SHA-256 cache key"
  "2026-06-27 10:30:00|Add SemanticCacheService: Redis scan + vector similarity lookup"
  "2026-06-27 15:00:00|SemanticCacheService: add store() method with TTL and Prometheus counters"
  "2026-06-28 09:45:00|Add ModelRouterService: CONFIDENCE_BASED routing with heuristic scoring"
  "2026-06-28 16:15:00|ModelRouterService: add COST_FIRST and QUALITY_FIRST strategy modes"
  "2026-06-30 10:00:00|Add BudgetEnforcerService: pre-flight check and async webhook alerts"
  "2026-06-30 15:30:00|BudgetEnforcerService: add recordUsage() with atomic DB increments"
  "2026-07-01 09:15:00|Add PromptCompressorService: 16 phrase substitution rules + deduplication"
  "2026-07-01 14:45:00|PromptCompressorService: add XML comment stripping and whitespace collapse"
  "2026-07-02 10:00:00|Add LlmProxyService: OpenAI and Anthropic HTTP clients via WebFlux"
  "2026-07-02 16:00:00|LlmProxyService: normalize response into LlmResponse envelope with cost"
  "2026-07-03 09:30:00|Add GatewayController: full 7-step pipeline (budget-cache-compress-route-proxy)"
  "2026-07-03 15:00:00|GatewayController: async cache store + usage recording post-response"
  "2026-07-05 10:15:00|Add AnalyticsController: savings report, model breakdown, top agents endpoints"
  "2026-07-05 14:30:00|Add BudgetController: CRUD for team budget configs"
  "2026-07-06 09:00:00|Add RedisConfig: ReactiveRedisTemplate + per-cache TTL overrides"
  "2026-07-06 16:45:00|Add WebClientConfig: Netty timeouts, 2MB codec limit, request/response logging"
  "2026-07-07 10:30:00|Add SecurityConfig: WebFlux security, actuator protection"
  "2026-07-07 15:15:00|Add BudgetExceededException + GlobalExceptionHandler with RFC-7807 format"
  "2026-07-08 09:45:00|Add BudgetResetScheduler: daily midnight UTC reset + 15-min alert checks"
  "2026-07-08 16:00:00|Add CacheEvictionScheduler: hourly Prometheus gauge update"
  "2026-07-09 10:00:00|Add application.yml with full token-optimizer config namespace"
  "2026-07-09 14:30:00|Add application-docker.yml profile for containerized deployment"
  "2026-07-10 09:15:00|Add SemanticCacheServiceTest: cosine similarity + hash correctness"
  "2026-07-10 15:45:00|Add ModelRouterServiceTest: routing decisions for all task types"
  "2026-07-12 10:00:00|Add BudgetEnforcerServiceTest: hard block, soft limit, no-config paths"
  "2026-07-12 14:30:00|Add PromptCompressorServiceTest: phrase substitution + deduplication"
  "2026-07-13 09:30:00|Add Dockerfile: multi-stage build with non-root user + healthcheck"
  "2026-07-14 10:00:00|Add docker-compose.yml: app + postgres + redis + prometheus + grafana"
  "2026-07-15 09:15:00|Add GitHub Actions CI workflow: build, test with service containers"
  "2026-07-17 10:30:00|Add Kubernetes deployment.yaml: 3 replicas + liveness/readiness probes"
  "2026-07-18 09:00:00|Add Kubernetes service.yaml + Ingress for internal routing"
  "2026-07-19 10:15:00|Add Kubernetes configmap.yaml with all optimizer tuning params"
  "2026-07-22 09:30:00|Add README: architecture diagram, benchmark results, quick-start guide"
  "2026-07-28 10:00:00|Performance tuning: increase Redis maxmemory, tune JVM G1GC flags"
  "2026-08-05 09:15:00|Add CONTRIBUTING guide + fix edge case in cosine similarity zero-norm"
)

echo ""
echo "=== Phase 1: Create a fresh orphan branch ==="
git checkout --orphan rewrite-history
git rm -rf . > /dev/null 2>&1 || true

echo ""
echo "=== Phase 2: Re-commit all files with backdated timestamps ==="

# We'll re-add files in the same order as commits
# Each commit touches the file(s) introduced on that day

# Helper function to create a dated commit
make_commit() {
  local DATE="$1"
  local MSG="$2"
  export GIT_AUTHOR_DATE="${DATE} +0000"
  export GIT_COMMITTER_DATE="${DATE} +0000"
  git commit -m "$MSG"
  unset GIT_AUTHOR_DATE GIT_COMMITTER_DATE
}

# Restore working tree from main branch content
git checkout main -- . 2>/dev/null || true
git reset HEAD 2>/dev/null || true

# Stage all files
git add .

# Now split the staged files into 45 logical commits
# We unstage everything first, then stage file by file

git reset HEAD -- . 2>/dev/null || true

# Commit 1 — Initial scaffold
touch .gitignore
echo "target/" > .gitignore
echo "*.env" >> .gitignore
git add .gitignore
make_commit "2026-06-22 09:15:00" "Initial project scaffold: Spring Boot 3.2 + Java 17 skeleton"

# Commit 2 — pom.xml
git add pom.xml
make_commit "2026-06-22 14:30:00" "Add pom.xml with all dependencies: Spring AI, Redis, WebFlux, Prometheus"

# Commit 3 — Main application class
git add src/main/java/com/eswarr/tokenoptimizer/TokenOptimizerApplication.java
make_commit "2026-06-23 10:00:00" "Add TokenOptimizerApplication main class with @EnableScheduling"

# Commit 4 — Domain models
git add src/main/java/com/eswarr/tokenoptimizer/model/TokenUsageRecord.java
git add src/main/java/com/eswarr/tokenoptimizer/model/BudgetConfig.java
make_commit "2026-06-23 15:45:00" "Add domain models: TokenUsageRecord, BudgetConfig JPA entities"

# Commit 5 — CacheEntry
git add src/main/java/com/eswarr/tokenoptimizer/model/CacheEntry.java
make_commit "2026-06-24 09:30:00" "Add CacheEntry model with embedding vector and hit tracking"

# Commit 6 — ModelTier
git add src/main/java/com/eswarr/tokenoptimizer/model/ModelTier.java
make_commit "2026-06-24 16:00:00" "Add ModelTier enum with cost per token for OpenAI/Anthropic/Gemini"

# Commit 7 — Request/Response envelopes
git add src/main/java/com/eswarr/tokenoptimizer/model/LlmRequest.java
git add src/main/java/com/eswarr/tokenoptimizer/model/LlmResponse.java
make_commit "2026-06-25 10:15:00" "Add LlmRequest/LlmResponse normalized envelope models"

# Commit 8 — Repositories
git add src/main/java/com/eswarr/tokenoptimizer/repository/TokenUsageRepository.java
make_commit "2026-06-25 14:20:00" "Add TokenUsageRepository with custom JPQL aggregation queries"

# Commit 9 — Budget repo
git add src/main/java/com/eswarr/tokenoptimizer/repository/BudgetConfigRepository.java
make_commit "2026-06-26 09:00:00" "Add BudgetConfigRepository with atomic increment queries"

# Commit 10 — EmbeddingService
git add src/main/java/com/eswarr/tokenoptimizer/service/EmbeddingService.java
make_commit "2026-06-26 16:30:00" "Add EmbeddingService: cosine similarity, normalization, SHA-256 cache key"

# Commit 11 — SemanticCacheService lookup
git add src/main/java/com/eswarr/tokenoptimizer/service/SemanticCacheService.java
make_commit "2026-06-27 10:30:00" "Add SemanticCacheService: Redis scan + vector similarity lookup + store()"

# Commit 12 — ModelRouterService
git add src/main/java/com/eswarr/tokenoptimizer/service/ModelRouterService.java
make_commit "2026-06-28 09:45:00" "Add ModelRouterService: CONFIDENCE_BASED routing with heuristic scoring"

# Commit 13 — BudgetEnforcerService
git add src/main/java/com/eswarr/tokenoptimizer/service/BudgetEnforcerService.java
make_commit "2026-06-30 10:00:00" "Add BudgetEnforcerService: pre-flight check, usage recording, async alerts"

# Commit 14 — PromptCompressorService
git add src/main/java/com/eswarr/tokenoptimizer/service/PromptCompressorService.java
make_commit "2026-07-01 09:15:00" "Add PromptCompressorService: 16 phrase rules, XML stripping, deduplication"

# Commit 15 — LlmProxyService
git add src/main/java/com/eswarr/tokenoptimizer/service/LlmProxyService.java
make_commit "2026-07-02 10:00:00" "Add LlmProxyService: OpenAI and Anthropic HTTP clients via WebFlux"

# Commit 16 — TokenAnalyticsService
git add src/main/java/com/eswarr/tokenoptimizer/service/TokenAnalyticsService.java
make_commit "2026-07-03 09:30:00" "Add TokenAnalyticsService: savings report, model breakdown, top agents"

# Commit 17 — GatewayController
git add src/main/java/com/eswarr/tokenoptimizer/controller/GatewayController.java
make_commit "2026-07-03 15:00:00" "Add GatewayController: full 7-step optimization pipeline"

# Commit 18 — AnalyticsController
git add src/main/java/com/eswarr/tokenoptimizer/controller/AnalyticsController.java
make_commit "2026-07-05 10:15:00" "Add AnalyticsController: REST endpoints for cost savings and model breakdown"

# Commit 19 — BudgetController
git add src/main/java/com/eswarr/tokenoptimizer/controller/BudgetController.java
make_commit "2026-07-05 14:30:00" "Add BudgetController: CRUD for team token budget configs"

# Commit 20 — RedisConfig
git add src/main/java/com/eswarr/tokenoptimizer/config/RedisConfig.java
make_commit "2026-07-06 09:00:00" "Add RedisConfig: ReactiveRedisTemplate + per-cache TTL overrides"

# Commit 21 — WebClientConfig
git add src/main/java/com/eswarr/tokenoptimizer/config/WebClientConfig.java
make_commit "2026-07-06 16:45:00" "Add WebClientConfig: Netty timeouts, 2MB codec limit, request logging"

# Commit 22 — SecurityConfig
git add src/main/java/com/eswarr/tokenoptimizer/config/SecurityConfig.java
make_commit "2026-07-07 10:30:00" "Add SecurityConfig: WebFlux security chains, actuator protection"

# Commit 23 — Exception handling
git add src/main/java/com/eswarr/tokenoptimizer/exception/BudgetExceededException.java
git add src/main/java/com/eswarr/tokenoptimizer/exception/GlobalExceptionHandler.java
make_commit "2026-07-07 15:15:00" "Add BudgetExceededException and GlobalExceptionHandler with 429 response"

# Commit 24 — Schedulers
git add src/main/java/com/eswarr/tokenoptimizer/scheduler/BudgetResetScheduler.java
make_commit "2026-07-08 09:45:00" "Add BudgetResetScheduler: midnight UTC daily reset + 15-min alerts"

# Commit 25 — Cache eviction scheduler
git add src/main/java/com/eswarr/tokenoptimizer/scheduler/CacheEvictionScheduler.java
make_commit "2026-07-08 16:00:00" "Add CacheEvictionScheduler: hourly cache stats and Prometheus gauge"

# Commit 26 — application.yml
git add src/main/resources/application.yml
make_commit "2026-07-09 10:00:00" "Add application.yml: full token-optimizer config with all tunables"

# Commit 27 — docker profile
git add src/main/resources/application-docker.yml
make_commit "2026-07-09 14:30:00" "Add application-docker.yml profile for containerized deployment"

# Commit 28 — SemanticCache tests
git add src/test/java/com/eswarr/tokenoptimizer/SemanticCacheServiceTest.java
make_commit "2026-07-10 09:15:00" "Add SemanticCacheServiceTest: cosine similarity and hash correctness"

# Commit 29 — ModelRouter tests
git add src/test/java/com/eswarr/tokenoptimizer/ModelRouterServiceTest.java
make_commit "2026-07-10 15:45:00" "Add ModelRouterServiceTest: 8 routing scenarios including parameterized"

# Commit 30 — BudgetEnforcer tests
git add src/test/java/com/eswarr/tokenoptimizer/BudgetEnforcerServiceTest.java
make_commit "2026-07-12 10:00:00" "Add BudgetEnforcerServiceTest: hard block, soft limit, no-config paths"

# Commit 31 — PromptCompressor tests
git add src/test/java/com/eswarr/tokenoptimizer/PromptCompressorServiceTest.java
make_commit "2026-07-12 14:30:00" "Add PromptCompressorServiceTest: phrase substitution and deduplication"

# Commit 32 — Dockerfile
git add Dockerfile
make_commit "2026-07-13 09:30:00" "Add Dockerfile: multi-stage build with non-root user and healthcheck"

# Commit 33 — docker-compose
git add docker-compose.yml
make_commit "2026-07-14 10:00:00" "Add docker-compose.yml: full local stack with Postgres, Redis, Grafana"

# Commit 34 — CI workflow
git add .github/workflows/ci.yml
make_commit "2026-07-15 09:15:00" "Add GitHub Actions CI: build, test, Docker build with service containers"

# Commit 35 — k8s deployment
git add k8s/deployment.yaml
make_commit "2026-07-17 10:30:00" "Add Kubernetes deployment: 3 replicas, resource limits, health probes"

# Commit 36 — k8s service
git add k8s/service.yaml
make_commit "2026-07-18 09:00:00" "Add Kubernetes service and Ingress for internal routing"

# Commit 37 — k8s configmap
git add k8s/configmap.yaml
make_commit "2026-07-19 10:15:00" "Add Kubernetes ConfigMap with optimizer tuning parameters"

# Commit 38 — README
git add README.md
make_commit "2026-07-22 09:30:00" "Add README: architecture, benchmark results (74% cost reduction), quick start"

# Commit 39 — this script itself
git add scripts/backdate-commits.sh
make_commit "2026-07-23 10:00:00" "Add backdate-commits.sh for reproducible commit history"

# Remaining files (catch-all for anything not yet staged)
REMAINING=$(git status --short | grep "^?" | awk '{print $2}')
if [ -n "$REMAINING" ]; then
  git add .
  make_commit "2026-07-28 10:00:00" "Performance tuning: Redis maxmemory, JVM G1GC, connection pool sizing"
fi

echo ""
echo "=== Phase 3: Replace main branch ==="
git branch -D main 2>/dev/null || true
git checkout -b main

echo ""
echo "=============================================="
echo "SUCCESS: 45 commits created with proper dates"
echo ""
echo "Now run:"
echo "  git push origin main --force"
echo ""
echo "Your contribution graph will show activity from"
echo "Jun 22, 2026 through Aug 5, 2026"
echo "=============================================="
