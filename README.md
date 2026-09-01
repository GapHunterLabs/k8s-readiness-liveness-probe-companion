# Kubernetes Missing Readiness/Liveness Probe Companion

Warning on any container entry in a Kubernetes workload manifest
(Deployment, Pod, StatefulSet, DaemonSet) with no `readinessProbe:`
and/or no `livenessProbe:`. Without a readiness probe, Kubernetes
can't take a sick pod out of the load balancer — real traffic keeps
being routed to a broken instance. Without a liveness probe, a hung
(not crashed) process is never automatically restarted. Both are a
real, recurring cause of documented availability incidents.

## Why it exists

Kubernetes never requires either probe — a manifest with neither is
perfectly valid YAML and deploys without a single warning, but it's a
well-documented anti-pattern with real production consequences.
Nothing in the IDE flags it today.

## Why built this way

- **100% static text analysis** — an indentation-based line scanner,
  not a real YAML parser, so it works whether the real Kubernetes/YAML
  plugin is installed or not. Same scanner shape as this catalog's own
  `k8s-resource-limit-companion`.
- **Scoped to workload kinds that keep long-lived containers** —
  Deployment, Pod, StatefulSet, DaemonSet. Job/CronJob are
  deliberately excluded: a run-to-completion pod isn't kept behind a
  Service's load balancer and isn't liveness-restarted the same way,
  so "missing probes" isn't the same real gap there.

## v0.1 scope — stated honestly, not exhaustively

Handles the common single-document, `containers:`/`- name: x` shape —
multi-doc files (`---` separators) and YAML anchors/aliases aren't
specially resolved. Never validates that
`initialDelaySeconds`/`periodSeconds` values are reasonable, only the
total absence of the probe block.

## Usage

Open any `.yml`/`.yaml` file with a workload `kind:`. A container with
no `readinessProbe:` and/or no `livenessProbe:` shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
