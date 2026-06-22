# Contributing to AI Token Optimizer

Thank you for your interest in contributing!

## Development Setup

```bash
git clone https://github.com/eswarr-dasi/ai-token-optimizer.git
cd ai-token-optimizer
docker-compose up -d postgres redis
mvn spring-boot:run
```

## Running Tests

```bash
mvn test
```

## Code Style

- Follow standard Java conventions
- All new code must have unit tests (aim for 80%+ coverage)
- Run `mvn verify` before submitting a PR

## Pull Request Process

1. Fork the repo and create a feature branch
2. Write tests for new functionality
3. Ensure CI passes
4. Open a PR with a clear description of the change

## Architecture Decisions

Major changes (new services, data model changes, new LLM providers) should be discussed in an issue first.

## Bug Reports

Please include:
- Java version, OS
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs (with sensitive keys redacted)
