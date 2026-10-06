<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kubernetes Missing Readiness/Liveness Probe Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- New inspection: flags a container entry in a Kubernetes workload
  manifest (Deployment, Pod, StatefulSet, DaemonSet) with no
  `readinessProbe:` and/or no `livenessProbe:`. Indentation-based text
  scanner, no real YAML parser needed; Job/CronJob manifests are out
  of scope since they aren't kept behind a load balancer.

[Unreleased]: https://github.com/GapHunterLabs/k8s-readiness-liveness-probe-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/k8s-readiness-liveness-probe-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/k8s-readiness-liveness-probe-companion/commits/0.1.0
